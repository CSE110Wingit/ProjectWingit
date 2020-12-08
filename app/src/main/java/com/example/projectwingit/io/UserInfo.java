package com.example.projectwingit.io;

import android.content.Context;

import com.example.projectwingit.debug.WingitErrors;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

import static com.example.projectwingit.utils.WingitLambdaConstants.*;

public class UserInfo {
    public static Context APP_CONTEXT;
    public static UserInfo CURRENT_USER = new UserInfo();

    private static final String SAVE_PATH = "userInfo.txt";
    private static final String SEP = ",";
    private String username, email, passwordHash;
    private boolean nutAllergy, glutenFree;
    private int spicinessLevel;
    private String[] favoritedRecipes, ratedRecipes, createdRecipes;

    /**
     * @return true if the user is logged in and not a guest
     */
    public boolean isLoggedIn(){
        return username == null;
    }

    public String getUsername(){ return username; }
    public String getEmail(){ return email; }
    public boolean getNutAllergy(){ return nutAllergy; }
    public boolean getGlutenFree(){ return glutenFree; }
    public int getSpicinessLevel(){ return spicinessLevel; }
    public String[] getFavoritedRecipes(){ return favoritedRecipes; }
    public String[] getRatedRecipes(){ return ratedRecipes; }
    public String[] getCreatedRecipes(){ return createdRecipes; }

    public boolean correctPassword(String passwordHash){ return this.passwordHash.equals(passwordHash); }

    public boolean recipeIsFavorited(String recipeId){
         return strMember(recipeId, favoritedRecipes);
    }

    public boolean recipeIsRated(String recipeId){
        return strMember(recipeId, ratedRecipes);
    }

    protected void addFavorite(String recipeId){
        this.favoritedRecipes = addStr(recipeId, favoritedRecipes);
    }

    protected void addRated(String recipeId){
        this.ratedRecipes = addStr(recipeId, ratedRecipes);
    }

    protected void addCreated(String recipeId){
        this.createdRecipes = addStr(recipeId, createdRecipes);
    }

    private String[] addStr(String newStr, String[] curr){
        String[] newArr = new String[curr.length + 1];
        System.arraycopy(curr, 0, newArr, 0, curr.length);
        newArr[newArr.length - 1] = newStr;
        return newArr;
    }

    private boolean strMember(String str, String[] curr){
        for (String s : curr) if (s.equals(str)) return true;
        return false;
    }

    protected void removeFavorite(String recipeId){
        String[] newArr = removeStr(recipeId.replaceAll("-", ""), favoritedRecipes);
        if (newArr != null) favoritedRecipes = newArr;
    }

    protected void removeRated(String recipeId){
        String[] newArr = removeStr(recipeId.replaceAll("-", ""), ratedRecipes);
        if (newArr != null) ratedRecipes = newArr;
    }

    protected void removeCreated(String recipeId){
        String[] newArr = removeStr(recipeId.replaceAll("-", ""), createdRecipes);
        if (newArr != null) createdRecipes = newArr;
    }

    protected String epc(String newUsername, String newEmail, boolean nutAllergy, boolean glutenFree, int spicinessLevel){
        this.username = newUsername;
        this.email = newEmail;
        this.nutAllergy = nutAllergy;
        this.glutenFree = glutenFree;
        this.spicinessLevel = spicinessLevel;
        return write(false);
    }

    private String[] removeStr(String rStr, String[] curr){
        if (strMember(rStr, curr)){
            String[] newArr = new String[curr.length + 1];
            int idx = 0;
            for (int i = 0; i < curr.length; i++){
                if (curr[i].equals(rStr)){
                    idx = -1;
                }else{
                    newArr[i + idx] = curr[i];
                }
            }

            return newArr;
        }

        return null;
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
        this.favoritedRecipes = favoritedRecipes != null ? favoritedRecipes : new String[0];
        this.ratedRecipes = ratedRecipes != null ? ratedRecipes : new String[0];
        this.createdRecipes = createdRecipes != null ? createdRecipes : new String[0];
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
        this.favoritedRecipes = this.ratedRecipes = null;
    }

    private String[] toStringArray(JSONArray jsonArray){
        String[] ret = new String[jsonArray.length()];
        try {
            for (int i = 0; i < jsonArray.length(); i++) ret[i] = (String) jsonArray.get(i);
        }catch(Exception e){
            return null;
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
