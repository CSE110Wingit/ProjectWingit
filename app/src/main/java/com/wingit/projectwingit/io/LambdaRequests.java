package com.wingit.projectwingit.io;

import com.wingit.projectwingit.utils.LoginInfo;

import org.json.JSONObject;

import static com.wingit.projectwingit.utils.WingitLambdaConstants.*;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;

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
     * @param nutAllergy whether or not the user has a nut allergy (should default to False from
     *                   the frontend if user does not specify)
     * @param glutenFree whether or not the user prefers gluten free food (again, should default
     *                   to false in frontend)
     * @param preferredSpiciness preferred spiciness level as integer in range [0, 5] (inclusive).
     *                           If the user does not specify, then set to -1
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse createAccount(String username, String email, String passwordHash,
                                               Boolean nutAllergy, Boolean glutenFree,
                                               int preferredSpiciness){
        try{
            String[] params = {
                    USERNAME_STR, username,
                    EMAIL_STR, email,
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_CREATE_ACCOUNT_STR,
                    NUT_ALLERGY_STR, nutAllergy.toString(),
                    GLUTEN_FREE_STR, glutenFree.toString(),
                    SPICINESS_LEVEL_STR, preferredSpiciness == -1 ? "None" : "" + preferredSpiciness
            };

            LambdaResponse ret = sendRequest("POST", params);
            if (!ret.isError()) {
                String log = LoginInfo.setCurrentLogin(username, email, passwordHash);
                if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
            }

            return ret;
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending createAccount request: " + e.getMessage());
        }
    }

    /**
     * Login to an account
     * @param userOrEmail either the username or the email
     * @param passwordHash the hash of the password
     */
    public static LambdaResponse login(String userOrEmail, String passwordHash){
        try{
            String[] params = {
                    getUserOrEmail(userOrEmail), userOrEmail,
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_LOGIN_STR,
            };
            LambdaResponse ret = sendRequest("GET", params);
            JSONObject json = ret.getResponseJSON();

            if (!ret.isError()) {
                String log = LoginInfo.setCurrentLogin(json.getString(USERNAME_STR), json.getString(EMAIL_STR), passwordHash);
                if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
            }

            return ret;
        }catch (Exception e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending login request: " + e.getMessage());
        }
    }

    /**
     * Login to an account from the save file
     */
    public static LambdaResponse login(){
        String log = LoginInfo.readLoginInfo();
        if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
        return login(LoginInfo.CURRENT_LOGIN.username, LoginInfo.CURRENT_LOGIN.passwordHash);
    }

    /**
     * SR6 Delete User Account
     * @param passwordHash the password hash
     * @return
     */
    public static LambdaResponse deleteAccount(String passwordHash){
        try{
            String[] params = {
                    USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_DELETE_ACCOUNT_STR,
            };
            LoginInfo.deleteLoginInfo();
            return sendRequest("DELETE", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending delete account request: " + e.getMessage());
        }
    }

    /**
     * SR7 Request a change password code to be sent to the currently active user's email.
     * @return
     */
    public static LambdaResponse requestChangePasswordCode(){
        try{
            String[] params = {
                    USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                    EVENT_TYPE_STR, EVENT_GET_PASSWORD_CHANGE_CODE_STR,
            };
            return sendRequest("GET", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending change password code request: " + e.getMessage());
        }
    }

    /**
     * SR8 User Account Password Change.
     * @param oldPasswordHashOrCode either the current password hash, or the password change code
     *                              that was sent to the user via email
     * @return
     */
    public static LambdaResponse changePassword(String oldPasswordHashOrCode, String newPasswordHash){
        try{
            if (oldPasswordHashOrCode.length() == PASSWORD_CHANGE_CODE_SIZE){
                String[] params = {
                        USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                        PASSWORD_CHANGE_CODE_STR, oldPasswordHashOrCode,
                        NEW_PASSWORD_HASH_STR, newPasswordHash,
                        EVENT_TYPE_STR, EVENT_CHANGE_PASSWORD_STR,
                };
                return sendRequest("POST", params);
            } else{
                String[] params = {
                        USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                        PASSWORD_HASH_STR, oldPasswordHashOrCode,
                        NEW_PASSWORD_HASH_STR, newPasswordHash,
                        EVENT_TYPE_STR, EVENT_CHANGE_PASSWORD_STR,
                };
                return sendRequest("POST", params);
            }

        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending user account password change request: " + e.getMessage());
        }
    }

    /**
     * SR9 edit an account's personal characteristics DONE BY LOUIS
     * @param newUsername the new username to change to (set to LoginInfo.CURRENT_LOGIN.username
     *                    to not change anything)
     * @param newEmail the new email to change to (set to LoginInfo.CURRENT_LOGIN.email to not
     *                 change anything)
     * @param nutAllergy can the user handle nuts or are they allergic to them
     * @param glutenFree Is the user free of gluten
     * @param spicinessLevel how spicy does the user want their wings to be
     * @return
     */
    public static LambdaResponse editPersonalCharacteristics(String newUsername, String newEmail, String nutAllergy, String glutenFree, String spicinessLevel){
        try{
            String[] params = {
                    USERNAME_STR, newUsername,
                    EMAIL_STR, newEmail,
                    PASSWORD_HASH_STR, LoginInfo.CURRENT_LOGIN.passwordHash,
                    NUT_ALLERGY_STR, nutAllergy,
                    GLUTEN_FREE_STR, glutenFree,
                    SPICINESS_LEVEL_STR, spicinessLevel,
                    EVENT_TYPE_STR, EVENT_UPDATE_USER_PROFILE_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending edit personal characteristics request: " + e.getMessage());
        }
    }

    /**
     * SR11 query recipes
     * @param query the query string
     * @param nutAllergy if null: ignore nut allergy.
     *                   If True/False: filter results so those recipes that match are more likely
     *                      to be pushed to the top
     * @param glutenFree if null: ignore gluten free.
     *                   If True/False: filter results so those recipes that match are more likely
     *                      to be pushed to the top
     * @param spicinessLevel if -1: ignore spiciness
     *                       If an integer in range [0, 5]: filter results so those recipes that
     *                          match are more likely to be pushed to the top
     */
    public static LambdaResponse queryRecipe(String query, Boolean nutAllergy, Boolean glutenFree, int spicinessLevel){
        try{
            ArrayList<String> params = new ArrayList<>();
            params.add(USERNAME_STR);
            params.add(LoginInfo.CURRENT_LOGIN.username);
            params.add(PASSWORD_HASH_STR);
            params.add(LoginInfo.CURRENT_LOGIN.passwordHash);
            params.add(QUERY_STR);
            params.add(query);
            params.add(EVENT_TYPE_STR);
            params.add(EVENT_QUERY_RECIPES_STR);

            if (nutAllergy != null) {
                params.add(NUT_ALLERGY_STR);
                params.add(nutAllergy.toString());
            }
            if (glutenFree != null) {
                params.add(GLUTEN_FREE_STR);
                params.add(glutenFree.toString());
            }
            if (spicinessLevel != -1) {
                params.add(SPICINESS_LEVEL_STR);
                params.add(""+spicinessLevel);
            }

            return sendRequest("GET", (String[]) params.toArray());
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending search recipe request: " + e.getMessage());
        }
    }

    /**
     * S23 Create a recipe DONE BY LOUIS
     * @param title The title of the recipe.
     * @param ingredients The ingredients of the recipe.
     * @param description The description of the recipe.
     * @param tutorial The tutorial of the recipe.
     * @param isNutAllergy Whether or not there are nuts in the recipe
     * @param isGlutenFree Whether or not the recipe is gluten free
     * @param spicinessLevel an int in range [0, 5] for spiciness level (must have)
     * @param isPrivate The privacy of the recipe. If other people can search for it or not.
     * @return
     */
    public static LambdaResponse createRecipe(String title, String ingredients, String description,
                                              String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                              int spicinessLevel, boolean isPrivate){
        try{
            String[] params = {
                    USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                    PASSWORD_HASH_STR, LoginInfo.CURRENT_LOGIN.passwordHash,
                    RECIPE_TITLE_STR, title,
                    RECIPE_INGREDIENTS_STR, ingredients,
                    RECIPE_DESCRIPTION_STR, description,
                    RECIPE_TUTORIAL_STR, tutorial,
                    RECIPE_PRIVATE_STR, ""+isPrivate,
                    NUT_ALLERGY_STR, ""+isNutAllergy,
                    GLUTEN_FREE_STR, ""+isGlutenFree,
                    SPICINESS_LEVEL_STR, ""+spicinessLevel,
                    EVENT_TYPE_STR, EVENT_CREATE_RECIPE_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending create recipe request: " + e.getMessage());
        }
    }

    /**
     * Gets a recipe in the database with the given id
     * @param recipeId the recipe id
     */
    public static LambdaResponse getRecipe(int recipeId){
        try{
            String[] params = {
                    EVENT_TYPE_STR, EVENT_GET_RECIPE_STR,
                    RECIPE_ID_STR, ""+recipeId,
                    USERNAME_STR, LoginInfo.CURRENT_LOGIN.username,
                    PASSWORD_HASH_STR, LoginInfo.CURRENT_LOGIN.passwordHash
            };
            return sendRequest("GET", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending createAccount request: " + e.getMessage());
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
        StringBuilder ret = new StringBuilder(API_URL + "?");
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

    /**
     * Returns either USERNAME_STR or EMAIL_STR depending on if userOrEmail.contains("@");
     */
    private static String getUserOrEmail(String userOrEmail){
        return userOrEmail.contains("@") ? EMAIL_STR : USERNAME_STR;
    }
}
