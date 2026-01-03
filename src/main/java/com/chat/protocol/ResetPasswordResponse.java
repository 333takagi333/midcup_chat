package com.chat.protocol;

@SuppressWarnings("unused")
public class ResetPasswordResponse {
    private String type = MessageType.RESET_PASSWORD_RESPONSE;
    private boolean success;
    private String message;

    public ResetPasswordResponse() {
    }

    public ResetPasswordResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}

