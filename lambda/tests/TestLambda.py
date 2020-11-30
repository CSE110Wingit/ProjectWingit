"""
Test cases for the lambda API
"""
from tests.LambdaTestUtils import *
from lambda_code.errors import *
from BuildConstants import *
from lambda_code.s3_utils import *
from lambda_code.utils import get_new_db_conn


class TestLambda:
    """
    If you want to add a test method to this class, make sure the method name starts with "test_" and takes no args
    so that the test_all() function will find it and test it, and make sure nothing else in this class starts with
    "test_" so there are no collisions
    """

    @staticmethod
    def assert_true(b, message='No message'):
        """
        If b is not true, then raise an error
        """
        if not b:
            raise ValueError('%s is not True:\n%s' % (b, message))

    @staticmethod
    def assert_equal(a, b, message='No message'):
        """
        If a != b, then raise an error
        """
        if a != b:
            raise ValueError('%s != %s:\n %s' % (a, b, message))

    @staticmethod
    def _cpy_params(params, error_tup):
        """
        Copies and updates the params input for a few tests that need it
        """
        _params = params.copy()
        _params['error_tup'] = error_tup
        return _params

    def assert_no_server_error(self, **params):
        """
        Asserts there is no error from the server
        """
        response = request(**params)
        self.assert_true(RETURN_ERROR_CODE_STR not in response, "Error from server with params %s:\n%s" %
                         (params, response))
        return response

    def assert_server_error(self, error_tup=None, **params):
        """
        Asserts a server-side error response
        """
        response = request(**params)
        self.assert_true(RETURN_ERROR_CODE_STR in response,
                         "No error from params: %s, \nFull message: %s" % (params, response))

        if error_tup is not None:
            err_str = "Error codes not equal for expected code '%s', instead got '%s'\nFull Error: %s\nParams: %s"
            self.assert_equal(response[RETURN_ERROR_CODE_STR], error_tup[0],
                              err_str % (ALL_ERROR_NAMES_BY_CODE[error_tup[0]],
                                         ALL_ERROR_NAMES_BY_CODE[response[RETURN_ERROR_CODE_STR]], response, params))

    def assert_server_handles_bad_username(self, **params):
        """
        Asserts the server knows how to handle bad usernames no matter the event that is using them
        """
        _params = self._cpy_params(params, ERROR_INVALID_USERNAME)

        # Incorrect size
        _params[USERNAME_STR] = ''
        self.assert_server_error(**_params)

        _params[USERNAME_STR] = random_str(MAX_USERNAME_SIZE + 1)
        self.assert_server_error(**_params)

        _params[USERNAME_STR] = random_str(MAX_USERNAME_SIZE * 7)
        self.assert_server_error(**_params)

        # Invalid characters
        _params[USERNAME_STR] = ' '
        self.assert_server_error(**_params)

        _params[USERNAME_STR] = 'good_user_name_ohno!'
        self.assert_server_error(**_params)

        _params[USERNAME_STR] = random_str(MAX_USERNAME_SIZE, all_ascii=True)[:-1] + "`"
        self.assert_server_error(**_params)

        _params[USERNAME_STR] = '\"\';'
        self.assert_server_error(**_params)

    def assert_server_handles_bad_email(self, **params):
        """
        Asserts the server knows how to handle bad emails no matter the event that is using them
        """
        _params = self._cpy_params(params, ERROR_INVALID_EMAIL)

        # Doesn't have @
        _params[EMAIL_STR] = ""
        self.assert_server_error(**_params)

        _params[EMAIL_STR] = "apples.com"
        self.assert_server_error(**_params)

        _params[EMAIL_STR] = random_str(64, all_ascii=False).replace('@', '')
        self.assert_server_error(**_params)

        # Has multiple @
        _params[EMAIL_STR] = "apples@j@gmail.com"
        self.assert_server_error(**_params)

        # Nothing before/after @
        _params[EMAIL_STR] = "@gmail.com"
        self.assert_server_error(**_params)

        _params[EMAIL_STR] = "good@"
        self.assert_server_error(**_params)

        # Multiple periods after @ (adjacent)
        _params[EMAIL_STR] = "good@gmail..com"
        self.assert_server_error(**_params)

    def assert_server_handles_bad_password_hash(self, **params):
        """
        Asserts the server knows how to handle bad passwords
        """
        _params = self._cpy_params(params, ERROR_INVALID_PASSWORD_HASH)

        # Not right size
        _params[PASSWORD_HASH_STR] = ''
        self.assert_server_error(**_params)

        _params[PASSWORD_HASH_STR] = random_valid_password_hash()[:-1]
        self.assert_server_error(**_params)

        # Bad chars
        _params[PASSWORD_HASH_STR] = random_valid_password_hash()[:-1] + 'g'
        self.assert_server_error(**_params)

        _params[PASSWORD_HASH_STR] = random_str(PASSWORD_HASH_SIZE, all_ascii=False)
        self.assert_server_error(**_params)

    def assert_server_handles_bad_http_method(self, **params):
        """
        Asserts the server knows how to handle bad http methods
        """
        _params = self._cpy_params(params, ERROR_UNKNOWN_EVENT_TYPE)

        for http_method in [h for h in IMPLEMENTED_HTTP_METHODS if h != _params[HTTP_METHOD_STR]]:
            _params[HTTP_METHOD_STR] = http_method
            self.assert_server_error(**_params)

    def assert_server_handles_invalid_inputs(self, **params):
        """
        Used to more easily call the bad username, email, password_hash so it is done correctly
        Each value passed in params will be tested for invalid input (if it has a test below)
        """
        # Check not enough/correct args
        self.assert_server_missing_params(**params)

        # Check that it handles full request but wrong http method
        self.assert_server_handles_bad_http_method(**params)

        # Check invalid individual args
        if USERNAME_STR in params:
            self.assert_server_handles_bad_username(**params)
        if EMAIL_STR in params:
            self.assert_server_handles_bad_email(**params)
        if PASSWORD_HASH_STR in params:
            self.assert_server_handles_bad_password_hash(**params)

    def assert_server_missing_params(self, **params):
        """
        Switches through permutations of missing parameters to make sure the server only accepts if all params
            are accounted for. Assumes error_code: 'ERROR_MISSING_PARAMS'
        """
        keys = [k for k in params.keys() if k != EVENT_TYPE_STR and k != HTTP_METHOD_STR]

        # Go through every permutation using binary (except the last one because that should work)
        for s in get_binary_permutations(len(keys))[:-1]:
            # Get all the params that have a '1' in their index
            curr_params = {keys[i]: params[keys[i]] for i, b in enumerate(s) if b == '1'}
            curr_params.update({EVENT_TYPE_STR: params[EVENT_TYPE_STR], HTTP_METHOD_STR: params[HTTP_METHOD_STR]})
            self.assert_server_error(error_tup=ERROR_MISSING_PARAMS, **curr_params)

    def test_account_creation_fails(self):
        """
        Testing the user account creation API
        """

        # The full set of parameters that could theoretically pass invalid checks
        password = random_valid_password_hash()
        username = random_valid_username()
        params = {
            EVENT_TYPE_STR: EVENT_CREATE_ACCOUNT_STR,
            USERNAME_STR: username,
            EMAIL_STR: random_valid_email(),
            PASSWORD_HASH_STR: password,
            HTTP_METHOD_STR: POST_REQUEST_STR
        }

        # Check inputs are missing/invalid
        self.assert_server_handles_invalid_inputs(**params)

        # Doing usernames that have already been taken (including caps/no caps/periods in email)
        params['error_tup'] = ERROR_USERNAME_ALREADY_EXISTS

        #           Lowercase
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        self.assert_server_error(**params)

        #           Uppercase
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME.upper()
        self.assert_server_error(**params)

        # Doing emails that have already been taken
        params['error_tup'] = ERROR_EMAIL_ALREADY_IN_USE
        params[USERNAME_STR] = username

        #           Normal email
        params[EMAIL_STR] = TEST_ACCOUNT_VERIFIED_EMAIL
        self.assert_server_error(**params)

        #           No periods
        params[EMAIL_STR] = TEST_ACCOUNT_VERIFIED_EMAIL.split('@')[0].replace('.', '') \
                            + '@' + TEST_ACCOUNT_VERIFIED_EMAIL.split('@')[1]
        self.assert_server_error(**params)

        #           Lots of periods
        params[EMAIL_STR] = TEST_ACCOUNT_VERIFIED_EMAIL[:4] + "......" + TEST_ACCOUNT_VERIFIED_EMAIL[4:]
        self.assert_server_error(**params)

        # Bad user preferences
        params = {
            EMAIL_STR: random_valid_email(),
            USERNAME_STR: random_valid_username(),
            PASSWORD_HASH_STR: random_valid_password_hash(),
            NUT_ALLERGY_STR: True,
            GLUTEN_FREE_STR: True,
            SPICINESS_LEVEL_STR: "sdsda",
            HTTP_METHOD_STR: POST_REQUEST_STR,
            EVENT_TYPE_STR: EVENT_CREATE_ACCOUNT_STR
        }

        self.assert_server_error(ERROR_INVALID_SPICINESS, **params)
        params[SPICINESS_LEVEL_STR] = -2
        self.assert_server_error(ERROR_INVALID_SPICINESS, **params)
        params[SPICINESS_LEVEL_STR] = 6
        self.assert_server_error(ERROR_INVALID_SPICINESS, **params)
        params[SPICINESS_LEVEL_STR] = True
        self.assert_server_error(ERROR_INVALID_SPICINESS, **params)

        params[SPICINESS_LEVEL_STR] = 5
        params[NUT_ALLERGY_STR] = 0
        self.assert_server_error(ERROR_INVALID_NUT_ALLERGY, **params)

        params[NUT_ALLERGY_STR] = True
        params[GLUTEN_FREE_STR] = 0
        self.assert_server_error(ERROR_INVALID_GLUTEN_FREE, **params)

    def test_account_login(self):
        """
        Testing login to an existing user account
        """
        params = {
            EVENT_TYPE_STR: EVENT_LOGIN_STR,
            EMAIL_STR: random_valid_email(),
            PASSWORD_HASH_STR: random_valid_password_hash(),
            HTTP_METHOD_STR: GET_REQUEST_STR
        }

        # Check not enough/correct args for both username and email
        self.assert_server_handles_invalid_inputs(**params)

        del params[EMAIL_STR]
        params[USERNAME_STR] = random_valid_username()
        self.assert_server_handles_invalid_inputs(**params)

        # Check incorrect username/email, but a functional password
        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_USERNAME_DOES_NOT_EXIST,
                  PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH, USERNAME_STR: random_valid_username(),
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_EMAIL_DOES_NOT_EXIST,
                  PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH, EMAIL_STR: random_valid_email(),
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        # Check correct username/email, but wrong password
        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_INCORRECT_PASSWORD,
                  PASSWORD_HASH_STR: random_valid_password_hash(), USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_INCORRECT_PASSWORD,
                  PASSWORD_HASH_STR: random_valid_password_hash(), EMAIL_STR: TEST_ACCOUNT_VERIFIED_EMAIL,
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        # Check correct everything, but an unverified account
        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_ACCOUNT_UNVERIFIED,
                  PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH, EMAIL_STR: TEST_ACCOUNT_UNVERIFIED_EMAIL,
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, 'error_tup': ERROR_ACCOUNT_UNVERIFIED,
                  PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH, USERNAME_STR: TEST_ACCOUNT_UNVERIFIED_USERNAME,
                  HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_server_error(**params)

        # Check logging in with email and username both work
        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
                  EMAIL_STR: TEST_ACCOUNT_VERIFIED_EMAIL, HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_no_server_error(**params)

        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
                  USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME, HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_no_server_error(**params)

        # Check logging in with password_hash that has caps works
        params = {EVENT_TYPE_STR: EVENT_LOGIN_STR, PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH.upper(),
                  USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME, HTTP_METHOD_STR: GET_REQUEST_STR}
        self.assert_no_server_error(**params)

    def test_account_deletion_fails(self):
        """
        Deleting a user's account
        """
        params = {
            EVENT_TYPE_STR: EVENT_DELETE_ACCOUNT_STR,
            USERNAME_STR: TEST_ACCOUNT_UNVERIFIED_USERNAME,
            HTTP_METHOD_STR: DELETE_REQUEST_STR,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Invalid username
        params[USERNAME_STR] = random_valid_username()
        self.assert_server_error(ERROR_USERNAME_DOES_NOT_EXIST, **params)

        # Invalid email
        del params[USERNAME_STR]
        params[EMAIL_STR] = random_valid_email()
        self.assert_server_error(ERROR_EMAIL_DOES_NOT_EXIST, **params)

        # Password and hash do not match
        del params[EMAIL_STR]
        params[USERNAME_STR] = TEST_ACCOUNT_UNVERIFIED_USERNAME
        params[PASSWORD_HASH_STR] = random_valid_password_hash()
        self.assert_server_error(ERROR_INCORRECT_PASSWORD, **params)

    def testlast_create_delete_account(self):
        """
        Tests the ability to create and delete an account
        """
        username = random_valid_username()
        password = b"ThiIsAnpassword!2  3"
        password_hash = hashlib.sha256(password).hexdigest()
        email = random_valid_email()
        params = {
            EVENT_TYPE_STR: EVENT_CREATE_ACCOUNT_STR,
            USERNAME_STR: username,
            EMAIL_STR: email,
            PASSWORD_HASH_STR: password_hash,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            SPICINESS_LEVEL_STR: 3,
            NUT_ALLERGY_STR: False,
            GLUTEN_FREE_STR: True
        }

        # Create the account
        self.assert_no_server_error(**params)

        # Username and email should be all lower
        username = username.lower()
        email = email.lower()

        _sql = "SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR)

        # Now check that it exists in the database
        try:
            conn = get_new_db_conn()
            cursor = conn.cursor()
            cursor.execute(_sql, username)
            result = cursor.fetchone()
        except Exception as e:
            raise ValueError("SQL error in testlast_create_delete_account #1: %s" % repr(e))

        self.assert_true(result[USERNAME_STR] == username,
                         'Failure creating account in testlast_create_delete_account, result: %s, username: %s'
                         % (result[USERNAME_STR], username))
        self.assert_true(result[EMAIL_STR] == email,
                         'Failure creating account in testlast_create_delete_account, result: %s, email: %s'
                         % (result[EMAIL_STR], email))

        # Now try and delete
        del params[USERNAME_STR]
        params[EVENT_TYPE_STR] = EVENT_DELETE_ACCOUNT_STR
        params[HTTP_METHOD_STR] = DELETE_REQUEST_STR
        self.assert_no_server_error(**params)

        # Now check to make sure it no longer exists in the database
        try:
            conn = get_new_db_conn()
            cursor = conn.cursor()
            cursor.execute(_sql, username)
            result = cursor.fetchone()
        except Exception as e:
            raise ValueError("SQL error in testlast_create_delete_account #2: %s" % repr(e))

        self.assert_true(result is None,
                         'Failure deleting account in testlast_create_delete_account, result: %s' % result)

    def testlast_change_password(self):
        """
        The ability to change one's password
        """
        params = {
            EVENT_TYPE_STR: EVENT_CHANGE_PASSWORD_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            NEW_PASSWORD_HASH_STR: random_valid_password_hash()
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Invalid username
        params[USERNAME_STR] = random_valid_username()
        self.assert_server_error(ERROR_USERNAME_DOES_NOT_EXIST, **params)

        # Invalid email
        del params[USERNAME_STR]
        params[EMAIL_STR] = random_valid_email()
        self.assert_server_error(ERROR_EMAIL_DOES_NOT_EXIST, **params)

        # Password and hash do not match
        del params[EMAIL_STR]
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        params[PASSWORD_HASH_STR] = random_valid_password_hash()
        self.assert_server_error(ERROR_INCORRECT_PASSWORD, **params)

        # Actually works to change password
        params[PASSWORD_HASH_STR] = TEST_ACCOUNT_PASSWORD_HASH
        self.assert_no_server_error(**params)

        # Check to make sure the password hash is correct in the database
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [params[USERNAME_STR]])

        params = {
            EVENT_TYPE_STR: EVENT_LOGIN_STR,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: params[NEW_PASSWORD_HASH_STR]
        }
        self.assert_no_server_error(**params)

        self._change_password_without_knowing()

    def _change_password_without_knowing(self):
        # Try the updating of the password without knowing original password
        params = {
            EVENT_TYPE_STR: EVENT_GET_PASSWORD_CHANGE_CODE_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            HTTP_METHOD_STR: GET_REQUEST_STR,
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Invalid username
        params[USERNAME_STR] = random_valid_username()
        self.assert_server_error(ERROR_USERNAME_DOES_NOT_EXIST, **params)

        # Invalid email
        del params[USERNAME_STR]
        params[EMAIL_STR] = random_valid_email()
        self.assert_server_error(ERROR_EMAIL_DOES_NOT_EXIST, **params)

        # Send a good request for the code
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        self.assert_no_server_error(**params)

        # Make sure there is a valid code in the db
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR),
                       TEST_ACCOUNT_VERIFIED_USERNAME)
        result = cursor.fetchone()
        code = result[PASSWORD_CHANGE_CODE_STR]
        self.assert_true(len(code) == PASSWORD_CHANGE_CODE_SIZE)

        # Update the password knowing only the code
        params = {
            EVENT_TYPE_STR: EVENT_CHANGE_PASSWORD_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            NEW_PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            PASSWORD_CHANGE_CODE_STR: "0" * PASSWORD_CHANGE_CODE_SIZE
        }

        # Wrong code
        self.assert_server_error(ERROR_INVALID_PASSWORD_CHANGE_CODE, **params)

        # Actually change the password
        params[PASSWORD_CHANGE_CODE_STR] = code
        self.assert_no_server_error(**params)

        # Check that the new password hash is the original, and the password change code is gone
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR),
                                TEST_ACCOUNT_VERIFIED_USERNAME)
        result = cursor.fetchone()
        self.assert_true(result[PASSWORD_CHANGE_CODE_STR] == '')

        params = {
            EVENT_TYPE_STR: EVENT_LOGIN_STR,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH
        }
        self.assert_no_server_error(**params)

    def test_create_recipe_fails(self):
        params = {
            EVENT_TYPE_STR: EVENT_CREATE_RECIPE_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            RECIPE_TITLE_STR: "Fake title",
            RECIPE_INGREDIENTS_STR: "Lots of\n ingredients\n in this",
            RECIPE_DESCRIPTION_STR: "Descibr",
            RECIPE_TUTORIAL_STR: "THIS IS A TUTORIAL",
            RECIPE_PRIVATE_STR: False,
            NUT_ALLERGY_STR: False,
            GLUTEN_FREE_STR: False,
            SPICINESS_LEVEL_STR: 0,
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Check if an unverified account does it
        params[USERNAME_STR] = TEST_ACCOUNT_UNVERIFIED_USERNAME
        self.assert_server_error(ERROR_ACCOUNT_UNVERIFIED, **params)

        # Check if password is wrong
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        params[PASSWORD_HASH_STR] = random_valid_password_hash()
        self.assert_server_error(ERROR_INCORRECT_PASSWORD, **params)

    def test_get_recipe_fails(self):
        params = {
            EVENT_TYPE_STR: EVENT_GET_RECIPE_STR,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            RECIPE_ID_STR: -2,
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Getting recipe that is private without info
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Getting recipe that is private and not in user's created recipes
        params.update({
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH
        })
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

    def test_delete_recipe_fails(self):
        params = {
            EVENT_TYPE_STR: EVENT_DELETE_RECIPE_STR,
            HTTP_METHOD_STR: DELETE_REQUEST_STR,
            RECIPE_ID_STR: -2,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH
        }
        self.assert_server_handles_invalid_inputs(**params)

        # Check if an unverified account does it
        params[USERNAME_STR] = TEST_ACCOUNT_UNVERIFIED_USERNAME
        self.assert_server_error(ERROR_ACCOUNT_UNVERIFIED, **params)

        # Check if password is wrong
        params[USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        params[PASSWORD_HASH_STR] = random_valid_password_hash()
        self.assert_server_error(ERROR_INCORRECT_PASSWORD, **params)

        # Check if bad recipe_id
        params[RECIPE_ID_STR] = "aaa"
        params[PASSWORD_HASH_STR] = TEST_ACCOUNT_PASSWORD_HASH
        self.assert_server_error(ERROR_INVALID_RECIPE_ID, **params)

        # Check if the user does not own that thingy
        params[RECIPE_ID_STR] = -2
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

    def testlast_create_get_delete_recipe(self):
        # Create two private recipes
        params = {
            EVENT_TYPE_STR: EVENT_CREATE_RECIPE_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            RECIPE_TITLE_STR: "Fake title",
            RECIPE_INGREDIENTS_STR: "Lots of\n ingredients\n in this",
            RECIPE_DESCRIPTION_STR: "Descibr",
            RECIPE_TUTORIAL_STR: "THIS IS A TUTORIAL",
            RECIPE_PRIVATE_STR: True,
            NUT_ALLERGY_STR: False,
            GLUTEN_FREE_STR: False,
            SPICINESS_LEVEL_STR: 0,
        }
        self.assert_no_server_error(**params)
        params[RECIPE_TITLE_STR] = "FAKE TITLE 2"
        self.assert_no_server_error(**params)

        # Get the most recent recipe
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_NEXT_RECIPE_ID_SQL)
        recipe_id = cursor.fetchone()[NEXT_RECIPE_ID_STR] - 2

        # Make sure it is updated correctly on the user profile
        created_recipes_str = str(recipe_id - 2) + "," + str(recipe_id)
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [TEST_ACCOUNT_VERIFIED_USERNAME])
        self.assert_true(cursor.fetchone()[CREATED_RECIPES_STR] == created_recipes_str)

        # Get public recipe
        params = {
            EVENT_TYPE_STR: EVENT_GET_RECIPE_STR,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            RECIPE_ID_STR: -1,
        }
        self.assert_no_server_error(**params)

        # Get personal private recipe
        params.update({
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
        })
        self.assert_no_server_error(**params)

        # Check recipe string is correct
        created_recipes_str = str(recipe_id - 2) + "," + str(recipe_id)
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [TEST_ACCOUNT_VERIFIED_USERNAME])
        self.assert_true(cursor.fetchone()[CREATED_RECIPES_STR] == created_recipes_str)

        # Delete recipe1
        params = {
            EVENT_TYPE_STR: EVENT_DELETE_RECIPE_STR,
            HTTP_METHOD_STR: DELETE_REQUEST_STR,
            RECIPE_ID_STR: recipe_id,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH
        }
        self.assert_no_server_error(**params)

        conn = get_new_db_conn()
        cursor = conn.cursor()
        created_recipes_str = str(recipe_id - 2)
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [TEST_ACCOUNT_VERIFIED_USERNAME])
        self.assert_true(cursor.fetchone()[CREATED_RECIPES_STR] == created_recipes_str)

        # Delete recipe2
        params[RECIPE_ID_STR] = recipe_id - 2
        self.assert_no_server_error(**params)

        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [TEST_ACCOUNT_VERIFIED_USERNAME])
        self.assert_true(cursor.fetchone()[CREATED_RECIPES_STR] == '')

    def testlast_update_user_profile(self):
        params = {
            EVENT_TYPE_STR: EVENT_UPDATE_USER_PROFILE_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            NEW_USERNAME_STR: '',
            NEW_EMAIL_STR: '',
            NUT_ALLERGY_STR: False,
            GLUTEN_FREE_STR: False,
            SPICINESS_LEVEL_STR: -1,
        }
        self.assert_server_handles_invalid_inputs(**params)

        # Test cant change username/email to ones that already exist
        params[NEW_USERNAME_STR] = TEST_ACCOUNT_UNVERIFIED_USERNAME
        self.assert_server_error(ERROR_USERNAME_ALREADY_EXISTS, **params)
        params[NEW_USERNAME_STR] = TEST_ACCOUNT_VERIFIED_USERNAME
        params[NEW_EMAIL_STR] = TEST_ACCOUNT_UNVERIFIED_EMAIL
        self.assert_server_error(ERROR_USERNAME_ALREADY_EXISTS, **params)

        # Test things actually change
        email, username = random_valid_email(), random_valid_username()
        params[NEW_USERNAME_STR] = username
        params[NEW_EMAIL_STR] = email
        params[NUT_ALLERGY_STR] = True
        params[GLUTEN_FREE_STR] = True
        params[SPICINESS_LEVEL_STR] = 3
        self.assert_no_server_error(**params)

        # Get from the database to check it is all correct
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [username])
        result = cursor.fetchone()
        self.assert_true(result[EMAIL_STR] == email)
        self.assert_true(result[NUT_ALLERGY_STR] == 1)
        self.assert_true(result[GLUTEN_FREE_STR] == 1)
        self.assert_true(result[SPICINESS_LEVEL_STR] == 3)

        # Update it all back to what it should be
        params.update({
            USERNAME_STR: username,
            EMAIL_STR: email,
            NEW_USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            NEW_EMAIL_STR: TEST_ACCOUNT_VERIFIED_EMAIL.lower().replace('.', '').replace('gmailcom', 'gmail.com'),
            NUT_ALLERGY_STR: False,
            GLUTEN_FREE_STR: False,
            SPICINESS_LEVEL_STR: -1
        })
        self.assert_no_server_error(**params)

    def test_update_get_user_favorites(self):
        params = {
            EVENT_TYPE_STR: EVENT_UPDATE_USER_FAVORITES_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            RECIPE_ID_STR: -1
        }
        self.assert_server_handles_invalid_inputs(**params)

        # Add in some temporary recipes
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("INSERT INTO {0} ({1}) VALUES (%s)".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 2)
        cursor.execute("INSERT INTO {0} ({1}) VALUES (%s)".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 3)
        cursor.execute("INSERT INTO {0} ({1}) VALUES (%s)".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 4)
        conn.commit()

        # UPDATE
        # check for recipes that do not exist
        params[RECIPE_ID_STR] = 999999999
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Recipes that are not yet favorited
        params[RECIPE_ID_STR] = -2
        self.assert_server_error(ERROR_RECIPE_NOT_FAVORITED, **params)

        # Add in some favorited recipes
        params[RECIPE_ID_STR] = 2
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = 3
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = 4
        self.assert_no_server_error(**params)

        # Make sure it is as it should be
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, TEST_ACCOUNT_VERIFIED_USERNAME)
        self.assert_true(cursor.fetchone()[FAVORITED_RECIPES_STR] == ','.join(['2', '3', '4']))

        # Delete all of the added ones
        params[EVENT_TYPE_STR] = EVENT_UPDATE_USER_FAVORITES_STR
        params[HTTP_METHOD_STR] = POST_REQUEST_STR
        params[RECIPE_ID_STR] = -2
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = -3
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = -4
        self.assert_no_server_error(**params)

        # Make sure its good now
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, TEST_ACCOUNT_VERIFIED_USERNAME)
        self.assert_true(cursor.fetchone()[FAVORITED_RECIPES_STR] == '')

        # Delete temp recipes
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 2)
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 3)
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), 4)
        conn.commit()

    def test_update_recipe(self):
        params = {
            EVENT_TYPE_STR: EVENT_UPDATE_RECIPE_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            RECIPE_ID_STR: -1
        }
        self.assert_server_handles_invalid_inputs(**params)

        # Can't update a recipe that one does not own
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Can't update a recipe that does not exist
        params[RECIPE_ID_STR] = -3566
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Make a recipe owned by TEST_ACCOUNT_VERIFIED
        recipe_id = -123412
        create_args = [recipe_id, "A", "B", "", "JFDANSIJPNFIUANSFUINJAS", False, False, False, -1,
                       "RECIPE_PICTURE_STR", TEST_ACCOUNT_VERIFIED_USERNAME]
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(CREATE_RECIPE_SQL, create_args)
        conn.commit()

        # Make sure we can edit that one
        new_recipe_args = {
            RECIPE_ID_STR: recipe_id,
            RECIPE_TITLE_STR: "AAAAA",
            RECIPE_DESCRIPTION_STR: "BBBB",
            RECIPE_INGREDIENTS_STR: "CCCCC",
            RECIPE_TUTORIAL_STR: "NEW TUTORIAL",
            RECIPE_PRIVATE_STR: 1,
            NUT_ALLERGY_STR: 1,
            GLUTEN_FREE_STR: 1,
            SPICINESS_LEVEL_STR: 5,
            RECIPE_PICTURE_STR: ""
        }
        params.update(new_recipe_args)

        self.assert_no_server_error(**params)

        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_RECIPE_BY_ID_SQL, [new_recipe_args[RECIPE_ID_STR]])
        result = cursor.fetchone()

        for k, v in new_recipe_args.items():
            self.assert_true(result[k] == v, "Updating, %s != %s" % (result[k], v))

        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR),
                       [new_recipe_args[RECIPE_ID_STR]])
        conn.commit()

    def test_rate_recipe(self):
        params = {
            EVENT_TYPE_STR: EVENT_RATE_RECIPE_STR,
            HTTP_METHOD_STR: POST_REQUEST_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            RECIPE_ID_STR: -1,
            RECIPE_RATING_STR: 4,
        }

        self.assert_server_handles_invalid_inputs(**params)

        # Check rating is bad
        params[RECIPE_RATING_STR] = -1
        self.assert_server_error(ERROR_INVALID_RATING, **params)
        params[RECIPE_RATING_STR] = 6
        self.assert_server_error(ERROR_INVALID_RATING, **params)

        # Check if the recipe doesn't exist
        params[RECIPE_RATING_STR] = 5
        params[RECIPE_ID_STR] = -1824781
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Check if recipe exists, but is private and shouldn't be rated
        params[RECIPE_ID_STR] = -2
        self.assert_server_error(ERROR_UNKNOWN_RECIPE, **params)

        # Add in a few recipes that are able to rate
        conn = get_new_db_conn()
        cursor = conn.cursor()
        recipe1 = [-142, "title1", "", "", "", False, False, False, -1, "", "sda"]
        recipe2 = [-143, "title2", "", "", "", False, False, False, -1, "", "sda2"]
        recipe3 = [-144, "title3", "", "", "", False, False, False, -1, "", "sda3"]
        cursor.execute(CREATE_RECIPE_SQL, recipe1)
        cursor.execute(CREATE_RECIPE_SQL, recipe2)
        cursor.execute(CREATE_RECIPE_SQL, recipe3)
        conn.commit()

        # Rate the recipes
        ratings = [5, 3, 0]
        params[RECIPE_ID_STR] = recipe1[0]
        params[RECIPE_RATING_STR] = ratings[0]
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = recipe2[0]
        params[RECIPE_RATING_STR] = ratings[1]
        self.assert_no_server_error(**params)
        params[RECIPE_ID_STR] = recipe3[0]
        params[RECIPE_RATING_STR] = ratings[2]
        self.assert_no_server_error(**params)

        # Check the thingy is correct
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute(GET_WHERE_USERNAME_LIKE_SQL, [TEST_ACCOUNT_VERIFIED_USERNAME])
        self.assert_equal(cursor.fetchone()[RATED_RECIPES_STR],
                          ','.join([str(recipe1[0]) + ':' + str(ratings[0]),
                                    str(recipe2[0]) + ':' + str(ratings[1]),
                                    str(recipe3[0]) + ':' + str(ratings[2]), ]))

        # Delete the created recipes and reset the rated recipes in username
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), [recipe1[0]])
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), [recipe2[0]])
        cursor.execute("DELETE FROM {0} WHERE {1}=%s".format(RECIPES_TABLE_NAME, RECIPE_ID_STR), [recipe3[0]])
        cursor.execute("UPDATE {0} SET {1}='' WHERE {2} LIKE %s".format(USERS_TABLE_NAME, RATED_RECIPES_STR, USERNAME_STR),
                       [TEST_ACCOUNT_VERIFIED_USERNAME])
        conn.commit()

    def testverylast_search_engine(self):
        params = {
            EVENT_TYPE_STR: EVENT_QUERY_RECIPES_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            QUERY_STR: "",
        }
        self.assert_server_handles_invalid_inputs(**params)

        # Sent empty query should return empty string
        self.assert_equal(self.assert_no_server_error(**params)[QUERY_RESULTS_STR], "")
        params[QUERY_STR] = "  \n\n\t  "
        self.assert_equal(self.assert_no_server_error(**params)[QUERY_RESULTS_STR], "")

        # Do all of the creating of the recipes if we are offline only so it doesn't happen twice
        conn = get_new_db_conn()
        cursor = conn.cursor()
        cursor.execute("SELECT * FROM {0}".format(RECIPES_TABLE_NAME))
        results = cursor.fetchall()

        if len(results) < 5:
            # Create the new recipes if they do not already exist
            recipe_info, pics = read_recipe_info()

            # Make a new user
            params = {
                EVENT_TYPE_STR: EVENT_CREATE_ACCOUNT_STR,
                USERNAME_STR: JUST_WINGIT_USERNAME,
                EMAIL_STR: JUST_WINGIT_EMAIL,
                PASSWORD_HASH_STR: JUST_WINGIT_PASSWORD_HASH,
                HTTP_METHOD_STR: POST_REQUEST_STR,
            }
            request(**params)

            # Verify the user
            conn = get_new_db_conn()
            cursor = conn.cursor()
            cursor.execute(UPDATE_VERIFICATION_CODE_SQL, JUST_WINGIT_USERNAME)
            conn.commit()

            # Create each new recipe for the database
            for recipe, pic in zip(recipe_info, pics):
                file_ext = pic.split('.')[-1]
                # Get a link to the s3 upload and upload it
                params = {
                    EVENT_TYPE_STR: EVENT_GET_S3_URL_STR,
                    USERNAME_STR: JUST_WINGIT_USERNAME,
                    PASSWORD_HASH_STR: JUST_WINGIT_PASSWORD_HASH,
                    HTTP_METHOD_STR: GET_REQUEST_STR,
                    S3_REASON_STR: S3_REASON_UPLOAD_RECIPE_IMAGE,
                    IMAGE_FILE_EXTENSION_STR: file_ext,
                }
                response = request(**params)
                requests.post(response['url'], data=response['fields'], files={'file': (response[RECIPE_PICTURE_ID_STR], open(pic, 'rb'))})

                params = {
                    EVENT_TYPE_STR: EVENT_CREATE_RECIPE_STR,
                    USERNAME_STR: JUST_WINGIT_USERNAME,
                    PASSWORD_HASH_STR: JUST_WINGIT_PASSWORD_HASH,
                    HTTP_METHOD_STR: POST_REQUEST_STR,
                    RECIPE_PICTURE_STR: S3_BUCKET_URL + RECIPE_IMAGES_DIR + "/" + response[RECIPE_PICTURE_ID_STR] + "." + file_ext,
                    RECIPE_TITLE_STR: recipe[0],
                    RECIPE_INGREDIENTS_STR: recipe[1],
                    RECIPE_DESCRIPTION_STR: recipe[2],
                    RECIPE_TUTORIAL_STR: recipe[3],
                    NUT_ALLERGY_STR: recipe[4] == '1',
                    GLUTEN_FREE_STR: recipe[5] == '1',
                    SPICINESS_LEVEL_STR: int(recipe[6]),
                    RECIPE_PRIVATE_STR: False,
                }
                self.assert_no_server_error(**params)

        # Actual testing of search engine
        params = {
            EVENT_TYPE_STR: EVENT_QUERY_RECIPES_STR,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            USERNAME_STR: JUST_WINGIT_USERNAME,
            PASSWORD_HASH_STR: JUST_WINGIT_PASSWORD_HASH,
            QUERY_STR: "Crispy Chicken Wings"
        }

        print(request(**params))

    def test_s3_presigned_url(self):
        """
        Test get_s3_presigned_url for various reasons
        """
        params = {
            EVENT_TYPE_STR: EVENT_GET_S3_URL_STR,
            USERNAME_STR: TEST_ACCOUNT_VERIFIED_USERNAME,
            PASSWORD_HASH_STR: TEST_ACCOUNT_PASSWORD_HASH,
            HTTP_METHOD_STR: GET_REQUEST_STR,
            S3_REASON_STR: S3_REASON_UPLOAD_USER_PROFILE_IMAGE,
            IMAGE_FILE_EXTENSION_STR: 'png',
        }
        self.assert_server_handles_invalid_inputs(**params)

        params.update({
            S3_REASON_STR: S3_REASON_UPLOAD_RECIPE_IMAGE,
        })
        self.assert_server_handles_invalid_inputs(**params)

    def test_misc(self):
        """
        Test miscellaneous things
        """
        # Check that there is an error if there is no event_type in params all http methods,
        #   even if all other params are sent for a function
        for s in IMPLEMENTED_HTTP_METHODS:
            info = {'error_tup': ERROR_NO_EVENT_TYPE, USERNAME_STR: 'a', EMAIL_STR: 'a@b.c',
                    HTTP_METHOD_STR: s}
            self.assert_server_error(**info)

            # Error if an unknown event type is passed for both get and post
            info.update({'error_tup': ERROR_UNKNOWN_EVENT_TYPE, EVENT_TYPE_STR: "uhfn01q7hrn0dfq9und-q"})
            self.assert_server_error(**info)

        _sql = "SELECT * FROM {0} WHERE {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR)

        # Check that the salt works
        try:
            conn = get_new_db_conn()
            cursor = conn.cursor()
            cursor.execute(_sql, TEST_ACCOUNT_VERIFIED_USERNAME)
            result = cursor.fetchone()
            ph1 = result[PASSWORD_HASH_STR][VERIFICATION_CODE_SIZE:]

            cursor.execute(_sql, TEST_ACCOUNT_UNVERIFIED_USERNAME)
            result = cursor.fetchone()
            ph2 = result[PASSWORD_HASH_STR][VERIFICATION_CODE_SIZE:]
        except Exception as e:
            raise ValueError('Exception trying to get database: %s' % repr(e))
        self.assert_true(ph1 != ph2, message="Password hashes match, salt doesn't work!")

    def test_all(self):
        """
        Runs all methods in this test class that start with the string "test_", then all of the methods that start
        with a "testlast_"
        """
        tests_run = 0
        for s in [s for s in dir(self) if s.startswith('test_') and s != 'test_all']:
            print("Testing: %s()..." % s)
            self.__getattribute__(s)()
            tests_run += 1
            print("Test Passed!")

        for s in [s for s in dir(self) if s.startswith('testlast_')]:
            print("Testing: %s()..." % s)
            self.__getattribute__(s)()
            tests_run += 1
            print("Test Passed!")

        for s in [s for s in dir(self) if s.startswith('testverylast_')]:
            print("Testing: %s()..." % s)
            self.__getattribute__(s)()
            tests_run += 1
            print("Test Passed!")
        print("\nPassed all %d tests!\n" % tests_run)

    def single_test(self, method_name):
        print("Testing: %s()..." % method_name)
        self.__getattribute__(method_name)()
        print("Test Passed!")
