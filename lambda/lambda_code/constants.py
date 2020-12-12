"""
Contains constants for things that I want in constants
Also some functions that are base functions used everywhere

Most of these are just in case I change my mind on what I want things named
"""
import json

# The link to the api
API_URL = "https://mvmb9qdwti.execute-api.us-west-1.amazonaws.com/WingitProduction/wingitresource"

# The number of rounds to use with bcrypt salt, tried to keep time close to 1sec
BCRYPT_ROUNDS = 14

RETURN_INFO_STR = 'info'  # For when everything worked, this is the key for the message of goodness
RETURN_ERROR_MESSAGE_STR = 'error_message'  # The error message
RETURN_ERROR_CODE_STR = 'error_code'  # The error code

EVENT_TYPE_STR = 'event_type'
HTTP_METHOD_STR = 'httpMethod'
GET_REQUEST_STR = 'GET'
POST_REQUEST_STR = 'POST'
DELETE_REQUEST_STR = 'DELETE'
EVENT_CREATE_ACCOUNT_STR = 'create_account'
EVENT_VERIFY_ACCOUNT_STR = 'verify_account'
EVENT_DELETE_ACCOUNT_STR = 'delete_account'
EVENT_CHANGE_PASSWORD_STR = 'change_password'
EVENT_GET_PASSWORD_CHANGE_CODE_STR = 'get_password_change_code'
EVENT_UPDATE_USER_PROFILE_STR = 'update_user_profile'
EVENT_UPDATE_USER_FAVORITES_STR = 'update_user_favorites'
EVENT_UPDATE_RECIPE_STR = 'update_recipe'
EVENT_LOGIN_STR = 'login'
EVENT_GET_S3_URL_STR = 'get_s3'
EVENT_CREATE_RECIPE_STR = 'create_recipe'
EVENT_GET_RECIPE_STR = 'get_recipe'
EVENT_DELETE_RECIPE_STR = 'delete_recipe'
EVENT_RATE_RECIPE_STR = 'rate_recipe'
EVENT_QUERY_RECIPES_STR = 'query_recipes'
RECIPE_PICTURE_ID_STR = 'recipe_picture_id'

VERIFICATION_CODE_SIZE = 30  # The number of characters to use in the verification code
RANDOM_S3_FILENAME_SIZE = 30  # Number of chars in a random filename in the S3 bucket
PASSWORD_CHANGE_CODE_SIZE = 6  # Number of digits in the password change code
VERIFICATION_LINK = "{0}?event_type=verify_account&username=%s&verification_code=%s".format(API_URL)
VERIFICATION_EMAIL_HEADER = "\From: %s\nTo: %s\nSubject: %s\n\n%s"
VERIFICATION_EMAIL_SUBJECT = "Wingit Account Activation"

CHANGE_PASSWORD_TIMEOUT = 10 * 60 * 1000  # Timeout password change after 10 minutes

# The prompt that is sent to the user to activate their account
USER_ACTIVATION_PROMPT = """
Hello, %s!

Congratulations on creating your Wingit account! Endless chicken opportunties await you. In order to verify your account, please click the link below.

%s

If you did not create this account, then what are you doing with your life?
"""

PASSWORD_CHANGE_EMAIL = """
Hello, %s!

If you recently requested a password change, your code is below:
Password Change Code: %s

If you did not request this code, simply disregard this email.
"""

# Email/password for sending verification email
GMAIL_USER = "cse110wingit@gmail.com"
GMAIL_PASSWORD = 'teamWINGIT!'

MAX_USERNAME_SIZE = 64  # Max size for a username
PASSWORD_HASH_SIZE = 64  # Size of a sha256 hash in chars
HASH_CHARS = "abcdef0123456789"  # The chars that could exist in a sha256 has (lowercase)


################
# SQL THINGIES #
################


# Info for the database connection
RDS_ENDPOINT = "wingitdb.cv6ukx3546be.us-west-1.rds.amazonaws.com"
RDS_USERNAME = "admin"
RDS_PASSWORD = "teamWINGIT!"
RDS_DATABASE = "wingitdb"

