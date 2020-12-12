from lambda_code.s3_utils import *
from lambda_code.utils import gen_crypt, generate_verification_code, fix_email
import hashlib

# Test account info
TEST_ACCOUNT_VERIFIED_USERNAME = 'wingit_testing_account_verified'
TEST_ACCOUNT_VERIFIED_EMAIL = 'wingit.testing.account.verified@gmail.com'
TEST_ACCOUNT_PASSWORD_HASH = hashlib.sha256(b"TestPassword!1").hexdigest()

TEST_ACCOUNT_UNVERIFIED_USERNAME = 'wingit_testing_account_unverified'
TEST_ACCOUNT_UNVERIFIED_EMAIL = 'wingit.testing.account.unverified@gmail.com'

TEST_ACCOUNT_VERIFIED_KWARGS = {
    USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
    EMAIL_STR: fix_email(TEST_ACCOUNT_VERIFIED_EMAIL),
    VERIFICATION_CODE_STR: '',
    CREATION_TIME_STR: -1,
    PASSWORD_HASH_STR: gen_crypt(TEST_ACCOUNT_PASSWORD_HASH)
}

TEST_ACCOUNT_UNVERIFIED_KWARGS = {
    USERNAME_STR: TEST_ACCOUNT_UNVERIFIED_USERNAME,
    EMAIL_STR: fix_email(TEST_ACCOUNT_UNVERIFIED_EMAIL),
    VERIFICATION_CODE_STR: generate_verification_code(),
    CREATION_TIME_STR: -1,
    PASSWORD_HASH_STR: gen_crypt(TEST_ACCOUNT_PASSWORD_HASH)
}

JUST_WINGIT_USERNAME = 'JustWingit'
JUST_WINGIT_PASSWORD_HASH = hashlib.sha256(b"wingit!1").hexdigest()
JUST_WINGIT_EMAIL = "cse110wingit@gmail.com"

# SQL info
DELETE_TABLE_SQL = "DROP TABLE IF EXISTS %s"

# Data for making the sql database
_VARCHAR_SIZE = 255
_SERVER_PASSWORD_HASH_SIZE = len(gen_crypt("dfaonefdkaj"))

# Just need to input a tuple here with the string name for the column, and what datatype should be
_CREATE_USERS_TABLE_ARGS = [
    (USERNAME_STR, "varchar(%d) NOT NULL" % _VARCHAR_SIZE),
    (EMAIL_STR, "varchar(%d) NOT NULL" % _VARCHAR_SIZE),
    (VERIFICATION_CODE_STR, "char(%d)" % VERIFICATION_CODE_SIZE),
    (CREATION_TIME_STR, "bigint"),
    (PASSWORD_HASH_STR, "char(%d) NOT NULL" % _SERVER_PASSWORD_HASH_SIZE),
    (PASSWORD_CHANGE_CODE_STR, "char(%d)" % VERIFICATION_CODE_SIZE),
    (PASSWORD_CHANGE_CODE_CREATION_TIME_STR, "bigint"),
    (NUT_ALLERGY_STR, "tinyint DEFAULT 0"),
    (GLUTEN_FREE_STR, "tinyint DEFAULT 0"),
    (SPICINESS_LEVEL_STR, "int DEFAULT -1"),
    (FAVORITED_RECIPES_STR, "varchar(2000) DEFAULT ''"),
    (RATED_RECIPES_STR, "varchar(2000) DEFAULT ''"),
    (CREATED_RECIPES_STR, "varchar(2000) DEFAULT ''"),
]

CREATE_USERS_TABLE_SQL = """CREATE TABLE %s (
%s);""" % (USERS_TABLE_NAME, ''.join(_str + " " + _type + ",\n" for _str, _type in _CREATE_USERS_TABLE_ARGS)[:-2])

_CREATE_RECIPES_TABLE_ARGS = [
    (RECIPE_ID_STR, "int NOT NULL"),
    (RECIPE_TITLE_STR, "varchar(%d)" % _VARCHAR_SIZE),
    (RECIPE_TOTAL_RATING_STR, "int DEFAULT 0 NOT NULL"),
    (RECIPE_NUMBER_OF_RATINGS_STR, "int DEFAULT 0 NOT NULL"),
    (RECIPE_INGREDIENTS_STR, "TEXT"),
    (RECIPE_DESCRIPTION_STR, "TEXT"),
    (RECIPE_TUTORIAL_STR, "TEXT"),
    (RECIPE_PICTURE_STR, "char(%d)" % (len(S3_BUCKET_URL) + len(RECIPE_IMAGES_DIR) + RANDOM_S3_FILENAME_SIZE + 10)),
    (RECIPE_AUTHOR_STR, "varchar(%d) DEFAULT ''" % _VARCHAR_SIZE),
    (RECIPE_PRIVATE_STR, "tinyint"),
    (NUT_ALLERGY_STR, "tinyint DEFAULT 0 NOT NULL"),
    (GLUTEN_FREE_STR, "tinyint DEFAULT 0 NOT NULL"),
    (VEGETARIAN_STR, "tinyint DEFAULT 0 NOT NULL"),
    (SPICINESS_LEVEL_STR, "int DEFAULT 0"),
]

