package com.dajava.backend.domain.heatmap.validation;

import org.springframework.stereotype.Component;

/**
 * width 값을 widthRange 값과 동일한지 확인하기 위해 정규화
 */
@Component
public class ScreenWidthValidator {

	public int normalizeToWidthRange(int width) {
		return (width / 100) * 100;
	}
}
