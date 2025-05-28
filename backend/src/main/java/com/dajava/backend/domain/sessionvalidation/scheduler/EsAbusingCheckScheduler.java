package com.dajava.backend.domain.sessionvalidation.scheduler;

import java.util.List;

import org.springframework.data.elasticsearch.UncategorizedElasticsearchException;
import org.springframework.stereotype.Component;

import com.dajava.backend.domain.sessionvalidation.exception.AbusingBaseLineException;
import com.dajava.backend.domain.mouseeventsave.infra.memory.exception.PointerEventException;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.SessionDataDocument;
import com.dajava.backend.domain.mouseeventvalidation.service.SessionDataDocumentService;
import com.dajava.backend.global.component.analyzer.BufferSchedulerProperties;

import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 주기적으로 대량으로 들어온 어뷰징 데이터를 필터링하는 스케줄러 입니다.
 * 검사 후 어뷰징 세션 데이터의 boolean 값을 바꿉니다.
 * 1시간 내 세션 데이터 조회 -> 상위, 하위 10퍼센트 데이터 제외한 데이터로 시간, 수신 데이터량에 대한 baseline 갱신
 * -> 데이터들을 baseline 기준으로 검사해 baseline에 과도하게 넘는 삭제
 * 아니면 검증 스케줄러 돌기전에 직접 호출해 어뷰징 세션 제거 후 검증 로직 동작하게 할 수 있음
 * 각 사이트 성격에 따라 baseline 초기값 설정 다르게 할 필요 있음 -> 경험적으로 파악
 * @author NohDongHui
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EsAbusingCheckScheduler {

	private final SessionDataDocumentService sessionDataDocumentService;
	private final AbusingCheckProcessor abusingCheckProcessor;

	private final BufferSchedulerProperties bufferSchedulerProperties;

	//@Scheduled(fixedRateString = "#{@bufferSchedulerProperties.abusingCheckMs}")
	//@SentryMonitored(level = SentryLevel.FATAL, operation = "abusing_check_scheduler")
	public void runScheduledAbusingCheck() {

		log.info("[AbusingCheckScheduler] 어뷰징 데이터 필터링 스케줄러 시작");

		List<SessionDataDocument> sessionDataDocuments = sessionDataDocumentService.getRecentSessionsInLastHour();

		for (SessionDataDocument session : sessionDataDocuments) {
			try {
				abusingCheckProcessor.processSession(session);
			} catch (PointerEventException e) {
				log.warn("[AbusingCheckScheduler] 세션 검증 실패 (이미 검증된 세션일 수 있음): {}, {}",
					session.getSessionId(), e.getMessage());
			} catch (ElasticsearchException | UncategorizedElasticsearchException e) {
				log.warn("[AbusingCheckScheduler] 세션 ID: {} - Elasticsearch 쿼리 실패", session.getSessionId());
				log.warn("[AbusingCheckScheduler] Exception Message: {}", e.getMessage());
				log.warn("[AbusingCheckScheduler] Full Stack Trace", e);

			} catch (AbusingBaseLineException e) {
				log.error("[AbusingCheckScheduler] 이미 해당 url baseline의 priorAverage이 설정되어 있습니다.  -  url : {}", session.getPageUrl());
			} catch (Exception e) {
				log.error("[AbusingCheckScheduler] 예상치 못한 에러 발생 - 세션 ID: {}", session.getSessionId(), e);
			}
		}

		log.info("[AbusingCheckScheduler] 어뷰징 데이터 필터링 스케줄러 종료");

	}

}
