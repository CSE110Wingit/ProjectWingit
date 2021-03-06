package com.example.projectwingit.io;

import android.content.Context;
import android.graphics.Bitmap;

import com.example.projectwingit.debug.WingitLogging;

import org.json.JSONObject;

import static com.example.projectwingit.utils.WingitLambdaConstants.*;
import static com.example.projectwingit.utils.WingitUtils.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Iterator;

import okhttp3.*;

/**
 * Provides easy access to calling the Lambda API
 */
public class LambdaRequests extends UserInfo{

    private static final String TEMP_FILE_PATH = "tempimage.png";

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
            LambdaResponse ret = sendRequest("GET", params);
            JSONObject json = ret.getResponseJSON();

            if (!ret.isError()) {
                String log = UserInfo.CURRENT_USER.setCurrentLogin(json, passwordHash);
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
        String log = UserInfo.CURRENT_USER.readLoginInfo();
        if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
        return login(UserInfo.CURRENT_USER.getUsername(), UserInfo.CURRENT_USER.getPasswordHash());
    }

    /**
     * Logout, and delete user save file
     */
    public static LambdaResponse logout(){
        UserInfo.CURRENT_USER.deleteLoginInfo();
        return new LambdaResponse(LambdaResponse.ErrorState.NO_ERROR, "");
    }