USERS_TABLE_NAME = 'USERS'  # The SQL table name for users
RECIPES_TABLE_NAME = 'RECIPES'
RECIPE_ID_STR = 'recipe_id'
RECIPE_TITLE_STR = 'recipe_title'
RECIPE_TOTAL_RATING_STR = 'total_rating'
RECIPE_NUMBER_OF_RATINGS_STR = 'number_of_ratings'
RECIPE_INGREDIENTS_STR = 'recipe_ingredients'
RECIPE_DESCRIPTION_STR = 'recipe_description'
RECIPE_TUTORIAL_STR = 'recipe_tutorial'
RECIPE_PICTURE_STR = 'recipe_picture'
RECIPE_AUTHOR_STR = 'recipe_author'
RECIPE_PRIVATE_STR = 'recipe_private'
RECIPE_RATING_STR = 'recipe_rating'
FAVORITED_RECIPES_STR = 'favorited_recipes'
NEXT_RECIPE_ID_STR = 'next_recipe_id'
NEW_USERNAME_STR = 'new_username'
NEW_EMAIL_STR = 'new_email'
IMAGE_FILE_EXTENSION_STR = 'image_file_ext'
CREATED_RECIPES_STR = 'created_recipes'
RATED_RECIPES_STR = 'rated_recipes'
NUT_ALLERGY_STR = 'nut_allergy'
SPICINESS_LEVEL_STR = 'spiciness_level'
GLUTEN_FREE_STR = 'gluten_free'
VEGETARIAN_STR = 'vegetarian'
PASSWORD_HASH_STR = 'password_hash'  # Password_hash name on database
NEW_PASSWORD_HASH_STR = 'new_hash'
QUERY_RESULTS_STR = 'query_results'
QUERY_STR = 'query'
USERNAME_STR = 'username'  # I think you get the point now
VERIFICATION_CODE_STR = 'verification_code'
EMAIL_STR = 'email'
CREATION_TIME_STR = 'creation_time'
S3_REASON_STR = 's3_reason'
PASSWORD_CHANGE_CODE_STR = 'password_change_code'
PASSWORD_CHANGE_CODE_CREATION_TIME_STR = 'password_change_code_creation_time'

USER_PREFERENCES_FIELDS = [USERNAME_STR, EMAIL_STR, NUT_ALLERGY_STR, SPICINESS_LEVEL_STR, GLUTEN_FREE_STR,
                           CREATED_RECIPES_STR, RATED_RECIPES_STR, FAVORITED_RECIPES_STR]

# Create the user account
CREATE_ACCOUNT_SQL = "INSERT INTO {0} ({1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}) VALUES (%s, %s, %s, %s, %s, %s, %s, %s)" \
    .format(USERS_TABLE_NAME, USERNAME_STR, EMAIL_STR, VERIFICATION_CODE_STR, CREATION_TIME_STR, PASSWORD_HASH_STR,
            NUT_ALLERGY_STR, GLUTEN_FREE_STR, SPICINESS_LEVEL_STR)

# Update a user's password
UPDATE_PASSWORD_SQL = "UPDATE {0} SET {1}=%s, {2}='' WHERE {3} LIKE %s".format(USERS_TABLE_NAME, PASSWORD_HASH_STR,
                                                                               PASSWORD_CHANGE_CODE_STR, USERNAME_STR)

UPDATE_PASSWORD_CHANGE_CODE_SQL = "UPDATE {0} SET {1}=%s, {2}=%d WHERE {3} LIKE %s".format(USERS_TABLE_NAME,
                                                                                           PASSWORD_CHANGE_CODE_STR,
                                                                                           PASSWORD_CHANGE_CODE_CREATION_TIME_STR,
                                                                                           USERNAME_STR)

# Get rows where [column] LIKE [value]
GET_WHERE_USERNAME_LIKE_SQL = "SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR)
GET_WHERE_EMAIL_LIKE_SQL = "SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, EMAIL_STR)
GET_RECIPE_BY_ID_SQL = "SELECT * FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR)

# Update user verification code
UPDATE_VERIFICATION_CODE_SQL = "UPDATE {0} SET {1} = '' WHERE {2} LIKE %s" \
    .format(USERS_TABLE_NAME, VERIFICATION_CODE_STR, USERNAME_STR)

