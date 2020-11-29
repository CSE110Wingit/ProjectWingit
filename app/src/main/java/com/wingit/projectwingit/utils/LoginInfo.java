package com.wingit.projectwingit.utils;

import com.wingit.projectwingit.debug.WingitErrors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File; // File class to make new files
import java.io.FileWriter;
import java.io.BufferedWriter;
import java.util.Scanner; // Used to read files
import java.io.FileNotFoundException; // Handle errors when the file is not found

/* Class containing read/write/delete methods relating to the file holding user
 * login information
 */
public class LoginInfo {
    public static final String SAVE_FILE_PATH = "./userSaveFile.txt"; 
    public String username;
    public String passwordHash;

    // Creates login info object holding the username & passwordhash of the user
    public LoginInfo(String username, String passwordHash) {
            this.username = username;
            this.passwordHash = passwordHash;
    }

    // Saves all user login information to a file in the app
    public void saveLoginInfo(String username, String passwordHash) {
        try{
            // Overwrites the previous user login information
            deleteLoginInfo();                      

            BufferedWriter bw = new BufferedWriter(new FileWriter(new File(SAVE_FILE_PATH)));

            // Create contents of the file
            String content = username + ',' + passwordHash;

            // Writes and closes content
            bw.write(content);
            bw.flush();
            bw.close();

        } catch (IOException e) {
            //IDK what to put here for an error so
            WingitLogging.error("Exception occurred: " + e.getMessage());
        }
    }
    
    // Reads and returns a loginInfo object containing username and password hash of the user
    public LoginInfo readLoginInfo() {
            try{
                    Scanner userInfoFile = Scanner(new File(SAVE_FILE_PATH));
                    String newUsername = userInfoFile.nextLine();
                    String newHash = userInfoFile.nextLine();
                    return new LoginInfo(newUsername, newHash);

            } catch (FileNotFoundException e) {
            WingitLogging.error("Exception occurred: " + e.getMessage());
            }
    }

    // Deletes the file with the user login information
    public void deleteLoginInfo() {
        File file = new File(SAVE_FILE_PATH);
        if (file.exists()) file.delete();        
    }
}
