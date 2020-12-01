from lambda_code.validate_email import validate_email
from lambda_code.errors import *
from lambda_code.constants import *
import time
import random
import smtplib
import hashlib
import lambda_code.pymysql as pymysql
import string
import pickle
import re


_PARAMS_BOOLEAN_NAMES = [NUT_ALLERGY_STR, GLUTEN_FREE_STR, RECIPE_PRIVATE_STR]
_PARAMS_BOOLEAN_ERRORS = [ERROR_INVALID_NUT_ALLERGY, ERROR_INVALID_GLUTEN_FREE, ERROR_INVALID_RECIPE_PRIVATE]


def username_valid(username):
    """
    Checks to make sure the username contains only the characters:
        - A-Z
        - a-z
        - 0-9
        - Underscore "_"
    :param username: the string username to check
    :returns: True or False
    """

    # Make sure the username is not empty or too large
    if len(username) == 0 or len(username) > MAX_USERNAME_SIZE:
        return False

    for c in username:
        if not c.isalnum() and not c == "_":
            return False
    return True


def current_time():
    """
    Return the current time in millis to keep track of when accounts were
    created, to delete non-verified accounts after a certain amount of time
    """
    return round(time.time() * 1000)


def generate_verification_code():
    """
    Generates a verification code to verify the account, full of random
    characters
    """
    return ''.join(random.choice(string.ascii_letters) for i in range(VERIFICATION_CODE_SIZE))


def send_activation_email(username, email, verification_code):
    """
    Sends the activation email/link to the given username and email
    """
    link = VERIFICATION_LINK % (username, verification_code)

    message = VERIFICATION_EMAIL_HEADER % (GMAIL_USER, email, VERIFICATION_EMAIL_SUBJECT,
                                           USER_ACTIVATION_PROMPT % (username, link))

    # Set up the server and send the message
    try:

        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.ehlo()
        server.starttls()
        server.login(GMAIL_USER, GMAIL_PASSWORD)
        server.sendmail(GMAIL_USER, email, message)
        server.close()

        # Return something saying everything went alright
        return return_message(good_message='Account Created!')

    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "16", repr(e))


def send_password_change_code_email(username, email, code):
    """
    Sends the activation email/link to the given username and email
    """
    message = PASSWORD_CHANGE_EMAIL % (username, code)

    # Set up the server and send the message
    try:

        server = smtplib.SMTP('smtp.gmail.com', 587)
        server.ehlo()
        server.starttls()
        server.login(GMAIL_USER, GMAIL_PASSWORD)
        server.sendmail(GMAIL_USER, email, message)
        server.close()

        # Return something saying everything went alright
        return return_message(good_message='Password Change Email Sent!')

    except Exception as e:
        return error(ERROR_UNKNOWN_ERROR, "17", repr(e))


def fix_email(email):
    """
    Removes all periods before the '@' and changes to all lowercase to get unique email address
    """
    email = email.lower()

    # Only remove before '@', or everywhere if there is no '@'
    idx = email.index('@') if '@' in email else len(email)
    return email[:idx].replace('.', '') + email[idx:]


def get_params_if_exist(params, *str_list):
    """
    Gets all of the cleaned params if they exist in params, otherwise None
    """
    ret = {}
    for s in str_list:
        if s not in params:
            ret[s] = None
            continue

        all_good, ret_dict = get_cleaned_params(params, s)
        if not all_good:
            return False, ret_dict
        ret[s] = ret_dict[s]
    return True, ret


def get_cleaned_params(params, *str_list):
    """
    Gets all of the parameters requested in str_list, cleaned up depending on the param
    Each param is immediately turned into a string before cleaning, and filtered further from there depending
        on what param it is

    If any error occurs, then a 2-tuple is returned: False, and the error that should be returned to the user

    Otherwise, the tuple returned is of size len(str_list) + 1 with the first element being True, and the rest of the
        elements as the cleaned parameters in the order of str_list

    :param params: the params
    :param str_list: the list of string params keys to get and clean
    """
    found, vals, val_str_names = _get_str_in_params(list(str_list), params)
    if not found:
        return False, vals

    clean_vals = []
    for val, val_str_name in zip(vals, val_str_names):
        all_good, val = _check_good_param(val, val_str_name)
        if not all_good:
            return False, val
        clean_vals.append(val)

    return True, dict(zip(val_str_names, clean_vals))


