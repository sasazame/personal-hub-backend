package com.zametech.personalhub.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = SessionTypeForActionValidator.class)
public @interface ValidSessionTypeForAction {
    String message() default "Session type is required when action is SWITCH_TYPE";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}