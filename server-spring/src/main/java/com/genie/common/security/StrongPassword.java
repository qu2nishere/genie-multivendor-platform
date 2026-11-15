package com.genie.common.security;

import jakarta.validation.Constraint;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean Validation cho trường mật khẩu mới — sai → {@code 422 GNE-VAL-4220}, {@code errors[].code = "StrongPassword"}.
 * {@code null} coi là hợp lệ (kết hợp {@code @NotBlank} khi bắt buộc).
 */
@Documented
@Constraint(validatedBy = StrongPassword.Validator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
public @interface StrongPassword {

    String message() default "Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường, chữ số và ký tự đặc biệt.";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

    class Validator implements ConstraintValidator<StrongPassword, String> {
        @Override
        public boolean isValid(String value, ConstraintValidatorContext context) {
            return value == null || PasswordPolicy.isStrong(value);
        }
    }
}
