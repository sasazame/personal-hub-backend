package com.zametech.personalhub.common.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class StrongPasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private final PasswordValidator passwordValidator;
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
        // No initialization needed
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null) {
            return true; // Let @NotNull handle null validation
        }
        return passwordValidator.isValid(password);
    }
}