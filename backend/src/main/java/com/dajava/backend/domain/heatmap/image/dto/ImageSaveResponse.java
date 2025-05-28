package com.dajava.backend.domain.heatmap.image.dto;

import lombok.Builder;

@Builder
public record ImageSaveResponse(
	int widthRange,
	String fileName
) {
}
