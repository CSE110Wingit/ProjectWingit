"""
The code that performs the actions specified in lambda_handler
"""
from lambda_code.utils import *
from lambda_code.s3_utils import *


def create_account(body):
    """
    Attempt to create a new account.
    
    This new account must not have the same username or email as an already
        existing account. The username and password must be working usernames
        and emails.
    If the account can be created, an email will be sent to the specified email
        with a link to activate the account. A random string of alphanumeric
        characters will be made as a verification code
    :param body: the parameters
    """
    all_good, rest = get_cleaned_params(body, USERNAME_STR, EMAIL_STR, PASSWORD_HASH_STR)
    if not all_good:
        return rest
    username, email, password_hash = rest[USERNAME_STR], rest[EMAIL_STR], rest[PASSWORD_HASH_STR]

    all_good, *rest = get_possible_params(body, params=[NUT_ALLERGY_STR, GLUTEN_FREE_STR, SPICINESS_LEVEL_STR],
                                          defaults=[False, False, -1])
    if not all_good:
        return rest[0]
    nut_allergy, gluten_free, spiciness = rest

    # All of this is dependant on the SQL database existing and the connection existing,
    #   so we will put it all in a try-catch since things can break easily
    try:

        # Search the database to make sure the username and email does not already exist
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])

        # Make sure there is no duplicate username
        if not cursor.fetchone() is None:
            return error(ERROR_USERNAME_ALREADY_EXISTS, username)

        # Search for email
        cursor.execute(GET_WHERE_EMAIL_LIKE_SQL, [email])
        if not cursor.fetchone() is None:
            return error(ERROR_EMAIL_ALREADY_IN_USE, email)

        # Everything looks good, make the account
        verification_code = generate_verification_code()
        server_password_hash = gen_crypt(password_hash)
        account_info = (username, email, verification_code, current_time(), server_password_hash, nut_allergy,
                        gluten_free, spiciness)

        cursor.execute(CREATE_ACCOUNT_SQL, account_info)
        conn.commit()  # Commit the changes to the database

    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "1", repr(e))

    # Send the email to the user telling them to activate their account
    return send_activation_email(username, email, verification_code)


def verify_account(params):
    """
    Verifies the given account (only needs username and verification_code)
    :param params: the params passed in the GET call
    """
    all_good, rest = get_cleaned_params(params, USERNAME_STR, VERIFICATION_CODE_STR)
    if not all_good:
        return rest
    username, verification_code = rest[USERNAME_STR], rest[VERIFICATION_CODE_STR]

    # Actually check to see if the verification code matches the database
    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])

        result = cursor.fetchone()

        # If the username does not exist
        if result is None:
            return error(ERROR_USERNAME_DOES_NOT_EXIST, username)

        # If the verification code is currently null (already verified), or if
        #   the verification codes match, then verify the account
        if result[VERIFICATION_CODE_STR] == '':
            pass
        elif result[VERIFICATION_CODE_STR] == verification_code:
            cursor.execute(UPDATE_VERIFICATION_CODE_SQL, [username])
            conn.commit()  # Commit the changes to the database
        else:
            return error(ERROR_UNMATCHING_VERIFICATION_CODE, verification_code, username)

        return return_message(good_message='Account Verified!')

    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "2", repr(e))


