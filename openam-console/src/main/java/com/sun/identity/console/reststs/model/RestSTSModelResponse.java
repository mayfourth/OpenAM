package com.sun.identity.console.reststs.model;

/**
 * Model responses, particularly in the context of Rest STS instance creation, require a success flag, and a message.
 * The success flag indicates whether the message will be displayed as in a success, or error, dialog.
 */
public class RestSTSModelResponse {
    private final boolean success;
    private final String message;

    private RestSTSModelResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public static RestSTSModelResponse failure(String failureMessage) {
        return new RestSTSModelResponse(false, failureMessage);
    }

    public static RestSTSModelResponse success(String successMessage) {
        return new RestSTSModelResponse(true, successMessage);
    }

    public static RestSTSModelResponse success() {
        return new RestSTSModelResponse(true, null);
    }

    public boolean isSuccessful() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
