package com.dajava.backend.domain.event.es.scheduler.vaildation;



import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.event.es.entity.PointerClickEventDocument;
import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.FSMHtmlParser;
import com.dajava.backend.domain.event.es.scheduler.vaildation.htmlparser.HtmlNode;
import com.dajava.backend.domain.event.exception.PointerEventException;
import com.dajava.backend.global.component.analyzer.ClickAnalyzerProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * es 클릭 이벤트를 분석합니다.
 * 이상 데이터인 경우 outlier 값을 true로 바꿉니다.
 * @author NohDongHui
 */
@Slf4j
@Component
public class EsClickEventAnalyzer implements EsAnalyzer<PointerClickEventDocument> {

	//5초 내 클릭 한지 감지
	private final int timeThresholdMs;
	private final int positionThresholdPx;
	private final int minClickCount;

	public EsClickEventAnalyzer(ClickAnalyzerProperties props) {
		this.timeThresholdMs = props.getTimeThresholdMs();
		this.positionThresholdPx = props.getPositionThresholdPx();
		this.minClickCount = props.getMinClickCount();
	}

	// 의심 점수 임계값 - 이 값 이상이면 이상치로 간주
	private static final int SUSPICIOUS_THRESHOLD = 70;

	// 태그 관련 점수 가중치
	private static final Map<String, Integer> TAG_SCORES = Map.ofEntries(
		Map.entry("div", 20),
		Map.entry("span", 20),
		Map.entry("p", 15),
		Map.entry("li", 10),
		Map.entry("img", 5),  // 이미지는 클릭 가능할 수 있음
		Map.entry("label", 5),
		Map.entry("section", 25),
		Map.entry("article", 25),
		Map.entry("body", 40),
		Map.entry("main", 30),
		Map.entry("header", 20),
		Map.entry("footer", 20)
	);

	// 클래스명에 포함될 경우 의심되는 키워드와 점수
	private static final Map<String, Integer> SUSPICIOUS_CLASS_KEYWORDS = Map.ofEntries(
		Map.entry("container", 15),
		Map.entry("wrapper", 15),
		Map.entry("background", 20),
		Map.entry("layout", 15),
		Map.entry("outer", 10),
		Map.entry("inner", 10)
	);

	// 클래스명에 포함될 경우 유의미한(클릭 가능성이 높은) 키워드와 감소 점수
	private static final Map<String, Integer> MEANINGFUL_CLASS_KEYWORDS = Map.ofEntries(
		Map.entry("button", -30),
		Map.entry("btn", -30),
		Map.entry("clickable", -40),
		Map.entry("link", -30),
		Map.entry("nav", -20),
		Map.entry("menu", -15),
		Map.entry("tab", -15),
		Map.entry("card", -10),
		Map.entry("select", -20),
		Map.entry("dropdown", -20)
	);

	// 컨텐츠 특성 관련 점수 조정
	private static final int EMPTY_CONTENT_SCORE = 20;
	private static final int HAS_TEXT_CONTENT_SCORE = -10;
	private static final int HAS_CHILD_ELEMENTS_SCORE = 10;

	// 이벤트 속성 관련 점수 조정
	private static final int NO_EVENT_HANDLER_SCORE = 25;
	private static final int HAS_EVENT_HANDLER_SCORE = -30;

	/**
	 * 클릭 이벤트를 검증해 이상치가 존재하는지 판단하는 구현체
	 * @param eventDocuments 클릭 이벤트 리스트
	 * @return 내부에서 객체 isOutlier값을 변경함 void 반환
	 */
	@Override
	public void analyze(List<PointerClickEventDocument> eventDocuments) {
		//es에서 시계열로 정렬해 가져옴
		log.info("클릭 이벤트 분석 시작 - 이벤트 수: {}, sessionId : {}", eventDocuments.size(),
			eventDocuments.getFirst().getSessionId());
		findRageClicks(eventDocuments);
		findSuspiciousClicks(eventDocuments);
		log.info("무브 이벤트 분석 완료");
	}