    /**
     * SR6 Delete User Account
     * @param passwordHash the password hash
     * @return
     */
    public static LambdaResponse deleteAccount(String passwordHash){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, passwordHash,
                    EVENT_TYPE_STR, EVENT_DELETE_ACCOUNT_STR,
            };
            LambdaResponse response = sendRequest("DELETE", params);
            if (!response.isError())
                UserInfo.CURRENT_USER.deleteLoginInfo();
            return response;
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending delete account request: " + e.getMessage());
        }
    }

    /**
     * SR7 Request a change password code to be sent to the currently active user's email.
     * @return
     */
    public static LambdaResponse requestPasswordChangeCode(String email){
        try{
            String[] params = {
                    EMAIL_STR, email,
                    EVENT_TYPE_STR, EVENT_GET_PASSWORD_CHANGE_CODE_STR,
            };
            return sendRequest("GET", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending password change code request: " + e.getMessage());
        }
    }

    /**
     * SR8 User Account Password Change. This is not written very well (by Justin), but too bad!
     * @param email the email
     * @param oldPasswordHashOrCode either the current password hash, or the password change code
     *                              that was sent to the user via email
     * @param newPasswordHash the new password hash
     * @return
     */
    public static LambdaResponse changePassword(String email, String oldPasswordHashOrCode, String newPasswordHash){
        try{
            if (oldPasswordHashOrCode.length() == PASSWORD_CHANGE_CODE_SIZE){
                String[] params = {
                        EMAIL_STR, email,
                        PASSWORD_CHANGE_CODE_STR, oldPasswordHashOrCode,
                        NEW_PASSWORD_HASH_STR, newPasswordHash,
                        EVENT_TYPE_STR, EVENT_CHANGE_PASSWORD_STR,
                };
                LambdaResponse response = sendRequest("POST", params);
                if (!response.isError()){
                    String log = UserInfo.CURRENT_USER.changePassword(newPasswordHash);
                    if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
                }

                return response;

            } else{
                if (!UserInfo.CURRENT_USER.correctPassword(oldPasswordHashOrCode))
                    return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, "Incorrect Password");
                String[] params = {
                        EMAIL_STR, email,
                        PASSWORD_HASH_STR, oldPasswordHashOrCode,
                        NEW_PASSWORD_HASH_STR, newPasswordHash,
                        EVENT_TYPE_STR, EVENT_CHANGE_PASSWORD_STR,
                };
                LambdaResponse response = sendRequest("POST", params);
                if (!response.isError()){
                    String log = UserInfo.CURRENT_USER.changePassword(newPasswordHash);
                    if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
                }

                return response;
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
    public static LambdaResponse editPersonalCharacteristics(String newUsername, String newEmail,
                                                             boolean nutAllergy, boolean glutenFree,
                                                             int spicinessLevel){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    NEW_USERNAME_STR, newUsername,
                    NEW_EMAIL_STR, newEmail,
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    NUT_ALLERGY_STR, ""+nutAllergy,
                    GLUTEN_FREE_STR, ""+glutenFree,
                    SPICINESS_LEVEL_STR, ""+spicinessLevel,
                    EVENT_TYPE_STR, EVENT_UPDATE_USER_PROFILE_STR,
            };

            LambdaResponse response = sendRequest("POST", params);
            if (!response.isError()){
                String log = UserInfo.CURRENT_USER.epc(newUsername, newEmail, nutAllergy, glutenFree, spicinessLevel);
                if (!log.isEmpty()) return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, log);
            }else{
                WingitLogging.log(response.getErrorMessage());
            }

            return response;
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending edit personal characteristics request: " + e.getMessage());
        }
    }

    /**
     * Sends a rate recipe request to the API
     * @param recipeID - The recipe's ID that the user wants to rate
     * @param recipeStarRating - The rating which the user gave to the recipe
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse rateRecipe(String recipeID, int recipeStarRating){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    RECIPE_ID_STR, recipeID,
                    RECIPE_RATING_STR, ""+recipeStarRating,
                    EVENT_TYPE_STR, EVENT_RATE_RECIPE_STR,
            };

            LambdaResponse response = sendRequest("POST", params);
            WingitLogging.log("HHHHHHHHHH "+recipeStarRating+"  "+response.getResponseInfo());
            if (!response.isError()) UserInfo.CURRENT_USER.addRated(recipeID);

            return response;
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending rateRecipe request: " + e.getMessage());
        }
    }

    /**
     * Adds a recipe to the user's favorited recipe list
     * @param recipeID - The Id of the recipe the user wants to add or remove from his/her favorites
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse favoriteRecipe(String recipeID){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    RECIPE_ID_STR, recipeID,
                    EVENT_TYPE_STR, EVENT_UPDATE_USER_FAVORITES_STR,
            };

            LambdaResponse response = sendRequest("POST", params);
            if (!response.isError()){
                if (recipeID.contains("-")){
                    UserInfo.CURRENT_USER.removeFavorite(recipeID.replaceAll("-", ""));
                }else{
                    UserInfo.CURRENT_USER.addFavorite(recipeID);
                }
            }

            return response;
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending favoriteRecipe request: " + e.getMessage());
        }
    }

    /**
     * Unfavorites a recipe
     * @param recipeID - The Id of the recipe the user wants to add or remove from his/her favorites
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse unfavoriteRecipe(String recipeID){
        return favoriteRecipe("-" + recipeID);
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
     * @param imageURL the url to the image, or null/"" if you don't want an image
     * @return
     */
    private static LambdaResponse _createRecipe(String title, String[] ingredients, String description,
                                              String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                              boolean isVegetarian, int spicinessLevel, boolean isPrivate,
                                                String imageURL){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    RECIPE_TITLE_STR, title,
                    RECIPE_INGREDIENTS_STR, mergeArray(ingredients),
                    RECIPE_DESCRIPTION_STR, description,
                    RECIPE_TUTORIAL_STR, tutorial,
                    RECIPE_PRIVATE_STR, ""+isPrivate,
                    NUT_ALLERGY_STR, ""+isNutAllergy,
                    GLUTEN_FREE_STR, ""+isGlutenFree,
                    VEGETARIAN_STR, ""+isVegetarian,
                    SPICINESS_LEVEL_STR, ""+spicinessLevel,
                    RECIPE_PICTURE_STR, imageURL == null ? "" : imageURL,
                    EVENT_TYPE_STR, EVENT_CREATE_RECIPE_STR,
            };

            LambdaResponse response = sendRequest("POST", params);
            if (!response.isError()){
                UserInfo.CURRENT_USER.addCreated(response.getResponseJSON().getString(RECIPE_ID_STR));
            }

            return response;
        }catch (Exception e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending create recipe request: " + e.getMessage());
        }
    }

    public static LambdaResponse createRecipe(String title, String[] ingredients, String description,
                                              String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                              boolean isVegetarian, int spicinessLevel, boolean isPrivate){
        return _createRecipe(title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                isVegetarian, spicinessLevel, isPrivate, null);
    }

    public static LambdaResponse createRecipe(String title, String[] ingredients, String description,
                                              String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                              boolean isVegetarian, int spicinessLevel, boolean isPrivate,
                                              Bitmap recipeImage){
        if (recipeImage == null){
            return _createRecipe(title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                    isVegetarian, spicinessLevel, isPrivate, null);
        }
        LambdaResponse response = uploadImage(recipeImage);
        if (!response.isError()){
            return _createRecipe(title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                    isVegetarian, spicinessLevel, isPrivate, response.getExactErrorMessage());
        }
        return response;
    }

    /**
     * Gets a recipe in the database with the given id
     * @param recipeId the recipe id
     */
    public static LambdaResponse getRecipe(int recipeId){
        try{
            if (UserInfo.CURRENT_USER.isLoggedIn()) {
                String[] params = {
                        EVENT_TYPE_STR, EVENT_GET_RECIPE_STR,
                        RECIPE_ID_STR, "" + recipeId,
                        USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                        PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                };
                return sendRequest("GET", params);
            }else {
                String[] params = {
                        EVENT_TYPE_STR, EVENT_GET_RECIPE_STR,
                        RECIPE_ID_STR, "" + recipeId,
                };
                return sendRequest("GET", params);
            }
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending createAccount request: " + e.getMessage());
        }
    }

    /**
     * Sends a edit recipe request to the API
     * @param title The title of the recipe.
     * @param ingredients The ingredients of the recipe.
     * @param description The description of the recipe.
     * @param tutorial The tutorial of the recipe.
     * @param isNutAllergy Whether or not there are nuts in the recipe
     * @param isGlutenFree Whether or not the recipe is gluten free
     * @param spicinessLevel an int in range [0, 5] for spiciness level (must have)
     * @param isPrivate The privacy of the recipe. If other people can search for it or not.
     * @param imageURL the url to the image, or null/"" if you don't want an image
     * @return A LambdaResponse of the response
     */
    private static LambdaResponse _editRecipe(String recipeId, String title, String[] ingredients, String description,
                                            String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                            boolean isVegetarian, int spicinessLevel, boolean isPrivate,
                                              String imageURL){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    RECIPE_ID_STR, recipeId,
                    RECIPE_TITLE_STR, title,
                    RECIPE_INGREDIENTS_STR, mergeArray(ingredients),
                    RECIPE_DESCRIPTION_STR, description,
                    RECIPE_TUTORIAL_STR, tutorial,
                    RECIPE_PRIVATE_STR, ""+isPrivate,
                    NUT_ALLERGY_STR, ""+isNutAllergy,
                    GLUTEN_FREE_STR, ""+isGlutenFree,
                    VEGETARIAN_STR, ""+isVegetarian,
                    SPICINESS_LEVEL_STR, ""+spicinessLevel,
                    RECIPE_PICTURE_STR, imageURL == null ? "" : imageURL,
                    EVENT_TYPE_STR, EVENT_UPDATE_RECIPE_STR,
            };

            return sendRequest("POST", params);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending editRecipe request: " + e.getMessage());
        }
    }

    public static LambdaResponse editRecipe(String recipeId, String title, String[] ingredients, String description,
                                              String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                              boolean isVegetarian, int spicinessLevel, boolean isPrivate){
        return _editRecipe(recipeId, title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                isVegetarian, spicinessLevel, isPrivate, null);
    }

    public static LambdaResponse editRecipe(String recipeId, String title, String[] ingredients, String description,
                                            String tutorial, boolean isNutAllergy, boolean isGlutenFree,
                                            boolean isVegetarian, int spicinessLevel, boolean isPrivate,
                                            Bitmap recipeImage){
        if (recipeImage == null){
            return _editRecipe(recipeId, title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                    isVegetarian, spicinessLevel, isPrivate, null);
        }
        LambdaResponse response = uploadImage(recipeImage);
        if (!response.isError()){
            return _editRecipe(recipeId, title, ingredients, description, tutorial, isNutAllergy, isGlutenFree,
                    isVegetarian, spicinessLevel, isPrivate, response.getExactErrorMessage());
        }
        return response;
    }

    /**
     * Sends a delete recipe request to the API
     * @param recipeID - The id of the recipe that we want to delete
     * @return A LambdaResponse of the response
     */
    public static LambdaResponse deleteRecipe(String recipeID){
        try{
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    RECIPE_ID_STR, recipeID,
                    EVENT_TYPE_STR, EVENT_DELETE_RECIPE_STR,
            };

            LambdaResponse response = sendRequest("DELETE", params);
            if (!response.isError()){
                UserInfo.CURRENT_USER.removeCreated(response.getResponseJSON().getString(RECIPE_ID_STR));
            }

            return response;
        }catch (Exception e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending deleteSavedRecipe request: " + e.getMessage());
        }
    }

    /**
     * SR11 query recipes
     * @param query the query string
     * @param containsNuts if null: ignore containsNuts.
     *                   If True/False: filter results so those recipes that match are more likely
     *                      to be pushed to the top
     * @param glutenFree if null: ignore gluten free.
     *                   If True/False: filter results so those recipes that match are more likely
     *                      to be pushed to the top
     * @param spicinessLevel if -1/null: ignore spiciness
     *                       If an integer in range [0, 5]: filter results so those recipes that
     *                          match are more likely to be pushed to the top
     */
    public static LambdaResponse searchRecipes(String query, Boolean containsNuts, Boolean glutenFree,
                                               Boolean vegetarian, Integer spicinessLevel){
        try{
            ArrayList<String> params = new ArrayList<>();
            params.add(QUERY_STR);
            params.add(query);
            params.add(EVENT_TYPE_STR);
            params.add(EVENT_QUERY_RECIPES_STR);

            if (containsNuts != null) {
                params.add(NUT_ALLERGY_STR);
                params.add(containsNuts.toString());
            }
            if (glutenFree != null) {
                params.add(GLUTEN_FREE_STR);
                params.add(glutenFree.toString());
            }
            if (spicinessLevel == null || spicinessLevel < 0) {
                params.add(SPICINESS_LEVEL_STR);
                params.add("-1");
            }else{
                params.add(SPICINESS_LEVEL_STR);
                params.add(spicinessLevel.toString());
            }
            if (vegetarian != null){
                params.add(VEGETARIAN_STR);
                params.add(vegetarian.toString());
            }

            String[] req = new String[params.size()];
            for (int i = 0; i < params.size(); i++) req[i] = params.get(i);

            return sendRequest("GET", req);
        }catch (IOException e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR,
                    "Error sending search recipe request: " + e.getMessage());
        }
    }

    private static LambdaResponse uploadImage(Bitmap image) {
        try {
            String[] params = {
                    USERNAME_STR, UserInfo.CURRENT_USER.getUsername(),
                    PASSWORD_HASH_STR, UserInfo.CURRENT_USER.getPasswordHash(),
                    EVENT_TYPE_STR, EVENT_GET_S3_URL_STR,
                    S3_REASON_STR, S3_REASON_UPLOAD_RECIPE_IMAGE,
                    IMAGE_FILE_EXTENSION_STR, "png"
            };

            LambdaResponse response = sendRequest("GET", params);

            if (!response.isError()) {
                APP_CONTEXT.deleteFile(TEMP_FILE_PATH);
                FileOutputStream fos = APP_CONTEXT.openFileOutput(TEMP_FILE_PATH, Context.MODE_PRIVATE);
                image.compress(Bitmap.CompressFormat.PNG, 100, fos);
                fos.flush();
                fos.close();

                File tmpFile = new File(APP_CONTEXT.getFilesDir(), TEMP_FILE_PATH);
                String filename = response.getResponseJSON().getString("recipe_picture_id") + ".png";

                WingitLogging.log("DDD " + filename);

                String url = response.getResponseJSON().getString("url").replaceAll("\\\\/", "/");
                WingitLogging.log("DDD " + url);
                WingitLogging.log("DDD " + response.getResponseJSON().getString("fields").replaceAll("\\\\/", "/"));

                OkHttpClient client = new OkHttpClient();
                RequestBody formBody = buildImgForm(filename, response.getResponseJSON().getString("fields").replaceAll("\\\\/", "/"), tmpFile);
                Request request = new Request.Builder().url(url).post(formBody).build();
                LambdaResponse resp = new LambdaResponse(client.newCall(request));

                if (!resp.isError()) return new LambdaResponse(LambdaResponse.ErrorState.NO_ERROR, url + RECIPE_IMAGES_DIR + "/" + filename);
                return resp;
            }

            return response;
        }catch(Exception e){
            return new LambdaResponse(LambdaResponse.ErrorState.CLIENT_ERROR, "Error uploading image: " + e.getMessage());
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
            requestBody = requestBody.addFormDataPart(params[i], params[i+1].replaceAll("\r", "\n"));
        }
        return requestBody.build();
    }

    /**
     * Returns either USERNAME_STR or EMAIL_STR depending on if userOrEmail.contains("@");
     */
    private static String getUserOrEmail(String userOrEmail){
        return userOrEmail.contains("@") ? EMAIL_STR : USERNAME_STR;
    }

    /**
     * ','.join(args)
     */
    private static String mergeArray(String[] args){
        StringBuilder ret = new StringBuilder();
        for (String s : args){ ret.append(s).append(","); }
        return ret.substring(0, ret.length() - 1);
    }

    private static MultipartBody buildImgForm(String filename, String fields, File tmpFile) throws Exception{
        JSONObject json = new JSONObject(fields);
        MultipartBody.Builder formBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM);

        Iterator<String> keys = json.keys();
        while(keys.hasNext()){
            String name = keys.next();
            formBody.addFormDataPart(name, json.getString(name));
        }

        formBody.addFormDataPart("file", filename,
                RequestBody.create(MediaType.parse("img/png"), tmpFile));
        return formBody.build();
    }
}