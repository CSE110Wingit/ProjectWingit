"""
Automatically builds and deploys the lambda API. Does the following in order:

1. Does prechecks to make sure some basic things were not messed up
2. If --db_rebuild (-D/-d) tag is passed:
    - Delete the current table under USERS_TABLE_NAME
    - Rebuild the table with SQL found in BuildConstants
    - Insert into the table two values:
        * A test user that will remain unverified
        * A test user that is immediately verified
3. Runs offline tests on the lambda_code to make sure there are no initial errors in the python code (could be more
    later on the server)
    - If there are failed tests or errors, they are shown and program execution is exited
4. If there were no errors, generate a zip file of all the code that needs to be copied to the lambda server, and
    places it onto the desktop under the file name "lambda_code.zip"
"""
print("Loading packages...")
from BuildConstants import *
from tests.LambdaTestUtils import random_str
import argparse
import os, shutil
import sys
import pickle

_LAMBDA_CODE_DIR = 'lambda_code'
_LAMBDA_IGNORE = [r'.*dist-info.*']

_REBUILD_DB = True  # So I don't have to keep changing build args
_single_test = None


def build_prechecks():
    """
    Initial checks to make sure I didn't mess up anything with variables that I could see myself overlooking...
    """
    if RETURN_INFO_STR == 'message':
        raise ValueError("Cannot set the RETURN_GOOD_MESSAGE_STR to 'message'")


def rebuild_database(conn):
    """
    WARNING: deletes and rebuilds the database, saving no information
    :param conn: the database connection
    """
    # Delete the table
    cursor = conn.cursor()
    for table in [USERS_TABLE_NAME, RECIPES_TABLE_NAME, "INGREDIENTS", "USER_PREFERENCES"]:
        cursor.execute(DELETE_TABLE_SQL % table)

    # Recreate the tables
    cursor.execute(CREATE_USERS_TABLE_SQL)
    cursor.execute(CREATE_RECIPES_TABLE_SQL)

    # Insert two elements into the users table
    cursor.execute(*make_insert_sql(USERS_TABLE_NAME, **TEST_ACCOUNT_VERIFIED_KWARGS))
    cursor.execute(*make_insert_sql(USERS_TABLE_NAME, **TEST_ACCOUNT_UNVERIFIED_KWARGS))

    # Insert into the recipes table
    recipe1 = [-1, "recipe1", "", "", "", False, "", "dhfashfujhdasjfnajksd", False, False, 0]
    recipe2 = [-2, "recipe2", "", "", "", True, "", "dhfashfujhdasjfnajksd", False, False, 0]
    cursor.execute(CREATE_RECIPE_SQL, recipe1)
    cursor.execute(CREATE_RECIPE_SQL, recipe2)

    conn.commit()
    conn.close()

    # Delete the s3 bucket recipe info
    s3 = boto3.resource('s3')
    bucket = s3.Bucket(S3_BUCKET_NAME)
    bucket.objects.filter(Prefix=RECIPE_IMAGES_DIR + "/").delete()


def build_zip_file():
    """
    Builds a zip file of the directory lambda_code/ and saves it in the base directory as lambda_code.zip. Some things
    need to be done automatically before the zip file is built, however:
        - Everything needed is copied into a temp directory
            - ignores every path that matches a regex in the LAMBDA_IGNORE list
        - As files are being copied, all files in lambda_code/*.py will be skimmed and any imports that start with
            "from lambda_code." or "import lambda_code." will be replaced with "from " and "import " respectively, to
            fix the package references for when it is actually on the server
    """
    # Make a temporary directory to work in
    temp_dir = random_str(30, all_ascii=True)

    # Surround in a try-catch so if something goes wrong, we can at least try to delete the temp_dir
    try:
        # Go through all files
        for root, _, files in os.walk('.\\%s\\' % _LAMBDA_CODE_DIR):

            # Ignore __pychache__
            if root == '.\\%s\\__pycache__' % _LAMBDA_CODE_DIR:
                continue

            # Make the directory
            new_dir = root.replace('.\\%s\\' % _LAMBDA_CODE_DIR, '.\\%s\\' % temp_dir)
            os.mkdir(new_dir)

            # Go through each file in this directory
            for f in files:
                filename = os.path.join(root, f)
                new_filename = os.path.join(new_dir, f)

                # For the base files in _LAMBDA_CODE_DIR, do the whole replacy thingy
                if root == '.\\%s\\' % _LAMBDA_CODE_DIR and filename.endswith('.py'):

                    # Read the new file, change any line that needs changing, then write it back
                    with open(filename, 'r') as infile:
                        with open(new_filename, 'w') as outfile:
                            for line in infile.readlines():
                                line = line.replace('from %s.' % _LAMBDA_CODE_DIR, 'from ')
                                line = line.replace('import %s.' % _LAMBDA_CODE_DIR, 'import ')
                                line = line.replace('./lambda_code/stop_words.pkl', './stop_words.pkl')
                                outfile.write(line)

                # Otherwise copy it over directly
                else:
                    shutil.copy(filename, new_filename)

        # Make the actual zip file
        shutil.make_archive(_LAMBDA_CODE_DIR, 'zip', '.\\%s\\' % temp_dir)

    # Try and delete temp_dir if there was a problem, then show the error
    except Exception:
        exc_inf = str(sys.exc_info()[0])

        try:
            shutil.rmtree(temp_dir)
        except:
            pass

        print("Unexpected error:", exc_inf)
        raise

    # Delete the old temp directory
    shutil.rmtree(temp_dir)