	/**
	 * Rage Click으로 의심되는 클릭 그룹들을 탐지합니다.
	 * 이상치인 경우 isOutlier 값을 변경합니다.
	 * @param clickEvents 클릭 이벤트 리스트 (동일 사용자/세션 기준)
	 * @return void
	 */
	public void findRageClicks(List<PointerClickEventDocument> clickEvents) {
		if (clickEvents == null || clickEvents.size() < minClickCount) {
			log.debug("이벤트 수 부족으로 Rage click 분석 생략");
			return;
		}

		PointerClickEventDocument[] window = new PointerClickEventDocument[clickEvents.size()];
		int start = 0;
		int end = 0;

		log.info("Rage Click 분석 시작");

		for (PointerClickEventDocument current : clickEvents) {
			window[end++] = current;

			// 시간 범위를 벗어난 이벤트 제거
			start = removeOldEvents(window, start, end, current);

			// 현재 윈도우 내에서 근접 클릭 수 계산
			int proximityCount = countProximityEvents(window, start, end, current);


			// 근접 클릭이 임계값 이상이면 이상치로 처리
			if (proximityCount >= minClickCount) {
				log.debug("Rage Click 이상치 로그 개수 : {}", (end - start));
				markProximityEventsAsOutliers(window, start, end, current);
				start = end; // 중복 방지
			}
		}

		log.info("Rage Click 이상치 마킹 완료"); // count 로직 추가 시
	}

	/**
	 * 시간 범위를 벗어난 이벤트를 제거하고 새로운 시작 인덱스를 반환합니다.
	 */
	private int removeOldEvents(PointerClickEventDocument[] window, int start, int end,
		PointerClickEventDocument current) {
		while (start < end && isOutOfTimeRange(window[start], current)) {
			start++;
		}
		return start;
	}

	/**
	 * 주어진 이벤트와 근접한 이벤트의 수를 계산합니다.
	 */
	private int countProximityEvents(PointerClickEventDocument[] window, int start, int end,
		PointerClickEventDocument current) {
		int count = 0;
		for (int i = start; i < end; i++) {
			if (isInProximity(window[i], current)) {
				count++;
			}
		}
		return count;
	}

	/**
	 * 근접 이벤트들을 이상치로 표시합니다.
	 */
	private void markProximityEventsAsOutliers(PointerClickEventDocument[] window, int start, int end,
		PointerClickEventDocument current) {
		for (int i = start; i < end; i++) {
			if (isInProximity(window[i], current)) {
				markAsOutlier(window[i]);
			}
		}
	}

	/**
	 * 단일 이벤트를 이상치로 표시합니다.
	 */
	private void markAsOutlier(PointerClickEventDocument event) {
		try {
			event.markAsOutlier();
		} catch (PointerEventException ignored) {
			log.debug("이미 이상치로 처리된 데이터입니다: {}", event.getId());
		}
	}

	private boolean isOutOfTimeRange(PointerClickEventDocument first, PointerClickEventDocument current) {
		Duration duration = Duration.between(first.getTimestamp(), current.getTimestamp());
		return duration.toMillis() > timeThresholdMs;
	}

	private boolean isInProximity(PointerClickEventDocument clickEvent1, PointerClickEventDocument clickEvent2) {
		return (Math.abs(clickEvent1.getClientX() - clickEvent2.getClientX()) <= positionThresholdPx)
			&& (Math.abs(clickEvent1.getClientY() - clickEvent2.getClientY()) <= positionThresholdPx);
	}

	/**
	 * 비정상적인 클릭 이벤트를 이상치값으로 체크합니다.
	 *
	 * @param events PointerClickEventDocument 리스트
	 * @return void
	 */
	/**
	 * 이벤트 목록을 분석하여 이상치로 의심되는 클릭을 식별하고 마킹합니다.
	 *
	 * @param events 분석할 클릭 이벤트 목록
	 * @return void
	 */
	public void findSuspiciousClicks(List<PointerClickEventDocument> events) {
		if (events == null || events.isEmpty()) {
			log.info("이벤트 수 부족으로 suspiciousClick 실행 ");
			return;
		}

		log.info("suspiciousClicks 분석 시작");

		for (PointerClickEventDocument event : events) {
			int suspiciousScore = calculateSuspiciousScore(event);

			if (suspiciousScore >= SUSPICIOUS_THRESHOLD) {
				markAsOutlier(event);
			} else {
				log.debug("정상 클릭으로 판단됨: {} (점수: {})", event.getId(), suspiciousScore);
			}
		}

		log.info("의심 클릭 분석 완료");
	}

