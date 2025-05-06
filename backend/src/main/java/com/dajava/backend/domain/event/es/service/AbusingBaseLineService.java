package com.dajava.backend.domain.event.es.service;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;

/**
 *
 * AbusingBaseLine 도메인의 서비스 로직을 처리하는 인터페이스
 *
 * @author NohDongHui
 */
public interface AbusingBaseLineService {

	/**
	 * pageUrl 기준으로 Baseline 조회
	 */
	AbusingBaseLine getBaselineByPageUrl(String pageUrl);
}
