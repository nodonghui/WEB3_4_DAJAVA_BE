package com.dajava.backend.domain.heatmap.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record HeatmapRequest(
	@NotBlank(message = "일련번호는 필수 값입니다.") String serialNumber,
	@NotBlank(message = "비밀번호는 필수 값입니다.") String password,
	@NotBlank(message = "타입은 필수 값입니다.") String type,
	@Min(value = 5, message = "그리드 사이즈는 5 미만 값을 지원하지 않습니다.")
	@Max(value = 50, message = "그리드 사이즈는 50 초과 값을 지원하지 않습니다.")
	Integer gridSize,
	// TODO : 100 단위로 widthRange 받도록 Validation
	@Min(value = 800, message = "히트맵 너비값의 최소 값은 800 입니다.")
	int widthRange
) {
}
