package com.example.a2hauto.auth;

public final class AuthValidator {

    private AuthValidator() {
    }

    public static String normalizePhone(String phone) {
        if (phone == null) {
            return "";
        }
        return phone.replaceAll("\\s+", "").trim();
    }

    public static boolean isValidPhone(String phone) {
        return normalizePhone(phone).matches("^\\d{9,11}$");
    }

    public static boolean isValidFullName(String fullName) {
        return fullName != null && fullName.trim().length() >= 2;
    }

    public static boolean isValidPassword(String password) {
        if (password == null || password.isEmpty()) {
            return false;
        }

        if (password.length() < 8 || password.length() > 32) {
            return false;
        }

        boolean hasUppercase = false;
        boolean hasLowercase = false;
        boolean hasDigit = false;

        for (char character : password.toCharArray()) {
            if (Character.isUpperCase(character)) {
                hasUppercase = true;
            } else if (Character.isLowerCase(character)) {
                hasLowercase = true;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            }
        }

        return hasUppercase && hasLowercase && hasDigit;
    }
}


