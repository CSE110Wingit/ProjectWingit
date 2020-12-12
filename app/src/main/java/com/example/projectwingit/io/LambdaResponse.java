package com.example.projectwingit.io;

import com.example.projectwingit.debug.WingitLogging;
import com.example.projectwingit.utils.WingitLambdaConstants;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import okhttp3.Call;
import okhttp3.Response;

import static com.example.projectwingit.utils.WingitLambdaConstants.*;

/**
 * The returned response from a LambdaRequest. It exists as a separate thread that is returned
 * from LambdaRequests and immediately executed so we get around the whole "having to execute
 * http requests in a separate thread" thing that Android does, and makes it easier for people
 * using this code to have asynchronous calls.
 */
public class LambdaResponse extends Thread{
    private static final long SLEEP_MILLIS = 10;
    private static final long TIMEOUT_MILLIS = 30 * 1000;  // Timeout after 30 seconds of nothing

    private static final String[] _LIST_NAMES = {QUERY_RESULTS_STR, CREATED_RECIPES_STR, RATED_RECIPES_STR, FAVORITED_RECIPES_STR, RECIPE_INGREDIENTS_STR};

    public enum ErrorState{
        NO_ERROR, SERVER_ERROR, CLIENT_ERROR, AWAITING_RESPONSE
    }

    private final Call serverCall;
    private final AtomicBoolean running = new AtomicBoolean();
    private JSONObject json;
    private ErrorState errorState;
    private String errorMessage;

    public LambdaResponse(Call serverCall){
        this.errorState = ErrorState.AWAITING_RESPONSE;
        this.serverCall = serverCall;
        this.running.set(true);
        this.start();
    }

    public LambdaResponse(ErrorState errorState, String errorMessage){
        this.errorState = errorState;
        this.serverCall = null;
        this.running.set(false);
        this.errorMessage = errorMessage;
    }

    /**
     * Actually execute the request to the server and get back the response
     */
    @Override
    public void run() {
        try {
            if (this.serverCall != null) {
                Response response = serverCall.execute();
                interpretResponse(response.body().string());
            }
        } catch (IOException e){
            this.errorState = ErrorState.CLIENT_ERROR;
            this.errorMessage = "IOException while executing the request to the server: " + e.getMessage();
        }catch (JSONException e){
            this.errorState = ErrorState.CLIENT_ERROR;
            this.errorMessage = "Error reading response JSON: " + e.getMessage();
        }

        this.running.set(false);
    }

    private void interpretResponse(String response) throws JSONException{
        if (response.isEmpty()){
            this.errorState = ErrorState.NO_ERROR;
            this.json = new JSONObject("{\"" + RETURN_INFO_STR + "\":\"Successfully uploaded image to s3!\"}");
            return;
        }

        this.json = new JSONObject(response);

        // If there was a server error
        if (!this.json.isNull(WingitLambdaConstants.RETURN_ERROR_CODE_STR) && this.json.isNull("url")){
            WingitLogging.log("Got error");
            this.errorState = ErrorState.SERVER_ERROR;
            this.errorMessage = "Error Code " + this.json.getString(WingitLambdaConstants.RETURN_ERROR_CODE_STR)
                    + ": " + this.json.getString(WingitLambdaConstants.RETURN_ERROR_MESSAGE_STR);
        }
        else if (!this.json.isNull("message")){
            this.errorState = ErrorState.SERVER_ERROR;
            this.errorMessage = "ERROR_UNCAUGHT_SERVER_ERROR: Error was not caught by main try/catch block:\n" + json.getString("message");
        }else{
            this.errorState = ErrorState.NO_ERROR;
            WingitLogging.log("QQQQQQQQ: " + response);
            if (this.json.isNull("url"))
                this.errorMessage = this.json.getString(WingitLambdaConstants.RETURN_INFO_STR);
        }

        fixJSONResponse();
    }

