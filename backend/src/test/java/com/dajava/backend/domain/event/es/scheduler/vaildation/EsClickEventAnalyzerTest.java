package com.dajava.backend.domain.event.es.scheduler.vaildation;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.dajava.backend.domain.event.es.entity.PointerClickEventDocument;
import com.dajava.backend.domain.event.es.entity.PointerMoveEventDocument;
import com.dajava.backend.global.component.analyzer.ClickAnalyzerProperties;

/*
 * click docuements 를 분석하는 검증 로직 단위 테스트 입니다.
 *
 * @author NohDongHui
 * @since 2025-04-04
 */
@DisplayName("EsClickEventAnalyzer 테스트")
class EsClickEventAnalyzerTest {

	private EsClickEventAnalyzer analyzer;


	@BeforeEach
	void setUp() {
		ClickAnalyzerProperties props = new ClickAnalyzerProperties();
		props.setTimeThresholdMs(5000);
		props.setPositionThresholdPx(10);
		props.setMinClickCount(3);

		analyzer = new EsClickEventAnalyzer(props);
	}

	private PointerClickEventDocument createEvent(Long time, int clientX, int clientY, String tag) {
		return PointerClickEventDocument.builder()
			.timestamp(time)
			.clientX(clientX)
			.clientY(clientY)
			.sessionId("test-session")
			.pageUrl("/test")
			.browserWidth(1920)
			.memberSerialNumber("user-1")
			.element(tag)
			.isOutlier(false)
			.build();
	}

	private PointerMoveEventDocument createMoveEvent(Long timestamp, int clientX, int clientY) {
		return PointerMoveEventDocument.builder()
			.timestamp(timestamp)
			.clientX(clientX)
			.clientY(clientY)
			.sessionId("test-session")
			.pageUrl("/test")
			.browserWidth(1920)
			.memberSerialNumber("member-123")
			.isOutlier(false)
			.build();
	}

	@Test
	@DisplayName("Rage Click이 감지되어 isOutlier로 마킹되는지 확인")
	void testRageClickDetection() {
		LocalDateTime now = LocalDateTime.now();
		long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		List<PointerClickEventDocument> docs = List.of(
			createEvent(timestamp, 100, 100, "button"),
			createEvent(timestamp + 1, 102, 101, "button"),
			createEvent(timestamp + 2 , 103, 99, "button")
		);

		analyzer.analyze(docs);

		assertThat(docs.stream().filter(PointerClickEventDocument::getIsOutlier).count())
			.isEqualTo(3);
	}

	@Test
	void testNestedHtmlElementScoreCalculation() {
		LocalDateTime now = LocalDateTime.now();
		long timestamp = now.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
		// given: 테스트용 HTML
		String html = "<div class=\"outer\">\n" +
			"  <span class=\"inner\">Hello <b>World</b></span>\n" +
			"</div>";

		PointerClickEventDocument event = createEvent(timestamp, 100, 100, html);


		// when: 점수 계산
		int score = analyzer.calculateContentScore(event);

		// then: 디버그 출력 & 검증
		System.out.println("최종 점수: " + score);
		assertTrue(score > 0, "중첩된 자식 태그의 점수가 포함되어야 함");
	}
}