def _verify_login_credentials(params, require_verified=True):
    """
    Verifies the login credentials are correct.
    :param params: could contain either email or username, along with password
    :param require_verified: if True, return an error if the account is not yet verified
    :return: a tuple with 1st element: False if there was an error, True if all good, 2nd element: the return
        error if there was an error, or the username of the login (irregardless of if the user gave email or username)
    """
    all_good, ret_dict = get_cleaned_params(params, (USERNAME_STR, EMAIL_STR),
                                            (PASSWORD_HASH_STR, PASSWORD_CHANGE_CODE_STR))

    if not all_good:
        return False, ret_dict

    username = ret_dict[USERNAME_STR] if USERNAME_STR in ret_dict else None
    email = ret_dict[EMAIL_STR] if EMAIL_STR in ret_dict else None
    password_hash = ret_dict[PASSWORD_HASH_STR] if PASSWORD_HASH_STR in ret_dict else None
    change_code = ret_dict[PASSWORD_CHANGE_CODE_STR] if PASSWORD_CHANGE_CODE_STR in ret_dict else None

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()

        # Query will depend on whether or not we are using email/password
        if email is not None:
            cursor.execute(GET_WHERE_EMAIL_LIKE_SQL, [email])
        else:
            cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])

        result = cursor.fetchone()

        # If the username/email does not exist
        if result is None:
            return (False, error(ERROR_EMAIL_DOES_NOT_EXIST, email)) if email is not None else \
                (False, error(ERROR_USERNAME_DOES_NOT_EXIST, username))

        # If the account has not yet been verified
        if require_verified and result[VERIFICATION_CODE_STR] != '':
            return False, error(ERROR_ACCOUNT_UNVERIFIED)

        # If the password_hash does not match, tell them
        if password_hash is not None and not password_correct(password_hash, result[PASSWORD_HASH_STR]):
            return False, error(ERROR_INCORRECT_PASSWORD)

        elif change_code is not None and (change_code != result[PASSWORD_CHANGE_CODE_STR] or
                                          current_time() - result[
                                              PASSWORD_CHANGE_CODE_CREATION_TIME_STR] > CHANGE_PASSWORD_TIMEOUT):
            return False, error(ERROR_INVALID_PASSWORD_CHANGE_CODE)

        return True, result[USERNAME_STR]

    except Exception as e:
        return False, error(ERROR_UNKNOWN_ERROR, "3", repr(e))


def login_account(params):
    """
    Returns the string "Credentials Accepted" to affirm that the username/email
    (defaults to email if both are given) match the given password_hash
    """
    all_good, username = _verify_login_credentials(params)
    if not all_good:
        return username

    # Get all the user info
    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, username)
        result = cursor.fetchone()

        user_info = {s: result[s] for s in USER_PREFERENCES_FIELDS}
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "4", repr(e))

    return return_message(good_message='Credentials Accepted!', data=user_info)


def delete_account(body):
    """
    Delete a user's account
    """
    # Attempt to login with the given credentials, and if it fails, return the login error
    all_good, username = _verify_login_credentials(body, require_verified=False)
    if not all_good:
        return username

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(DELETE_ACCOUNT_SQL, [username])
        conn.commit()

        return return_message(good_message="Account Deleted!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "5", repr(e))


def get_s3_permissions(params):
    """
    Gets a presigned url for an s3 bucket so a user can upload a file
    """
    # Make sure we have correct params
    all_good, rest = get_cleaned_params(params, PASSWORD_HASH_STR, S3_REASON_STR, IMAGE_FILE_EXTENSION_STR)
    if not all_good:
        return rest
    password_hash, reason, file_ext = rest[PASSWORD_HASH_STR], rest[S3_REASON_STR], rest[IMAGE_FILE_EXTENSION_STR]

    # Attempt to login with the given credentials, and if it fails, return the login error
    all_good, username = _verify_login_credentials(params)
    if not all_good:
        return username

    if reason == S3_REASON_UPLOAD_USER_PROFILE_IMAGE:
        all_good, extra_s3_info = get_extra_s3_info(username, reason, file_ext)
        if not all_good:
            return extra_s3_info
        all_good, ret = create_presigned_post(extra_s3_info[S3_IMAGE_DEST_STR])

    elif reason == S3_REASON_UPLOAD_RECIPE_IMAGE:
        recipe_pic_id = generate_verification_code()

        all_good, extra_s3_info = get_extra_s3_info(recipe_pic_id, reason, file_ext)
        if not all_good:
            return extra_s3_info
        all_good, ret = create_presigned_post(extra_s3_info[S3_IMAGE_DEST_STR])
        ret[RECIPE_PICTURE_ID_STR] = recipe_pic_id

    else:
        return error(ERROR_IMPOSSIBLE_ERROR, "get_s3_permissions (reason should already be good now because "
                                             "get_extra_s3_info should have checked it)")

    return ret if not all_good else return_message(data=ret)


def change_password(body):
    """
    Updates the user's password in the database.
    """
    # Make sure we have correct params
    all_good, rest = get_cleaned_params(body, NEW_PASSWORD_HASH_STR)
    if not all_good:
        return rest

    new_hash = rest[NEW_PASSWORD_HASH_STR]

    # Attempt to login with the given credentials, and if it fails, return the login error
    all_good, username = _verify_login_credentials(body, require_verified=False)
    if not all_good:
        return username

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(UPDATE_PASSWORD_SQL, [gen_crypt(new_hash), username])
        conn.commit()

        return return_message(good_message="Password changed!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "6", repr(e))


def get_password_change_code(params):
    """
    Sends a password change code to the username
    """
    all_good, ret_dict = get_cleaned_params(params, (USERNAME_STR, EMAIL_STR))
    if not all_good:
        return ret_dict

    username = ret_dict[USERNAME_STR] if USERNAME_STR in ret_dict else None
    email = ret_dict[EMAIL_STR] if EMAIL_STR in ret_dict else None

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()

        if username is not None:
            cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])
        else:
            cursor.execute(GET_WHERE_EMAIL_LIKE_SQL, [email])

        result = cursor.fetchone()

        if result is None:
            if username is not None:
                return error(ERROR_USERNAME_DOES_NOT_EXIST, username)
            return error(ERROR_EMAIL_DOES_NOT_EXIST, email)

        code = random_password_change_code()
        cursor.execute(UPDATE_PASSWORD_CHANGE_CODE_SQL.replace('%d', str(current_time())), [code, result[USERNAME_STR]])
        conn.commit()

        return send_password_change_code_email(username, result[EMAIL_STR], code)
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "7", repr(e))