    /**
     * Converts incomming strings to appropriate types if needed
     */
    private void fixJSONResponse(){
        try {
            if (!json.isNull(NUT_ALLERGY_STR)) {
                String str = json.getString(NUT_ALLERGY_STR);
                json.remove(NUT_ALLERGY_STR);
                json.put(NUT_ALLERGY_STR, str.toLowerCase().equals("1"));
            }

            if (!json.isNull(GLUTEN_FREE_STR)) {
                String str = json.getString(GLUTEN_FREE_STR);
                json.remove(GLUTEN_FREE_STR);
                json.put(GLUTEN_FREE_STR, str.toLowerCase().equals("1"));
            }

            if (!json.isNull(SPICINESS_LEVEL_STR)) {
                WingitLogging.log("GOT HERE");
                String str = json.getString(SPICINESS_LEVEL_STR);
                json.remove(SPICINESS_LEVEL_STR);
                json.put(SPICINESS_LEVEL_STR, Integer.parseInt(str));
            }

            for (String s : _LIST_NAMES) {
                if (!json.isNull(s)) {
                    String str = json.getString(s);
                    json.remove(s);
                    if (str.isEmpty()){
                        json.put(s, new JSONArray());
                    }else{
                        json.put(s, new JSONArray(str.split(",")));
                    }
                }
            }
        }catch (JSONException e){
            this.errorState = ErrorState.CLIENT_ERROR;
            this.errorMessage = "Exception fixing JSON response: " + e.getMessage();
        }
    }

    /**
     * @return whether or not this thread is still running and we are awaiting a response from
     * the server
     */
    public boolean isRunning(){ return this.running.get(); }

    /**
     * Waits for the response from the server to get back. If it takes too long, makes this an error
     */
    private void awaitResponse(){
        long totalTime = 0;
        while (this.isRunning()){
            try{
                Thread.sleep(SLEEP_MILLIS);
                totalTime += SLEEP_MILLIS;

                if (totalTime > TIMEOUT_MILLIS){
                    this.errorState = ErrorState.CLIENT_ERROR;
                    this.errorMessage = "Timed out waiting for response...";
                }
            }catch (InterruptedException e){
                this.errorState = ErrorState.CLIENT_ERROR;
                this.errorMessage = "InterruptedException while waiting for response from server.";
            }
        }
    }

    /**
     * Returns true if the error was on the client side and not the server side (IE: You passed
     * some bad parameters to the functions, didn't call them right, etc.)
     */
    public boolean isClientError() {
        this.awaitResponse();
        return errorState == ErrorState.CLIENT_ERROR;
    }

    /**
     * Returns true if the error was on the server side (IE: bad parameters were sent to the server,
     * there was an error with the server, etc.)
     */
    public boolean isServerError() { return errorState == ErrorState.SERVER_ERROR; }

    /**
     * Returns true if there was an error
     */
    public boolean isError() {
        this.awaitResponse();
        return errorState != ErrorState.NO_ERROR;
    }

    /**
     * Gets the response string. Could be either the response from the server, or the error
     * message if the request could not be sent out due to a CLIENT_ERROR
     */
    public String getErrorMessage() {
        this.awaitResponse();
        return this.isError() ? this.errorMessage : "No Error";
    }

    public String getExactErrorMessage() {
        this.awaitResponse();
        return this.errorMessage == null ? "" : this.errorMessage;
    }

    /**
     * Gets the response JSON object
     */
    public JSONObject getResponseJSON(){
        this.awaitResponse();
        return this.json;
    }

    /**
     * Gets the info in the response. Will return the error message if there was one, otherwise
     * the info message.
     */
    public String getResponseInfo(){
        try {
            return this.isError() ? this.getErrorMessage() : this.json.getString(WingitLambdaConstants.RETURN_INFO_STR);
        }catch (JSONException e){
            return "JSON error: " + e.getMessage();
        }
    }

}