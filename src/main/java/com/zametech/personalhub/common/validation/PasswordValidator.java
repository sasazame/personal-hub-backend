package com.zametech.personalhub.common.validation;

import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class PasswordValidator {
    
    private static final int MIN_LENGTH = 8;
    private static final Pattern UPPERCASE_PATTERN = Pattern.compile("[A-Z]");
    private static final Pattern LOWERCASE_PATTERN = Pattern.compile("[a-z]");
    private static final Pattern DIGIT_PATTERN = Pattern.compile("[0-9]");
    private static final Pattern SPECIAL_CHAR_PATTERN = Pattern.compile("[!@#$%^&*(),.?\":{}|<>]");
    
    public boolean isValid(String password) {
        if (password == null || password.length() < MIN_LENGTH) {
            return false;
        }
        
        // Check for at least one uppercase letter
        if (!UPPERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check for at least one lowercase letter
        if (!LOWERCASE_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check for at least one digit
        if (!DIGIT_PATTERN.matcher(password).find()) {
            return false;
        }
        
        // Check for at least one special character
        if (!SPECIAL_CHAR_PATTERN.matcher(password).find()) {
            return false;
        }
        
        return true;
    }
    
    public String getPasswordRequirements() {
        return "Password must be at least " + MIN_LENGTH + " characters long and contain at least one uppercase letter, " +
               "one lowercase letter, one digit, and one special character (!@#$%^&*(),.?\":{}|<>)";
    }
}