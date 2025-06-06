package com.dajava.backend.domain.register.dto.register;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

@Schema(description = "솔루션 삭제 요청 응답 DTO")
@Builder
public record RegisterDeleteResponse(
) {
	public static RegisterDeleteResponse create() {
		return RegisterDeleteResponse.builder().build();
	}
	/// 현재 전해주는 데이터는 없지만, 변경 가능성을 염두하여 빈 데이터 객체를 생성
}