DELETE_ACCOUNT_SQL = "DELETE FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR)
DELETE_RECIPE_SQL = "DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR)

GET_NEXT_RECIPE_ID_SQL = "SELECT COALESCE(MAX(%s) + 2,1) AS %s FROM %s;" % (
RECIPE_ID_STR, NEXT_RECIPE_ID_STR, RECIPES_TABLE_NAME)
UPDATE_CREATED_RECIPES_SQL = "UPDATE {0} SET {1} = %s WHERE {2} LIKE %s".format(USERS_TABLE_NAME, CREATED_RECIPES_STR,
                                                                                USERNAME_STR)

CREATE_RECIPE_SQL = "INSERT INTO {0} ({1}, {2}, {3}, {4}, {5}, {6}, {7}, {8}, {9}, {10}, {11}, {12}) VALUES (%s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s, %s)" \
    .format(RECIPES_TABLE_NAME, RECIPE_ID_STR, RECIPE_TITLE_STR, RECIPE_INGREDIENTS_STR, RECIPE_DESCRIPTION_STR,
            RECIPE_TUTORIAL_STR, RECIPE_PRIVATE_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR, VEGETARIAN_STR, SPICINESS_LEVEL_STR,
            RECIPE_PICTURE_STR, RECIPE_AUTHOR_STR)

UPDATE_USER_PROFILE_SQL = "UPDATE {0} SET {1}=%s, {2}=%s, {3}=%s, {4}=%s, {5}=%s WHERE {1} LIKE %s" \
    .format(USERS_TABLE_NAME, USERNAME_STR, EMAIL_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR, SPICINESS_LEVEL_STR)

UPDATE_FAVORITED_RECIEPS_SQL = "UPDATE {0} SET {1}=%s WHERE {2} LIKE %s" \
    .format(USERS_TABLE_NAME, FAVORITED_RECIPES_STR, USERNAME_STR)

UPDATE_RECIPE_SQL = "UPDATE {0} SET {1}=%s, {2}=%s, {3}=%s, {4}=%s, {5}=%s, {6}=%s, {7}=%s, {8}=%s, {9}=%s, {10}=%s WHERE {11}=%s" \
    .format(RECIPES_TABLE_NAME, RECIPE_TITLE_STR, RECIPE_INGREDIENTS_STR, RECIPE_DESCRIPTION_STR, RECIPE_TUTORIAL_STR,
            RECIPE_PRIVATE_STR, NUT_ALLERGY_STR, GLUTEN_FREE_STR, VEGETARIAN_STR, SPICINESS_LEVEL_STR, RECIPE_PICTURE_STR,
            RECIPE_ID_STR)

UPDATE_RECIPE_RATING_SQL = "UPDATE {0} SET {1}={1}+%s, {2}={2}+%s WHERE {3}=%s" \
    .format(RECIPES_TABLE_NAME, RECIPE_TOTAL_RATING_STR, RECIPE_NUMBER_OF_RATINGS_STR, RECIPE_ID_STR)

UPDATE_RATED_RECIPES_SQL = "UPDATE {0} SET {1}=%s WHERE {2} LIKE %s" \
    .format(USERS_TABLE_NAME, RATED_RECIPES_STR, USERNAME_STR)

GET_ALL_PUBLIC_RECIPES_SQL = "SELECT * FROM {0} WHERE {1}!=1".format(RECIPES_TABLE_NAME, RECIPE_PRIVATE_STR)
GET_ALL_PRIVATE_RECIPES_SQL = "SELECT * FROM {0} WHERE {1}=1 AND {2} LIKE %s".format(RECIPES_TABLE_NAME,
                                                                                     RECIPE_PRIVATE_STR,
                                                                                     RECIPE_AUTHOR_STR)


def return_message(good_message=None, data=None):
    """
    Builds a return message to send back to the client if everything was processed (could be an error, or not, but at
        least is was handled.
    :param good_message: if not None, then add an extra key/value to the return dict with key=RETURN_GOOD_MESSAGE_STR
        and value=good_message
    :param data: a dictionary of the rest of the data to return
    """
    if data is None:
        data = {}
    if good_message is not None:
        data[RETURN_INFO_STR] = good_message
    return {
        'statusCode': 200,
        'body': json.dumps(data)
    }