def _get_str_in_params(_str, params):
    """
    If _str a tuple: call _get_str_in_params with all elements, and return good for first one that exists
    if _str a list: call _get_str_in_params with all elements, assert all exists and return list
    if _str a string: make sure _str exists in params, then return singleton list of element
    """
    if isinstance(_str, tuple):
        for s in _str:
            found, vals, val_str_names = _get_str_in_params(s, params)
            if found:
                return True, vals, val_str_names
        return False, error(ERROR_MISSING_PARAMS, _str), None

    elif isinstance(_str, list):
        all_vals = []
        all_val_str_names = []
        for s in _str:
            found, vals, val_str_names = _get_str_in_params(s, params)
            if not found:
                return False, error(ERROR_MISSING_PARAMS, s), None
            all_vals += vals
            all_val_str_names += val_str_names
        return True, all_vals, all_val_str_names

    else:
        if _str not in params:
            return False, error(ERROR_MISSING_PARAMS, _str), None
        return True, [params[_str]], [_str]


def _check_good_param(val, val_str_name):
    """
    Check that passed values are good, and return error if not
    """
    if val_str_name == USERNAME_STR:
        val = val.lower()
        if not username_valid(val):
            return False, error(ERROR_INVALID_USERNAME, val)

    elif val_str_name == EMAIL_STR:
        val = fix_email(val)
        if not validate_email(val):
            return False, error(ERROR_INVALID_EMAIL, val)

    elif val_str_name == PASSWORD_HASH_STR:
        val = val.lower()
        if len(val) != PASSWORD_HASH_SIZE or not all(c in HASH_CHARS for c in val):
            return False, error(ERROR_INVALID_PASSWORD_HASH)

    elif val_str_name in _PARAMS_BOOLEAN_NAMES:
        error_name = list(zip(_PARAMS_BOOLEAN_ERRORS, _PARAMS_BOOLEAN_NAMES))[_PARAMS_BOOLEAN_NAMES.index(val_str_name)][0]
        if isinstance(val, int):
            if val != 0 and val != 1:
                return False, error(error_name, val)
            val = val == 1
        elif not isinstance(val, bool):
            val = val.lower()
            if val not in ['true', 'false', '1', '0']:
                return False, error(error_name, val)
            val = val == 'true' or val == '1'

    elif val_str_name == SPICINESS_LEVEL_STR:
        try:
            val = int(val)
            if not -1 <= val <= 5:
                raise ValueError()
        except:
            return False, error(ERROR_INVALID_SPICINESS, val)

    elif val_str_name == RECIPE_RATING_STR:
        try:
            val = int(val)
            if not 0 <= val <= 5:
                raise ValueError()
        except:
            return False, error(ERROR_INVALID_RATING, val)

    elif val_str_name == RECIPE_ID_STR:
        try:
            val = int(val)
        except:
            return False, error(ERROR_INVALID_RECIPE_ID, val)

    elif val_str_name == QUERY_STR:
        val = val.replace("\t", " ").replace("\n", " ")
        while "  " in val:
            val = val.replace("  ", " ")
        if val.startswith(" "):
            val = val[1:]
        if val.endswith(" "):
            val = val[:-1]

    return True, val


def get_possible_params(info, params=(), defaults=()):
    """
    Returns params if they exist in info, otherwise fill with defaults (or None if no defaults)
    """
    ret = [True]
    for i, s in enumerate(params):
        if s in info:
            all_good, val = _check_good_param(info[s], s)
            if not all_good:
                return False, val
            ret.append(val)
        elif i < len(defaults):
            ret.append(defaults[i])
        else:
            ret.append(None)
    return ret


def gen_crypt(password_hash):
    """
    Generates the hash and salt to store in the password_hash in the database
    """
    # Make a random salt using the verification code
    salt = generate_verification_code()
    return salt + hashlib.sha256(str.encode(password_hash + salt)).hexdigest()


def password_correct(password_hash, server_password_hash):
    """
    Returns true if the password matches the given password hash
    """
    return server_password_hash[VERIFICATION_CODE_SIZE:] == \
           hashlib.sha256(str.encode(password_hash + server_password_hash[:VERIFICATION_CODE_SIZE])).hexdigest()


def get_new_db_conn():
    """
    Returns a new connection to the database
    """
    return pymysql.connect(RDS_ENDPOINT, user=RDS_USERNAME, passwd=RDS_PASSWORD, db=RDS_DATABASE,
                           cursorclass=pymysql.cursors.DictCursor)


def random_password_change_code():
    """
    Returns a random code for the user to change their password
    """
    return ''.join([random.choice("0123456789") for i in range(PASSWORD_CHANGE_CODE_SIZE)])


