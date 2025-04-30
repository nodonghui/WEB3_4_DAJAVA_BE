package com.dajava.backend.domain.heatmap.validation;

import static java.lang.annotation.ElementType.*;
import static java.lang.annotation.RetentionPolicy.*;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

/**
 * 커스텀 어노테이션 MultipleOf 인터페이스
 * @since 25-04-27
 * @author Metronon
 */
@Documented
@Constraint(validatedBy = MultipleOfValidator.class)
@Target({ FIELD, METHOD, PARAMETER})
@Retention(RUNTIME)
public @interface MultipleOf {
	String message() default "값은 {value}의 배수여야 합니다.";
	Class<?>[] groups() default {};
	Class<? extends Payload>[] payload() default {};
	int value();
}
