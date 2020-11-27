package com.wingit.projectwingit.io;

import static com.wingit.projectwingit.utils.WingitLambdaConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;

/**
 * Provides easy access to calling the Lambda API
 */
public class LambdaRequests {

    /**
     * Sends a create_account request to the API
     * @param username the username
     * @param email the email
     * @param passwordHash the password needs to be hashed first before giving to this method
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse createAccount(String username, String email, String passwordHash){
        try{
            String[] params = {
                    USERNAME_STR, username,
                    EMAIL_STR, email,
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_CREATE_ACCOUNT_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending createAccount request: " + e.getMessage());
        }
    }
    
    /**
     * Login to an account
     * @param userOrEmail either the username or the email
     * @param passwordHash the hash of the password
     * @return
     */
    public static LambdaResponse login(String userOrEmail, String passwordHash){
        try{
            if (userOrEmail.contains("@")){
                String[] params = {
                        EMAIL_STR, userOrEmail,
                        PASSWORD_HASH_STR, passwordHash,
                        EVENT_TYPE_STR, EVENT_LOGIN_STR,
                };
                return sendRequest("GET", params);
            }

            String[] params = {
                    USERNAME_STR, userOrEmail,
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_LOGIN_STR,
            };
            return sendRequest("GET", params);

        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending login request: " + e.getMessage());
        }
    }

    /**
	* SR6 Delete User Account
	* @param userOrEmail the username or email
	* @param passwordHash the password hash
	* @return
	*/
    public static LambdaResponse deleteAccount(String userOrEmail, String passwordHash){
        try{
            String[] params = {
                    USERNAME_STR, getUserOrEmail(userOrEmail),
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_USER_DELETE_ACCOUNT_STR,
            };
            return sendRequest("DELETE", params);

        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending delete account request: " + e.getMessage());
        }
    }

    /**
     * SR7 Request change password. DONE BY LOUIS
     * @param usernameOrEmail the username or the email or the user
     * @return
     */
    public static LambdaResponse requestChangePasswordCode(String usernameOrEmail){
        try{
            if (userOrEmail.contains("@")){
                String[] params = {
                        EMAIL_STR, userOrEmail,
                        EVENT_TYPE_STR, EVENT_REQUEST_CHANGE_PASSWORD_STR,
                };
                return sendRequest("GET", params);
            }

            String[] params = {
                    USERNAME_STR, userOrEmail,
                    EVENT_TYPE_STR, EVENT_REQUEST_CHANGE_PASSWORD_STR,
            };
            return sendRequest("GET", params);

        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending change password code request: " + e.getMessage());
        }
    }

    /**
     * SR8 User Account Password Change. DONE BY LOUIS
     * @param username the username, as implied by the name of the parameter
     * @param oldPasswordHash the old password hash, the current one
     * @param newPasswordHash the new password hash, the desired one
     * @return
     */
    public static LambdaResponse userAccountPasswordChange(String username, String oldPasswordHash, String newPasswordHash ){
        try{
            String[] params = {
                    USERNAME_STR, username,
                    OLD_PASSWORD_HASH_STR, oldPasswordHash,
                    NEW_PASSWORD_HASH_STR, newPasswordHash,
                    EVENT_TYPE_STR, EVENT_USER_ACCOUNT_PASSWORD_CHANGE_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending user account password change request: " + e.getMessage());
        }
    }

    /**
     * SR9 edit an account's personal characteristics DONE BY LOUIS
     * @param username the username, as implied by the name of the parameter
     * @param nutAllergy can the user handle nuts or are they allergic to them
     * @param glutenFree Is the user free of gluten
     * @param spicinessLevel how spicy does the user want their wings to be
     * @return
     */
    public static LambdaResponse editPersonalCharacteristics(String username, String nutAllergy, String glutenFree, String spicinessLevel ){
        try{
            String[] params = {
                    USERNAME_STR, username,
                    NUT_ALLERGY_STR, nutAllergy,
                    GLUTEN_FREE_STR, glutenFree,
                    SPICINESS_LEVEL_STR, spicinessLevel,
                    EVENT_TYPE_STR, EVENT_EDIT_PERSONAL_CHARACTERISTICS_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending edit personal characteristics request: " + e.getMessage());
        }
    }

    /**
	* SR11 query recipes
	* @param recipeName the name of the recipe that is being searched for
	* @param nutAllergy determines if the user is allergic to nuts
	* @param glutenFree determines if the user can have recipes with gluten
	* @param spicinessLevel determines the user's spicy preference level
	*/
    public static LambdaResponse queryRecipe(String query, String nutAllergy, String glutenFree, String spicinessLevel ){
        try{
            String[] params = {
                    RECIPE_QUERY_STR, query,
                    NUT_ALLERGY_STR, nutAllergy,
                    GLUTEN_FREE_STR, glutenFree,
                    SPICINESS_LEVEL_STR, spicinessLevel,
                    EVENT_TYPE_STR, EVENT_QUERY_RECIPE_STR,
            };

            return sendRequest("GET", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending search recipe request: " + e.getMessage());
        }
    }
	
    /**
     * S23 Create a recipe DONE BY LOUIS
     * @param recipeAuthor The author of the recipe.
     * @param recipeTitle The title of the recipe.
     * @param recipeIngredients The ingredients of the recipe.
     * @param recipeDescription The description of the recipe. 
     * @param recipeTutorial The tutorial of the recipe.
     * @param recipePrivate The privacy of the recipe. If other people can search for it or not.
     * @return
     */
    public static LambdaResponse createRecipe(String recipeAuthor, String recipeTitle, String recipeIngredients, String recipeDescription, String recipeTutorial, String recipePrivate ){
        try{
            String[] params = {
                    AUTHOR_STR, recipeAuthor,
                    TITLE_STR, recipeTitle,
                    INGREDIENTS_STR, recipeIngredients,
                    DESCRIPTION_STR, recipeDescription,
                    TUTORIAL_STR, recipeTutorial,
                    PRIVACY_STRING, recipePrivate,
                    EVENT_TYPE_STR, EVENT_CREAT_RECIPE_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending create recipe request: " + e.getMessage());
        }
    }

    /**
     * Send the prepared request and get back the response
     */
    private static LambdaResponse sendRequest(String httpMethod, String[] params) throws IOException{
        OkHttpClient client = new OkHttpClient();

        Request request;
        switch (httpMethod){
            case "POST":
                request = new Request.Builder().url(API_URL).post(buildParams(params)).build();
                break;
            case "DELETE":
                request = new Request.Builder().url(API_URL).delete(buildParams(params)).build();
                break;
            case "GET":
                request = new Request.Builder().url(buildGetUrl(params)).build();
                break;
            default:
                return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                        "Error unknown http method: " + httpMethod);
        }

        return new LambdaResponse(client.newCall(request));
    }

    /**
     * Gets the url encoded args string as a byte[]
     */
    private static String buildGetUrl(String[] params) throws UnsupportedEncodingException {
        StringBuilder ret = new StringBuilder();
        for(int i = 0; i < params.length; i+=2)
            ret.append(URLEncoder.encode(params[i], "UTF-8")).append("=").append(URLEncoder.encode(params[i+1], "UTF-8")).append("&");
        return ret.toString();
    }

    /**
     * Builds the request body
     */
    private static MultipartBody buildParams(String[] params){
        MultipartBody.Builder requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM);
        for(int i = 0; i < params.length; i+=2){
            requestBody = requestBody.addFormDataPart(params[i], params[i+1]);
        }
        return requestBody.build();
    }
}
