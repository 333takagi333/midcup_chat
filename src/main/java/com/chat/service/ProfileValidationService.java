package com.chat.service;

/**
 * 资料验证服务类 - 处理用户资料输入的验证
 */
public class ProfileValidationService {

    /**
     * 验证电话号码格式
     */
    public static boolean validatePhone(String phone) {
        if (phone == null || phone.trim().isEmpty()) {
            return true; // 空值允许
        }
        return phone.matches("\\d{11}");
    }

    /**
     * 验证生日格式
     */
    public static boolean validateBirthday(String birthday) {
        if (birthday == null || birthday.trim().isEmpty()) {
            return true; // 空值允许
        }
        return birthday.matches("\\d{4}-\\d{2}-\\d{2}");
    }

    /**
     * 获取性别代码
     */
    public static Integer getGenderCode(String genderText) {
        if (genderText == null) return 0;
        switch (genderText) {
            case "男": return 1;
            case "女": return 2;
            default: return 0;
        }
    }

    /**
     * 获取性别文本
     */
    public static String getGenderText(Integer genderCode) {
        if (genderCode == null) return "未知";
        switch (genderCode) {
            case 1: return "男";
            case 2: return "女";
            default: return "未知";
        }
    }
}