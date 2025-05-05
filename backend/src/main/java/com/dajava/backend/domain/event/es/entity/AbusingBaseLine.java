package com.dajava.backend.domain.event.es.entity;


import com.dajava.backend.global.common.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 어뷰징으로 인해 과도한 api요청이 들어오는 걸 방지하기 위해 url당 평균 이벤트 요청 량을 저장하는 엔티티 입니다.
 * 데이터가 들어오며 지속적으로 baseline값이 수정됩니다.
 * @author NohDongHui
 */
@Entity
@Builder
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class AbusingBaseLine extends BaseTimeEntity {

	@Id
	@GeneratedValue
	private long id;

	@Column(nullable = false)
	private String pageUrl;

	@Column(nullable = false)
	private int averageEventsPerHour;

	//averageEventsPerHour의 디폴트 값은 300으로 설정, 데이터를 받으며 값이 계속 수정됨
	public static AbusingBaseLine create(String pageUrl) {
		return AbusingBaseLine.builder()
			.pageUrl(pageUrl)
			.averageEventsPerHour(300)
			.build();
	}
}
