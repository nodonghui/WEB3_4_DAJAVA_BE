package com.dajava.backend.domain.heatmap.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * 커스텀 어노테이션 MultipleOf 구현체
 * 주어지는 숫자의 배수인지 확인해 boolean 값으로 Validation 을 진행한다.
 * @since 25-04-27
 * @author Metronon
 */
public class MultipleOfValidator implements ConstraintValidator<MultipleOf, Integer> {
	private int divisor;

	@Override
	public void initialize(MultipleOf constraintAnnotation) {
		this.divisor = constraintAnnotation.value();
	}

	@Override
	public boolean isValid(Integer value, ConstraintValidatorContext context) {
		if (value == null) {
			return true;
		}
		return value % divisor == 0;
	}
}
