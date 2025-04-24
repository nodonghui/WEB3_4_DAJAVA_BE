package com.dajava.backend.domain.image.dto;

import lombok.Builder;

@Builder
public record ImageSaveResponse(
	int widthRange,
	String fileName
) {
}