def make_java_constants():
    """
    Writes all of the needed constants to a java file for the app
    """
    if os.path.isfile(JAVA_CONSTANTS_FILE_PATH):
        os.remove(JAVA_CONSTANTS_FILE_PATH)

    with open(JAVA_CONSTANTS_FILE_PATH, 'w') as f:
        f.write(JAVA_CONSTANTS_FILE_DATA)


def count_python_lines():
    files = ['./lambda_code/%s.py' % s for s in ['actions', 'constants', 'errors', 'lambda_function', 's3_utils', 'utils']]
    files += ['./tests/%s.py' % s for s in ['LambdaTestUtils', 'TestLambda']]
    files += ['./%s.py' % s for s in ['BuildConstants', 'buildlambda']]

    total_count = 0
    for f in files:
        with open(f, 'r') as infile:
            for line in infile.readlines():
                line = line.replace("\n", "").replace("\t", "").replace(" ", "")
                total_count += 0 if line == '' else 1
    return total_count


def build_stop_words_pickle():
    words = []
    with open('./stop_words.txt', 'r') as f:
        for line in f.readlines():
            line = line.replace(" ", "").replace("\n", "").replace("\t", "")
            if line != "":
                words.append(line)

    with open("./lambda_code/stop_words.pkl", 'wb') as f:
        pickle.dump(set(words), f)


def delete_testing_accounts():
    conn = get_new_db_conn()
    cursor = conn.cursor()
    cursor.execute("DELETE FROM {0} WHERE {1} LIKE %s OR {1} LIKE %s".format(USERS_TABLE_NAME, USERNAME_STR),
                   [TEST_ACCOUNT_VERIFIED_USERNAME, TEST_ACCOUNT_UNVERIFIED_USERNAME])
    cursor.execute("DELETE FROM {0} WHERE {1} < 0".format(RECIPES_TABLE_NAME, RECIPE_ID_STR))
    conn.commit()


# Main call
if __name__ == "__main__":
    # Do the build prechecks first
    print("Doing prechecks...")
    build_prechecks()
    print("Prechecks passed!")

    # Parse them args
    parser = argparse.ArgumentParser()
    parser.add_argument("-d", "-D", "--db_rebuild",
                        help="WARNING: deletes and rebuilds the USERS database, all data will be deleted.",
                        action="store_true")
    args = parser.parse_args()

    # Delete and rebuild the database if the arg has been passed
    if args.db_rebuild or _REBUILD_DB:
        print("Rebuilding database...")
        from lambda_code.utils import get_new_db_conn

        rebuild_database(get_new_db_conn())
        print("Database rebuilt!")

    # Build the stop words pickle
    print("Building stop words...")
    build_stop_words_pickle()
    print("Stop words built!")

    # Now, do the unit tests offline to make sure everything works
    from tests.LambdaTestUtils import set_request_type_online

    set_request_type_online(False)

    # Check and do only the one test if we need
    if _single_test is not None and _single_test != '':
        print("Running single test...")
        from tests.TestLambda import TestLambda
        TestLambda().single_test(_single_test)
        exit(0)

    print("\nTesting lambda code offline...")
    from tests.TestLambda import TestLambda

    TestLambda().test_all()

    # Tests should have passed if we made it this far, make a zip file of everything needed
    print('Building zip file...')
    build_zip_file()
    print("Zipfile made!")

    # Make the file of constants for java app
    print('Writing java constants file...')
    make_java_constants()
    print('Constants written!')

    # Count the total number of lines of python written
    print("\nTotal number of non-whitespace python lines: %d\n\n" % count_python_lines())

    # Wait for input to test online stuff...
    print("Would you like to test online? (y/n)")
    if input().lower() == 'y':
        print("\nTesting online...")
        set_request_type_online(True)
        TestLambda().test_all()
    else:
        print("Not testing online")

    delete_testing_accounts()
