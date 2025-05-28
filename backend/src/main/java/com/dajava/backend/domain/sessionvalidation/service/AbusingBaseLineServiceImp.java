package com.dajava.backend.domain.sessionvalidation.service;

import org.springframework.stereotype.Service;

import com.dajava.backend.domain.sessionvalidation.repository.AbusingBaseLineRepository;
import com.dajava.backend.domain.sessionvalidation.entity.AbusingBaseLine;

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
	 * 만약 url에 대한 baseline이 없다면 새로 생성
	 */
	@Override
	public AbusingBaseLine getBaselineByPageUrl(String pageUrl) {
		return abusingBaseLineRepository.findByPageUrl(pageUrl)
			.orElseGet(() -> {
				AbusingBaseLine newBaseline = AbusingBaseLine.create(pageUrl);
				return abusingBaseLineRepository.save(newBaseline);
			});
	}
}
