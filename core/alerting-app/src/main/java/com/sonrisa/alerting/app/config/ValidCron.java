package com.sonrisa.alerting.app.config;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.scheduling.support.CronExpression;

/** A Spring cron expression with six fields (second minute hour day month weekday). */
@Documented
@Constraint(validatedBy = ValidCron.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidCron {

    String message() default "must be a valid Spring cron expression with six fields, for example \"0 0 * * * *\"";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<ValidCron, String> {

        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            // null is handled by @NotBlank
            return value == null || CronExpression.isValidExpression(value);
        }
    }
}
