package com.wingit.projectwingit.utils;

import android.content.Context;
import android.os.Environment;

import com.wingit.projectwingit.debug.WingitErrors;
import com.wingit.projectwingit.debug.WingitLogging;
import com.wingit.projectwingit.io.LambdaRequests;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Scanner;

/* Class containing read/write/delete methods relating to the file holding user
 * login information
 */
public class LoginInfo {
    private static final String SAVE_PATH = "userInfo.txt";
    private static final String SEP = ",";
    public static LoginInfo CURRENT_LOGIN;
    public static Context APP_CONTEXT;
    public String username, email, passwordHash;

    // Creates login info object holding the username & passwordhash of the user
    public LoginInfo(String username, String email, String passwordHash) {
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
    }

    public static String setCurrentLogin(String username, String email, String passwordHash){
        CURRENT_LOGIN = new LoginInfo(username, email, passwordHash);
        return write(false);
    }
    
    // Reads and returns a loginInfo object containing username and password hash of the user
    public static String readLoginInfo() {
        String read = read();
        if (read.isEmpty()) return "Could not read save info";
        String[] info = read.replaceAll("\n", "").split(SEP);
        CURRENT_LOGIN = new LoginInfo(info[0], info[1], info[2]);
        return "";
    }

    // Deletes the file with the user login information
    public static void deleteLoginInfo() {
        write(true);
        CURRENT_LOGIN = null;
    }

    private static String write(boolean delete) {
        try {
            String data = delete ? "" : CURRENT_LOGIN.username + SEP + CURRENT_LOGIN.email + SEP + CURRENT_LOGIN.passwordHash;
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