##################
# Query Thingies #
##################


def _get_stop_words():
    with open("./lambda_code/stop_words.pkl", 'rb') as f:
        return pickle.load(f)


QUERY_MIN_RETURN_VALUE = 7
QUERY_PRIVATE_START_VAL = 3
QUERY_SAME_NUT_ALLERGY = 9
QUERY_SAME_GLUTEN_FREE = 7
QUERY_SPICINESS_MULT = 2
QUERY_START_OF_TEXT_MULT = 2
QUERY_END_OF_TEXT_MULT = 0.5
QUERY_TITLE_STR_MULT = 8
QUERY_OTHER_STR_MULT = 1
QUERY_AUTHOR_STR_VAL = 5


def do_query(query, nut_allergy, gluten_free, spiciness, public_results, private_results, top_n=10):
    """
    The actual search engine bit
    """
    stop_words = _get_stop_words()

    def _get_indices_in_str(q, _str):
        _str = _str.lower()
        return [m.start() for m in re.finditer(q, _str)], len(_str)

    def _lerp(_idx, _size):
        return QUERY_END_OF_TEXT_MULT + (1 - (_idx / _size)) * (QUERY_START_OF_TEXT_MULT - QUERY_END_OF_TEXT_MULT)

    def _get_recipe_query_value(r, public=True):
        val = QUERY_PRIVATE_START_VAL if not public else 0

        # Nut allergy
        if nut_allergy is not None and r[NUT_ALLERGY_STR] == (1 if nut_allergy else 0):
            val += QUERY_SAME_NUT_ALLERGY

        # Gluten free
        if gluten_free is not None and r[GLUTEN_FREE_STR] == (1 if gluten_free else 0):
            val += QUERY_SAME_GLUTEN_FREE

        # Spiciness
        if spiciness is not None and spiciness != -1:
            val += (5 - abs(spiciness - r[SPICINESS_LEVEL_STR])) * QUERY_SPICINESS_MULT

        # Go through each word in query
        for word in [w for w in query.lower().split(" ") if w not in stop_words]:
            if len(word) == 0:
                continue

            # Check if the author matches the word
            if r[RECIPE_AUTHOR_STR].lower() == word:
                val += QUERY_AUTHOR_STR_VAL

            def _check_start_and_end(_word, _str, m):
                v = 0
                if _str.lower().startswith(word):
                    v += QUERY_START_OF_TEXT_MULT * m
                if _str.lower().endswith(word):
                    v += QUERY_END_OF_TEXT_MULT * m
                return v

            # Check the starts and ends of words
            val += _check_start_and_end(word, r[RECIPE_TITLE_STR], QUERY_TITLE_STR_MULT)
            for r_str in [r[RECIPE_INGREDIENTS_STR], r[RECIPE_DESCRIPTION_STR], r[RECIPE_TUTORIAL_STR]]:
                val += _check_start_and_end(word, r_str, QUERY_OTHER_STR_MULT)

            # Convert word to have spaces as delimiter for future thingies
            word = " %s " % word

            def _add_to_val(indices, _size, m=QUERY_OTHER_STR_MULT):
                v = 0
                for idx in indices:
                    v += _lerp(idx, _size) * m
                return v

            # Check the title, ingredients, description, tutorial
            val += _add_to_val(*_get_indices_in_str(word, r[RECIPE_TITLE_STR]), m=QUERY_TITLE_STR_MULT)
            val += _add_to_val(*_get_indices_in_str(word, r[RECIPE_INGREDIENTS_STR]))
            val += _add_to_val(*_get_indices_in_str(word, r[RECIPE_DESCRIPTION_STR]))
            val += _add_to_val(*_get_indices_in_str(word, r[RECIPE_TUTORIAL_STR]))

        return val

    # Perform search value for each result
    recipes = []
    for result in public_results:
        recipes.append((result[RECIPE_ID_STR], _get_recipe_query_value(result, public=True)))
    for result in private_results:
        recipes.append((result[RECIPE_ID_STR], _get_recipe_query_value(result, public=False)))

    # Sort the recipe tuples by their value
    recipes = list(sorted(recipes, key=lambda x: x[1], reverse=True))
    recipes = [r for r in recipes if r[1] > QUERY_MIN_RETURN_VALUE]

    # Return the top_n result ids
    ret_recipes = recipes[:min(top_n, len(recipes))]
    return return_message(good_message="Queried Recipes!", data={
        QUERY_RESULTS_STR: ','.join([str(r[0]) for r in ret_recipes])
    })
