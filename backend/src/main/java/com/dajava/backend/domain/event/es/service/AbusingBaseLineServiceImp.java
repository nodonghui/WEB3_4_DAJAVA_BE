package com.dajava.backend.domain.event.es.service;

import static com.dajava.backend.global.exception.ErrorCode.*;

import org.springframework.stereotype.Service;

import com.dajava.backend.domain.event.es.entity.AbusingBaseLine;
import com.dajava.backend.domain.event.es.repository.AbusingBaseLineRepository;
import com.dajava.backend.domain.event.exception.AbusingBaseLineException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * AbusingBaseLineService 인터페이스 구현체
 * 어뷰징 기준치 엔티티를 저장, 수정하는 로직을 작성합니다.
 * 스케쥴러와 연계되어 작동합니다.
 * @author NohDongHui
 */
@RequiredArgsConstructor
@Service
@Slf4j
public class AbusingBaseLineServiceImp implements AbusingBaseLineService {

	private final AbusingBaseLineRepository abusingBaseLineRepository;

	/**
	 * pageUrl 기준으로 Baseline 조회
	 */
	@Override
	public AbusingBaseLine getBaselineByPageUrl(String pageUrl) {
		return abusingBaseLineRepository.findByPageUrl(pageUrl)
			.orElseThrow(() -> new AbusingBaseLineException(ABUSING_BASE_LINE_NOT_FOUND));
	}
}
