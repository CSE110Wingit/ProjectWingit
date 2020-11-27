package com.wingit.projectwingit.utils;

import com.wingit.projectwingit.debug.WingitErrors;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.io.File; // File class to make new files
import java.io.FileWriter;
import java.io.BufferedWriter;

public class WingitUtils {

    /**
     * Does a SHA256 hash of the given password string and returns a string in hex
     */
    public static String hashPassword(String password){
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return bytesToHex(digest.digest(password.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {}

        return null;
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (int i = 0; i < hash.length; i++) {
            String hex = Integer.toHexString(0xff & hash[i]);
            if(hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    /**
     * Returns true if the user enters an acceptable password. As of now, we only check that the
     * password is at least 8 characters long.
     */
    public boolean checkAcceptablePassword(String password){
        return password.length() >= 8;
    }

    // Saves all user login information to a file in the app
    public void saveLoginInfo(String username, String passwordHash) {
            try{
                    // Create file object
                    File file = new File("userInformation.txt");
                    
                    // Check if the file already exists
                    if(file.exists()) {
                            //Delete already existing file
                            deleteLoginInfo();
                    }                            

                    // Creating buffers needed to write to file
                    FileWriter fw = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(fw);

                    // Create contents of the file
                    String content = username + '\n' + passwordHash;

                    // Writes and closes content
                    bw.write(content);
                    bw.close();

            } catch (IOException ex) {
                    //IDK what to put here for an error so
                    System.out.println("Exception occurred:");
                    ex.printStackTrace();
            }
    }

    // Deletes the file with the user login information
    public void deleteLoginInfo() {
        File file = new File("userInformation.txt");
        if (file.exists()) file.delete();        
    }
}
