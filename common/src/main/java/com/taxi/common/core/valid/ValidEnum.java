package com.taxi.common.core.valid;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = EnumValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidEnum {

    Class<? extends Enum<?>> enumClass();
    String message() default "잘못된 입력입니다.";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};

}
