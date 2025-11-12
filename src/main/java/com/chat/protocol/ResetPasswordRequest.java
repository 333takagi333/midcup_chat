package com.chat.protocol;

@SuppressWarnings("unused")
public class ResetPasswordRequest {
    private String type = MessageType.RESET_PASSWORD_REQUEST;
    private String recovery_code;
    private String new_password;

    public ResetPasswordRequest() {
    }

    public ResetPasswordRequest(String recovery_code, String new_password) {
        this.recovery_code = recovery_code;
        this.new_password = new_password;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getRecovery_code() {
        return recovery_code;
    }

    public void setRecovery_code(String recovery_code) {
        this.recovery_code = recovery_code;
    }

    public String getNew_password() {
        return new_password;
    }

    public void setNew_password(String new_password) {
        this.new_password = new_password;
    }
}

