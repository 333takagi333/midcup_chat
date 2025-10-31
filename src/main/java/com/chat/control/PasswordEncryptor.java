package com.chat.control;

/**
 * 密码加密模块
 */
public class PasswordEncryptor {

    /**
     * 加密密码
     * @param password 原始密码
     * @return 加密后的密码
     */
    public static String encrypt(String password) {
        if (password == null) {
            return null;
        }
        return password;

    }
}
