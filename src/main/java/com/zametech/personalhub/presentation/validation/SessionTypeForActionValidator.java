package com.zametech.personalhub.presentation.validation;

import com.zametech.personalhub.presentation.dto.request.UpdatePomodoroSessionRequest;
import com.zametech.personalhub.shared.constants.SessionAction;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SessionTypeForActionValidator implements ConstraintValidator<ValidSessionTypeForAction, UpdatePomodoroSessionRequest> {
    
    @Override
    public boolean isValid(UpdatePomodoroSessionRequest request, ConstraintValidatorContext context) {
        if (request == null || request.getAction() == null) {
            return true;
        }
        
        if (request.getAction() == SessionAction.SWITCH_TYPE) {
            return request.getSessionType() != null;
        }
        
        return true;
    }
}