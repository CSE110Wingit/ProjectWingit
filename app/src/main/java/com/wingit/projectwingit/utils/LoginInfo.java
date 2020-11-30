package com.wingit.projectwingit.utils;

import com.wingit.projectwingit.debug.WingitErrors;

import java.io.IOException;
import java.io.File;
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Scanner;

/* Class containing read/write/delete methods relating to the file holding user
 * login information
 */
public class LoginInfo {
    private static final String SAVE_FILE_PATH = "./userSaveFile.txt";
    public static LoginInfo CURRENT_LOGIN;
    public String username, email, passwordHash;

    // Creates login info object holding the username & passwordhash of the user
    public LoginInfo(String username, String email, String passwordHash) {
            this.username = username;
            this.email = email;
            this.passwordHash = passwordHash;
    }

    // Saves all user login information to a file in the app
    private static String saveLoginInfo() {
        try{
            // Overwrites the previous user login information
            deleteLoginInfo();                      

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(SAVE_FILE_PATH)));

            // Create contents of the file
            String content = CURRENT_LOGIN.username + "," + CURRENT_LOGIN.email + "," + CURRENT_LOGIN.passwordHash;

            // Writes and closes content
            bw.write(content);
            bw.flush();
            bw.close();

            return "";
        } catch (IOException e) {
            WingitErrors.error("LoginInfo", "Exception occurred: " + e.getMessage(), WingitErrors.ErrorSeverity.WARNING);
            return "Exception occurred setting login info: " + e.getMessage();
        }
    }

    public static String setCurrentLogin(String username, String email, String passwordHash){
        CURRENT_LOGIN = new LoginInfo(username, email, passwordHash);
        return saveLoginInfo();
    }
    
    // Reads and returns a loginInfo object containing username and password hash of the user
    public static String readLoginInfo() {
        try{
            Scanner userInfoFile = new Scanner(new File(SAVE_FILE_PATH));
            String[] info = userInfoFile.nextLine().split(",");
            CURRENT_LOGIN = new LoginInfo(info[0], info[1], info[2]);
            saveLoginInfo();
            return "";
        } catch (Exception e) {
            WingitErrors.error("LoginInfo", "Exception occurred: " + e.getMessage(), WingitErrors.ErrorSeverity.WARNING);
            return "Exception occurred reading login info: " + e.getMessage();
        }
    }

    // Deletes the file with the user login information
    public static void deleteLoginInfo() {
        File file = new File(SAVE_FILE_PATH);
        if (file.exists()) file.delete();
        CURRENT_LOGIN = null;
    }
}
