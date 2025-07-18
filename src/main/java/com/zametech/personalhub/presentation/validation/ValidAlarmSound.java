package com.zametech.personalhub.presentation.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AlarmSoundValidator.class)
public @interface ValidAlarmSound {
    String message() default "Invalid alarm sound";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}