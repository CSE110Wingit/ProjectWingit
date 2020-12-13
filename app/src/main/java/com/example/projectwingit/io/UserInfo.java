package com.example.projectwingit.io;

import android.content.Context;

import com.example.projectwingit.debug.WingitErrors;
import com.example.projectwingit.debug.WingitLogging;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;

import static com.example.projectwingit.utils.WingitLambdaConstants.*;

public class UserInfo {
    public static Context APP_CONTEXT;
    public static UserInfo CURRENT_USER = new UserInfo();

    private static final String SAVE_PATH = "userInfo.txt";
    private static final String SEP = ",";
    private String username, email, passwordHash;
    private boolean nutAllergy, glutenFree;
    private int spicinessLevel;
    private ArrayList<String> favoritedRecipes = new ArrayList<>(),
            ratedRecipes = new ArrayList<>(),
            createdRecipes = new ArrayList<>();

    /**
     * @return true if the user is logged in and not a guest
     */
    public boolean isLoggedIn(){
        return username != null;
    }

    public String getUsername(){ return username; }
    public String getEmail(){ return email; }
    public boolean getNutAllergy(){ return nutAllergy; }
    public boolean getGlutenFree(){ return glutenFree; }
    public int getSpicinessLevel(){ return spicinessLevel; }
    public String[] getFavoritedRecipes(){ return arrToStr(favoritedRecipes); }
    public String[] getRatedRecipes(){ return arrToStr(ratedRecipes); }
    public String[] getCreatedRecipes(){ return arrToStr(createdRecipes); }

    public boolean correctPassword(String passwordHash){ return this.passwordHash.equals(passwordHash); }

    public boolean recipeIsFavorited(String recipeId){ return favoritedRecipes.contains(recipeId); }
    public boolean recipeIsRated(String recipeId){
        return ratedRecipes.contains(recipeId);
    }
    public boolean recipeIsCreated(String recipeId){
        return createdRecipes.contains(recipeId);
    }

    protected void addFavorite(String recipeId){
        WingitLogging.log("DDDDD: Adding favorite recipes: " + recipeId);
        if (!this.favoritedRecipes.contains(recipeId)){
            WingitLogging.log("DDDDD: definitely adding: " + recipeId);
            this.favoritedRecipes.add(recipeId);
        }
    }
    protected void addRated(String recipeId){ if (!this.ratedRecipes.contains(recipeId)) this.ratedRecipes.add(recipeId); }
    protected void addCreated(String recipeId){ if (!this.createdRecipes.contains(recipeId)) this.createdRecipes.add(recipeId); }

    protected void removeFavorite(String recipeId){
        WingitLogging.log("DDDDD: Removing recipe: " + recipeId);
        int size = this.favoritedRecipes.size();
        this.favoritedRecipes.remove(recipeId);
        if (this.favoritedRecipes.size() < size)
            WingitLogging.log("DDDDDD: definitely removed recipe: " + recipeId);
    }
    protected void removeRated(String recipeId){ this.ratedRecipes.remove(recipeId); }
    protected void removeCreated(String recipeId){ this.createdRecipes.remove(recipeId); }

    protected String epc(String newUsername, String newEmail, boolean nutAllergy, boolean glutenFree, int spicinessLevel){
        this.username = newUsername;
        this.email = newEmail;
        this.nutAllergy = nutAllergy;
        this.glutenFree = glutenFree;
        this.spicinessLevel = spicinessLevel;
        return write(false);
    }

    protected String changePassword(String passwordHash){
        this.passwordHash = passwordHash;
        return write(false);
    }

    protected String getPasswordHash(){ return passwordHash; }

