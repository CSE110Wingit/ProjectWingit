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
     */
    public static LambdaResponse login(String userOrEmail, String passwordHash){
        try{
            String[] params = {
                    getUserOrEmail(userOrEmail), userOrEmail,
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
     * Gets a recipe in the database with the given id
     * @param recipeId the recipe id
     */
    public static LambdaResponse getRecipe(int recipeId, String userOrEmail, String passwordHash){
        try{
            String[] params = {
                    EVENT_TYPE_STR, EVENT_GET_RECIPE_STR,
                    RECIPE_ID_STR, ""+recipeId,
                    getUserOrEmail(userOrEmail), userOrEmail,
                    PASSWORD_HASH_STR, passwordHash
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
