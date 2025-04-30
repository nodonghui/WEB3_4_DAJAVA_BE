package com.dajava.backend.domain.heatmap.controller;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.dajava.backend.domain.heatmap.dto.HeatmapRequest;
import com.dajava.backend.domain.heatmap.dto.HeatmapResponse;
import com.dajava.backend.domain.heatmap.dto.HeatmapWidthsResponse;
import com.dajava.backend.domain.heatmap.service.HeatmapService;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * HeatmapController
 * 히트맵과 관련된 API 요청 컨트롤러입니다.
 * @author Metronon
 * @since 2025-04-02
 */
@RestController
@RequestMapping("/v1/solution")
@RequiredArgsConstructor
@Slf4j
public class HeatmapController {

	private final HeatmapService heatmapService;

	@Operation(
		summary = "시리얼 번호 기반 전체 히트맵 조회",
		description = "등록된 시리얼 번호와 비밀번호를 이용해 각 이벤트 타입의 히트맵을 조회합니다.")
	@GetMapping("/heatmap/{serialNumber}/{password}")
	@ResponseStatus(HttpStatus.OK)
	public HeatmapResponse getHeatmap(@Valid HeatmapRequest request) {
		final int GRID_SIZE;

		// 그리드 사이즈가 주어지지 않으면 기본값 10 으로 바꿔서 상수 지정
		if (request.gridSize() == null) {
			GRID_SIZE = 10;
		} else {
			GRID_SIZE = request.gridSize();
		}

		return heatmapService.getHeatmap(
			request.serialNumber(),
			request.password(),
			request.type(),
			request.widthRange(),
			GRID_SIZE
		);
	}

	@Operation(
		summary = "히트맵 너비 범위 리스트 조회",
		description = "pageCaptureData에 존재하는 widthRange 리스트를 반환해 히트맵 조건을 선택할 수 있도록 합니다."
	)
	@GetMapping("/heatmap/widths/{serialNumber}/{password}")
	@ResponseStatus(HttpStatus.OK)
	public HeatmapWidthsResponse getHeatmapWidths(
		@PathVariable String serialNumber,
		@PathVariable String password
	) {
		return heatmapService.getWidths(serialNumber, password);
	}
}
