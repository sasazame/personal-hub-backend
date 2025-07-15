package com.zametech.personalhub.presentation.validation;

import com.zametech.personalhub.shared.constants.AlarmSound;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class AlarmSoundValidator implements ConstraintValidator<ValidAlarmSound, String> {
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return true; // Let @NotNull handle null validation
        }
        
        try {
            AlarmSound.fromValue(value);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}