	/**
	 * 이벤트의 의심 점수를 여러 요소를 고려하여 계산합니다.
	 *
	 * @param event 분석할 이벤트
	 * @return 의심 점수 (높을수록 의심)
	 */
	private int calculateSuspiciousScore(PointerClickEventDocument event) {
		String elementInfo = event.getElement();

		if (elementInfo == null || elementInfo.isBlank()) {
			log.warn("element 요소 데이터가 null이거나 비어있습니다 eventId : {} ", event.getId());
			return 0; // 정보가 없으면 판단 불가
		}

		String lowerElement = elementInfo.toLowerCase();

		int score = 0;

		// 1. 태그 기반 점수 계산
		score += calculateTagScore(lowerElement);

		// 2. 클래스명 기반 점수 계산
		score += calculateClassScore(lowerElement);

		// 3. 컨텐츠 기반 점수 계산
		score += calculateContentScore(event);

		// 4. 이벤트 핸들러 존재 여부 점수 계산
		score += calculateEventHandlerScore(lowerElement);

		log.debug("의심 점수 계산: {} (태그: {}, 점수: {})", event.getId(), extractTagName(lowerElement), score);

		return score;
	}

	/**
	 * HTML 문자열에서 태그 이름을 추출합니다.
	 */
	private String extractTagName(String elementHtml) {
		// 간단한 태그 이름 추출 로직
		if (elementHtml.startsWith("<") && elementHtml.contains(" ")) {
			return elementHtml.substring(1, elementHtml.indexOf(" "));
		} else if (elementHtml.startsWith("<") && elementHtml.contains(">")) {
			return elementHtml.substring(1, elementHtml.indexOf(">"));
		}
		return elementHtml; // 파싱 실패 시 원본 반환
	}

	/**
	 * 태그 이름 기반 점수를 계산합니다.
	 */
	private int calculateTagScore(String elementHtml) {
		String tagName = extractTagName(elementHtml);

		return TAG_SCORES.getOrDefault(tagName, 0);
	}

	/**
	 * 클래스명 기반 점수를 계산합니다.
	 */
	private int calculateClassScore(String elementHtml) {
		int score = 0;

		// class 속성 추출
		String classAttr = extractAttribute(elementHtml, "class");

		if (classAttr != null) {
			// 의심 클래스 키워드 점수 추가
			for (Map.Entry<String, Integer> entry : SUSPICIOUS_CLASS_KEYWORDS.entrySet()) {
				if (classAttr.contains(entry.getKey())) {
					score += entry.getValue();
				}
			}

			// 의미있는 클래스 키워드 점수 감소
			for (Map.Entry<String, Integer> entry : MEANINGFUL_CLASS_KEYWORDS.entrySet()) {
				if (classAttr.contains(entry.getKey())) {
					score += entry.getValue(); // 이미 음수값이 들어있음
				}
			}
		}

		return score;
	}

	/**
	 * HTML 요소에서 특정 속성 값을 추출합니다.
	 */
	private String extractAttribute(String elementHtml, String attrName) {
		String searchStr = attrName + "=\"";
		int start = elementHtml.indexOf(searchStr);

		if (start >= 0) {
			start += searchStr.length();
			int end = elementHtml.indexOf("\"", start);
			if (end > start) {
				return elementHtml.substring(start, end);
			}
		}

		// 작은따옴표로 둘러싸인 속성 검색
		searchStr = attrName + "='";
		start = elementHtml.indexOf(searchStr);

		if (start >= 0) {
			start += searchStr.length();
			int end = elementHtml.indexOf("'", start);
			if (end > start) {
				return elementHtml.substring(start, end);
			}
		}

		return null;
	}

