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
	private long averageEventsPerHour;

	@Column(nullable = false)
	private double standardDeviation;

	@Column(nullable = false)
	private int sampleSize;

	@Column(nullable = false)
	private double m2; // Welford 알고리즘에서 분산 누적합

	public void updateStatistics(long newAverage, double newStandardDeviation, int newSampleSize, double newM2) {
		this.averageEventsPerHour = newAverage;
		this.standardDeviation = newStandardDeviation;
		this.sampleSize = newSampleSize;
		this.m2 = newM2;
	}

	//averageEventsPerHour의 디폴트 값은 300으로 설정, 데이터를 받으며 값이 계속 수정됨
	public static AbusingBaseLine create(String pageUrl) {
		return AbusingBaseLine.builder()
			.pageUrl(pageUrl)
			.averageEventsPerHour(300)
			.standardDeviation(100)
			.sampleSize(0) // 데이터 들어오며 sampleSize = 1 로 시작
			.m2(100 * 100) // 표준편차^2 * sampleSize
			.build();
	}
}
