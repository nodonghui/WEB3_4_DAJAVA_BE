package com.dajava.backend.domain.event.es.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;

/**
 *  어뷰징 분석을 위한 세션당 존재하는 baseline을 저장하는 리포지드
 *  @author NohDongHui
 */
public interface AbusingBaseLineRepository extends JpaRepository<AbusingBaseLine, Long> {

	/**
	 * pageUrl 기준으로 Baseline 조회
	 */
	Optional<AbusingBaseLine> findByPageUrl(String pageUrl);
}