	/**
	 * 컨텐츠 기반 점수를 계산합니다.
	 */
	public int calculateContentScore(PointerClickEventDocument event) {
		int score = 0;
		String elementHtml = event.getElement();

		boolean hasTextContent = hasTextContent(elementHtml);
		if (!hasTextContent) {
			score += EMPTY_CONTENT_SCORE;
		} else {
			score += HAS_TEXT_CONTENT_SCORE;
		}

		boolean hasChildElements = hasChildElements(elementHtml);
		if (hasChildElements) {
			score += HAS_CHILD_ELEMENTS_SCORE;

			//자식 요소 파싱 및 점수 계산
			try {
				FSMHtmlParser parser = new FSMHtmlParser();
				HtmlNode root = parser.parse(elementHtml);

				// ✅ 자식 노드 전체를 재귀적으로 분석
				for (HtmlNode child : root.children) {
					score += calculateSuspiciousScoreRecursively(child, 0.5); // 첫 자식은 0.5배부터 시작
				}
			} catch (Exception e) {
				log.warn("자식 요소 분석 중 오류 발생: {}", e.getMessage());
			}
		}

		return score;
	}

	/**
	 * 요소에 텍스트 컨텐츠가 있는지 확인합니다.
	 */
	private boolean hasTextContent(String elementHtml) {
		if (elementHtml == null) return false;

		// 태그를 제외한 텍스트 추출 (간단한 구현)
		String textContent = elementHtml.replaceAll("<[^>]*>", "").trim();
		return !textContent.isEmpty();
	}

	/**
	 * 요소에 자식 요소가 있는지 확인합니다.
	 */
	private boolean hasChildElements(String elementHtml) {
		if (elementHtml == null) return false;


		// 여는 태그와 닫는 태그 사이에 다른 태그가 있는지 확인
		int openingTagEnd = elementHtml.indexOf('>');
		int closingTagStart = elementHtml.lastIndexOf("</");

		if (openingTagEnd >= 0 && closingTagStart > openingTagEnd) {
			String innerContent = elementHtml.substring(openingTagEnd + 1, closingTagStart);
			return innerContent.contains("<");
		}

		return false;
	}

	/**
	 * 이벤트 핸들러 존재 여부에 따른 점수를 계산합니다.
	 */
	private int calculateEventHandlerScore(String elementHtml) {
		// 다양한 이벤트 핸들러 검사
		boolean hasEventHandler = hasEventHandler(elementHtml);

		return hasEventHandler ? HAS_EVENT_HANDLER_SCORE : NO_EVENT_HANDLER_SCORE;
	}

	/**
	 * 요소에 이벤트 핸들러가 있는지 확인합니다.
	 */
	private boolean hasEventHandler(String elementHtml) {
		if (elementHtml == null) return false;

		// 일반적인 이벤트 핸들러 속성 체크
		String[] eventHandlers = {
			"onclick", "onmousedown", "onmouseup", "onmouseover", "onmousemove",
			"onmouseout", "ontouchstart", "ontouchend", "ontouchmove", "ontap"
		};

		for (String handler : eventHandlers) {
			if (elementHtml.contains(handler + "=")) {
				return true;
			}
		}

		return false;
	}

	private int calculateSuspiciousScoreRecursively(HtmlNode node, double weight) {
		int score = 0;

		// 현재 노드 점수 계산
		score += (int)(calculateSuspiciousScore(node) * weight);

		// 자식 노드도 재귀적으로 계산 (가중치는 점점 낮아짐)
		for (HtmlNode child : node.children) {
			score += calculateSuspiciousScoreRecursively(child, weight * 0.5); // 0.5배씩 점점 작아짐
		}

		return score;
	}

	private int calculateSuspiciousScore(HtmlNode node) {
		String element = node.toString().toLowerCase(); // 간단히 string 기반으로 파싱

		int score = 0;
		score += calculateTagScore(element);
		score += calculateClassScore(element);
		score += calculateEventHandlerScore(element);

		if (!node.textContent.isBlank()) {
			score += HAS_TEXT_CONTENT_SCORE;
		} else {
			score += EMPTY_CONTENT_SCORE;
		}

		if (!node.children.isEmpty()) {
			score += HAS_CHILD_ELEMENTS_SCORE;
		}

		return score;
	}


}
