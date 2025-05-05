package com.dajava.backend.domain.event.es.scheduler.vaildation;

import org.springframework.data.domain.Page;
import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.stereotype.Component;

import com.dajava.backend.domain.event.es.entity.SessionDataDocument;
import com.dajava.backend.domain.event.es.service.SessionDataDocumentService;
import com.dajava.backend.domain.event.exception.PointerEventException;
import com.dajava.backend.global.component.analyzer.BufferSchedulerProperties;
import com.dajava.backend.global.sentry.SentryMonitored;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import io.sentry.SentryLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주기적으로 대량으로 들어온 어뷰징 데이터를 필터링하는 스케줄러 입니다.
 * 검사 후 어뷰징 세션 데이터의 boolean 값을 바꿉니다.
 * 1시간 내 세션 데이터 조회 -> 상위, 하위 10퍼센트 데이터 제외한 데이터로 시간, 수신 데이터량에 대한 baseline 갱신
 * -> 데이터들을 baseline 기준으로 검사해 baseline에 과도하게 넘는 삭제
 * @author NohDongHui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsAbusingCheckScheduler {

	private final SessionDataDocumentService sessionDataDocumentService;

	private final BufferSchedulerProperties bufferSchedulerProperties;


	//@Scheduled(fixedRateString = "#{@bufferSchedulerProperties.abusingCheckMs}")
	@SentryMonitored(level = SentryLevel.FATAL, operation = "abusing_check_scheduler")
	public void runScheduledAbusingCheck() {

		log.info("[AbusingCheckScheduler] 어뷰징 데이터 필터링 스케줄러 시작");

	}

}