    protected String setCurrentLogin(String username, String email, String passwordHash, boolean nutAllergy,
                                  boolean glutenFree, int spicinessLevel, String[] favoritedRecipes,
                                  String[] ratedRecipes, String[] createdRecipes){
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.nutAllergy = nutAllergy;
        this.glutenFree = glutenFree;
        this.spicinessLevel = spicinessLevel;
        this.favoritedRecipes = favoritedRecipes != null ? toStringArray(favoritedRecipes) : new ArrayList<>();
        this.ratedRecipes = ratedRecipes != null ? toStringArray(ratedRecipes) : new ArrayList<>();
        this.createdRecipes = createdRecipes != null ? toStringArray(createdRecipes) : new ArrayList<>();
        return write(false);
    }

    protected String setCurrentLogin(JSONObject json, String passwordHash){
        try {
            this.username = json.getString(USERNAME_STR);
            this.email = json.getString(EMAIL_STR);
            this.passwordHash = passwordHash;
            this.nutAllergy = json.getBoolean(NUT_ALLERGY_STR);
            this.glutenFree = json.getBoolean(GLUTEN_FREE_STR);
            this.spicinessLevel = json.getInt(SPICINESS_LEVEL_STR);
            this.favoritedRecipes = toStringArray(json.getJSONArray(FAVORITED_RECIPES_STR));
            this.ratedRecipes = toStringArray(json.getJSONArray(RATED_RECIPES_STR));
            this.createdRecipes = toStringArray(json.getJSONArray(CREATED_RECIPES_STR));
            if (this.ratedRecipes == null || this.favoritedRecipes == null || this.createdRecipes == null){
                throw new Exception("There was a problem...");
            }
            return "";
        }catch (Exception e){
            return e.getMessage();
        }
    }

    // Reads and returns a loginInfo object containing username and password hash of the user
    protected String readLoginInfo() {
        String read = read();
        if (read.isEmpty()) return "Could not read save info";
        String[] info = read.replaceAll("\n", "").split(SEP);
        this.username = info[0];
        this.email = info[1];
        this.passwordHash = info[2];
        return "";
    }

    // Deletes the file with the user login information
    protected void deleteLoginInfo() {
        write(true);
        this.username = this.email = this.passwordHash = null;
        this.favoritedRecipes.clear();
        this.ratedRecipes.clear();
        this.createdRecipes.clear();
    }

    private ArrayList<String> toStringArray(JSONArray jsonArray){
        ArrayList<String> ret = new ArrayList<>();
        try {
            for (int i = 0; i < jsonArray.length(); i++) ret.add((String) jsonArray.get(i));
        }catch(Exception e){
            return null;
        }
        return ret;
    }

    private ArrayList<String> toStringArray(String[] strArr){
        ArrayList<String> ret = new ArrayList<>();
        try {
            for (int i = 0; i < strArr.length; i++) ret.add(strArr[i]);
        }catch(Exception e){
            return null;
        }
        return ret;
    }

    private String[] arrToStr(ArrayList<String> arr){
        String[] ret = new String[arr.size()];
        for (int i = 0; i < arr.size(); i++){
            ret[i] = arr.get(i);
        }
        return ret;
    }

    private String write(boolean delete) {
        try {
            String data = delete ? "" : username + SEP + email + SEP + passwordHash;
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(APP_CONTEXT.openFileOutput(SAVE_PATH, Context.MODE_PRIVATE));
            outputStreamWriter.write(data);
            outputStreamWriter.close();
            return "";
        }
        catch (IOException e) {
            WingitErrors.error("LoginInfo", "Exception occurred: " + e.getMessage(), WingitErrors.ErrorSeverity.WARNING);
            return "Exception occurred setting login info: " + e.getMessage();
        }
    }

    private static String read() {
        try {
            InputStream inputStream = APP_CONTEXT.openFileInput(SAVE_PATH);

            if ( inputStream != null ) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";
                StringBuilder stringBuilder = new StringBuilder();

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    stringBuilder.append("\n").append(receiveString);
                }

                inputStream.close();
                return stringBuilder.toString();
            }
        }
        catch (Exception e) {
            WingitErrors.error("LoginInfo", "Exception occurred: " + e.getMessage(), WingitErrors.ErrorSeverity.WARNING);
        }

        return "";
    }
}