def create_recipe(body):
    """
    Creates and adds a new recipe to the database
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, RECIPE_TITLE_STR, RECIPE_INGREDIENTS_STR, RECIPE_DESCRIPTION_STR,
                                            RECIPE_TUTORIAL_STR, RECIPE_PRIVATE_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR,
                                            SPICINESS_LEVEL_STR)
    if not all_good:
        return ret_dict
    data = [ret_dict[s] for s in [RECIPE_TITLE_STR, RECIPE_INGREDIENTS_STR, RECIPE_DESCRIPTION_STR,
                                  RECIPE_TUTORIAL_STR, RECIPE_PRIVATE_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR,
                                  SPICINESS_LEVEL_STR]]

    # Check for a possible recipe_picture id
    recipe_pic = body[RECIPE_PICTURE_STR] if RECIPE_PICTURE_STR in body else None

    # Update recipes table, then users created recipes table
    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()

        # Get next recipe id
        cursor.execute(GET_NEXT_RECIPE_ID_SQL)
        recipe_id = cursor.fetchone()[NEXT_RECIPE_ID_STR]
        rest = [recipe_id] + data + [recipe_pic, username]

        # Create the recipe in the table
        cursor.execute(CREATE_RECIPE_SQL, rest)

        # Add to user's list of created recipe
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, username)
        recipes = cursor.fetchone()[CREATED_RECIPES_STR]
        recipes = str(recipe_id) if recipes is None or recipes == '' else recipes + "," + str(recipe_id)
        cursor.execute(UPDATE_CREATED_RECIPES_SQL, [recipes, username])

        conn.commit()
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "8", repr(e))

    return return_message(good_message="Recipe Created!", data={RECIPE_ID_STR: recipe_id})


def get_recipe(params):
    """
    Gets a recipe by id
    """
    all_good, ret_dict = get_cleaned_params(params, RECIPE_ID_STR)
    if not all_good:
        return ret_dict
    recipe_id = ret_dict[RECIPE_ID_STR]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()

        # Get next recipe id
        cursor.execute(GET_RECIPE_BY_ID_SQL, [recipe_id])
        result = cursor.fetchone()

        if result is None:
            return error(ERROR_UNKNOWN_RECIPE, recipe_id)

        # If it's not private, just return
        if result[RECIPE_PRIVATE_STR] == 0:
            return return_message(good_message="Recipe Found!", data=_make_recipe_return(result))

        # Otherwise, try to login and make sure it is the right user
        all_good, username = _verify_login_credentials(params)

        if all_good and result[RECIPE_AUTHOR_STR] == username:
            return return_message(good_message="Recipe Found!", data=_make_recipe_return(result))

        return error(ERROR_UNKNOWN_RECIPE, recipe_id)
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "9", repr(e))


def delete_recipe(body):
    """
    Deletes a recipe
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, RECIPE_ID_STR)
    if not all_good:
        return ret_dict
    recipe_id = ret_dict[RECIPE_ID_STR]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()

        # Get next recipe id
        cursor.execute(GET_RECIPE_BY_ID_SQL, [recipe_id])
        result = cursor.fetchone()

        if result is None or result[RECIPE_AUTHOR_STR] != username:
            return error(ERROR_UNKNOWN_RECIPE, recipe_id)

        cursor.execute(DELETE_RECIPE_SQL, [recipe_id])

        # Update for the user
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, username)
        ids = cursor.fetchone()[CREATED_RECIPES_STR].split(",")
        ids.remove(str(recipe_id))

        cursor.execute(UPDATE_CREATED_RECIPES_SQL, [','.join(ids), username])
        conn.commit()

        return return_message(good_message="Recipe Deleted!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "10", repr(e))


def _make_recipe_return(result):
    return {
        RECIPE_TITLE_STR: result[RECIPE_TITLE_STR],
        RECIPE_DESCRIPTION_STR: result[RECIPE_DESCRIPTION_STR],
        RECIPE_INGREDIENTS_STR: result[RECIPE_INGREDIENTS_STR],
        RECIPE_TUTORIAL_STR: result[RECIPE_TUTORIAL_STR],
        RECIPE_PICTURE_STR: result[RECIPE_PICTURE_STR],
        RECIPE_AUTHOR_STR: result[RECIPE_AUTHOR_STR],
        NUT_ALLERGY_STR: result[NUT_ALLERGY_STR],
        GLUTEN_FREE_STR: result[GLUTEN_FREE_STR],
        SPICINESS_LEVEL_STR: result[SPICINESS_LEVEL_STR],
        RECIPE_RATING_STR: result[RECIPE_TOTAL_RATING_STR] / result[RECIPE_NUMBER_OF_RATINGS_STR] \
            if result[RECIPE_TOTAL_RATING_STR] is not None else None,
    }


def update_recipe(body):
    """
    Updates the recipe in the db
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, RECIPE_ID_STR)
    if not all_good:
        return ret_dict
    recipe_id = ret_dict[RECIPE_ID_STR]

    recipe_params = [RECIPE_TITLE_STR, RECIPE_INGREDIENTS_STR, RECIPE_DESCRIPTION_STR, RECIPE_TUTORIAL_STR,
                     RECIPE_PRIVATE_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR, SPICINESS_LEVEL_STR, RECIPE_PICTURE_STR]
    all_good, ret_dict = get_params_if_exist(body, *recipe_params)
    if not all_good:
        return ret_dict
    new_recipe_params = [ret_dict[s] for s in recipe_params]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_RECIPE_BY_ID_SQL, [recipe_id])
        result = cursor.fetchone()

        # Error if there was no recipe, or if the user does not own that recipe
        if result is None:
            return error(ERROR_UNKNOWN_RECIPE, recipe_id)
        if result[RECIPE_AUTHOR_STR] != username:
            return error(ERROR_UNKNOWN_RECIPE, recipe_id)

        # Update new_recipe_params with the stuff from the db for everything that was not defined
        new_recipe_params = [(result[recipe_params[i]] if p is None else p) for i, p in enumerate(new_recipe_params)] + [recipe_id]

        cursor.execute(UPDATE_RECIPE_SQL, new_recipe_params)
        conn.commit()

        return return_message(good_message="Recipe Updated!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "11", repr(e))


def update_user_profile(body):
    """
    Update user profile info (not rated recipes, saved recipes, or created recipes)
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, NEW_USERNAME_STR, NEW_EMAIL_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR,
                                            SPICINESS_LEVEL_STR)
    if not all_good:
        return ret_dict

    # Get the old profile info first
    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, username)
        result = cursor.fetchone()

        new_username = result[USERNAME_STR] if ret_dict[NEW_USERNAME_STR] == '' else ret_dict[NEW_USERNAME_STR]
        new_email = result[EMAIL_STR] if ret_dict[NEW_EMAIL_STR] == '' else ret_dict[NEW_EMAIL_STR]
        new_nut, new_glut, new_spicy = ret_dict[NUT_ALLERGY_STR], ret_dict[GLUTEN_FREE_STR], ret_dict[
            SPICINESS_LEVEL_STR]

        # Check for username/email already exists
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, new_username)
        if cursor.fetchone() is not None:
            return error(ERROR_USERNAME_ALREADY_EXISTS, new_username)
        cursor.execute(GET_WHERE_EMAIL_LIKE_SQL, new_email)
        if cursor.fetchone() is not None:
            return error(ERROR_EMAIL_ALREADY_IN_USE, new_email)

        cursor.execute(UPDATE_USER_PROFILE_SQL, [new_username, new_email, new_nut, new_glut, new_spicy, username])
        conn.commit()

        return return_message(good_message="Profile Updated!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "12", repr(e))


def update_user_favorites(body):
    """
    Updates user favorites (either adds or removes)
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, RECIPE_ID_STR)
    if not all_good:
        return ret_dict
    recipe_id = ret_dict[RECIPE_ID_STR]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_RECIPE_BY_ID_SQL, abs(recipe_id))
        if cursor.fetchone() is None:
            return error(ERROR_UNKNOWN_RECIPE, abs(recipe_id))

        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])
        result = cursor.fetchone()[FAVORITED_RECIPES_STR]
        ids = [] if result == '' else result.split(",")

        if recipe_id < 0:
            recipe_id = -recipe_id
            if str(recipe_id) not in ids:
                return error(ERROR_RECIPE_NOT_FAVORITED, username, recipe_id)
            ids.remove(str(recipe_id))
        else:
            ids.append(str(recipe_id))

        cursor.execute(UPDATE_FAVORITED_RECIEPS_SQL, [','.join(ids), username])
        conn.commit()
        return return_message(good_message="User Favorites Updated!")

    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "13", repr(e))


def rate_recipe(body):
    """
    Either adds a new rating to the recipe and user, or updates a previous rating
    """
    all_good, username = _verify_login_credentials(body)
    if not all_good:
        return username

    all_good, ret_dict = get_cleaned_params(body, RECIPE_ID_STR, RECIPE_RATING_STR)
    if not all_good:
        return ret_dict
    recipe_id, rating = ret_dict[RECIPE_ID_STR], ret_dict[RECIPE_RATING_STR]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])
        result = cursor.fetchone()

        # Check if the user has rated this recipe before
        ids = [] if result[RATED_RECIPES_STR] == '' else result[RATED_RECIPES_STR].split(',')
        ids, ratings = ([], []) if len(ids) == 0 else ([i.split(':')[0] for i in ids], [i.split(':')[1] for i in ids])

        if str(recipe_id) in ids:
            recipe_rating_inc = rating - int(ratings[ids.index(recipe_id)])
            recipe_rating_count = 0
            ratings[ids.index(recipe_id)] = rating
        else:
            recipe_rating_inc = rating
            recipe_rating_count = 1
            ids.append(str(recipe_id))
            ratings.append(str(rating))

        # Make sure this recipe is not private
        cursor.execute(GET_RECIPE_BY_ID_SQL, [recipe_id])
        result = cursor.fetchone()
        if result is None or result[RECIPE_PRIVATE_STR] == 1:
            return error(ERROR_UNKNOWN_RECIPE, [recipe_id])

        # Update the recipe ratings
        cursor.execute(UPDATE_RECIPE_RATING_SQL, [recipe_rating_inc, recipe_rating_count, recipe_id])
        conn.commit()

        # Update the user info
        rated_recipes = ",".join([ids[i] + ':' + ratings[i] for i in range(len(ids))])
        cursor.execute(UPDATE_RATED_RECIPES_SQL, [rated_recipes, username])
        conn.commit()

        return return_message(good_message="Recipe Ratings Updated!")
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "14", repr(e))


def query_recipes(params):
    """
    Queries the database for recipes based on input
    """
    all_good_login, username = _verify_login_credentials(params)

    all_good, ret_dict = get_cleaned_params(params, QUERY_STR)
    if not all_good:
        return ret_dict
    query = ret_dict[QUERY_STR]

    all_good, ret_dict = get_params_if_exist(params, NUT_ALLERGY_STR, GLUTEN_FREE_STR, SPICINESS_LEVEL_STR)
    if not all_good:
        return ret_dict
    nut_allergy, gluten_free, spiciness = ret_dict[NUT_ALLERGY_STR], ret_dict[GLUTEN_FREE_STR], ret_dict[SPICINESS_LEVEL_STR]

    try:
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_ALL_PUBLIC_RECIPES_SQL)
        public_results = cursor.fetchall()
        if all_good_login:
            cursor.execute(GET_ALL_PRIVATE_RECIPES_SQL, [username])
            private_results = cursor.fetchall()
        else:
            private_results = []

        return do_query(query, nut_allergy, gluten_free, spiciness, public_results, private_results)
    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "15", repr(e))