CREATE_RECIPES_TABLE_SQL = """CREATE TABLE %s (
%s
)
DEFAULT CHARSET=utf8
""" % (RECIPES_TABLE_NAME, ''.join(_str + " " + _type + ",\n" for _str, _type in _CREATE_RECIPES_TABLE_ARGS)[:-2])


def make_insert_sql(table_name, **kwargs):
    """
    Makes the sql and sql_args to insert all values in kwargs into table_name
    """
    column_names = ''.join(key + ", " for key, val in kwargs.items())[:-2]
    values_str = ''.join('%s, ' for key, val in kwargs.items())[:-2]
    return "INSERT INTO {0} ({1}) VALUES ({2})".format(table_name, column_names, values_str), \
           [val for key, val in kwargs.items()]


##################
# Java Constants #
##################


JAVA_CONSTANTS_FILE_DIR = "../app/src/main/java/com/example/projectwingit/utils"
JAVA_CONSTANTS_CLASS_NAME = "WingitLambdaConstants"
JAVA_CONSTANTS_FILE_PATH = "../app/src/main/java/com/example/projectwingit/utils/%s.java" % JAVA_CONSTANTS_CLASS_NAME
JAVA_PACKAGE_PATH = JAVA_CONSTANTS_FILE_DIR.replace('../app/src/main/java/', '').replace('/', '.')


def _gen_java_data(java_vars):
    ret = """package %(package)s;

/*
 * An automatically generated list of constants from the lambda API
 */

public class %(classname)s {
""" % {'package': JAVA_PACKAGE_PATH, 'classname': JAVA_CONSTANTS_CLASS_NAME}

    for s in java_vars:
        val = globals()[s]
        if isinstance(val, str):
            ret += "    public static final String %s = \"%s\";\n" % (s, val)
        elif isinstance(val, int):
            ret += "    public static final int %s = %d;\n" % (s, val)

    return ret + "}"


JAVA_CONSTANTS_FILE_DATA = \
    _gen_java_data(["API_URL", "RETURN_INFO_STR", "RETURN_ERROR_MESSAGE_STR", "RETURN_ERROR_CODE_STR", "EVENT_TYPE_STR",
                    "EVENT_CREATE_ACCOUNT_STR", "EVENT_LOGIN_STR", "PASSWORD_HASH_STR", "USERNAME_STR", "EMAIL_STR",
                    "NUT_ALLERGY_STR", "GLUTEN_FREE_STR", "SPICINESS_LEVEL_STR", "EVENT_CREATE_RECIPE_STR",
                    "EVENT_GET_RECIPE_STR", "RECIPE_ID_STR", "EVENT_DELETE_ACCOUNT_STR",
                    "EVENT_GET_PASSWORD_CHANGE_CODE_STR", "NEW_PASSWORD_HASH_STR",
                    "EVENT_CHANGE_PASSWORD_STR", "PASSWORD_CHANGE_CODE_STR", "EVENT_UPDATE_USER_PROFILE_STR",
                    "QUERY_STR", "EVENT_QUERY_RECIPES_STR", "RECIPE_TITLE_STR", "RECIPE_INGREDIENTS_STR",
                    "RECIPE_DESCRIPTION_STR", "RECIPE_TUTORIAL_STR", "RECIPE_PRIVATE_STR", "RECIPE_RATING_STR",
                    "EVENT_RATE_RECIPE_STR", "EVENT_DELETE_RECIPE_STR", "EVENT_UPDATE_RECIPE_STR",
                    "EVENT_UPDATE_USER_FAVORITES_STR", "RECIPE_PICTURE_STR", "CREATED_RECIPES_STR", "RATED_RECIPES_STR",
                    "FAVORITED_RECIPES_STR", "QUERY_RESULTS_STR", "PASSWORD_CHANGE_CODE_SIZE", "S3_REASON_STR",
                    "EVENT_GET_S3_URL_STR", "S3_REASON_UPLOAD_RECIPE_IMAGE", "S3_REASON_UPLOAD_USER_PROFILE_IMAGE",
                    "IMAGE_FILE_EXTENSION_STR", "VEGETARIAN_STR", "RECIPE_IMAGES_DIR", "NEW_USERNAME_STR",
                    "NEW_EMAIL_STR"])


###############
# MISC THINGS #
###############


IMPLEMENTED_HTTP_METHODS = [GET_REQUEST_STR, POST_REQUEST_STR, DELETE_REQUEST_STR]
