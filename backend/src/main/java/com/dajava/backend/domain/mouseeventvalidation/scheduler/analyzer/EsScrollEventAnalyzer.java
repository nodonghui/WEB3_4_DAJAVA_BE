package com.dajava.backend.domain.mouseeventvalidation.scheduler.analyzer;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.mouseeventsave.infra.memory.exception.PointerEventException;
import com.dajava.backend.domain.mouseeventsave.infra.memory.entity.PointerScrollEventDocument;
import com.dajava.backend.global.component.analyzer.ScrollAnalyzerProperties;

import lombok.extern.slf4j.Slf4j;

/**
 * 스크롤 이벤트를 분석합니다.
 * 이상 데이터를 반환합니다.
 * @author NohDongHui
 */
@Slf4j
@Component
public class EsScrollEventAnalyzer implements EsAnalyzer<PointerScrollEventDocument> {



	private final long timeWindowMs;
	private final int minScrollDelta;
	private final int minEventCount;
	private final int rageThresholdPerWindow;
	private final int minDirectionChanges;
	private final double contentConsumedThreshold;

	public EsScrollEventAnalyzer(ScrollAnalyzerProperties props) {
		this.timeWindowMs = props.getTimeWindowMs();
		this.minScrollDelta = props.getMinScrollDelta();
		this.minEventCount = props.getMinEventCount();
		this.rageThresholdPerWindow = props.getRageThresholdPerWindow();
		this.minDirectionChanges = props.getMinDirectionChanges();
		this.contentConsumedThreshold = props.getContentConsumedThreshold();
	}

	@Override
	public void analyze(List<PointerScrollEventDocument> documents) {
		//es 에서 조회시 정렬
		log.info("스크롤 이벤트 분석 시작 - 이벤트 수: {}", documents.size());

		findRageScrollBursts(documents);
		findBackAndForthScrollOutliers(documents);
		findTopRepeatScrollOutliers(documents);

		log.info("스크롤 이벤트 분석 완료");
	}

	/**
	 * rage scroll을 감지합니다.
	 * 짫은 시간 내 여러번 rage scroll이 있는 경우
	 * rage scroll에 해당하는 데이터를 outlier로 마킹합니다.
	 * @param events PointerClickEvent 리스트
	 * @return void
	 */
	public void findRageScrollBursts(List<PointerScrollEventDocument> events) {
		if (events == null || events.size() < minEventCount) {
			log.debug("이벤트 수 부족으로 Rage Scroll 분석 생략");
			return;
		}

		log.info("Rage Scroll 분석 시작");

		PointerScrollEventDocument[] window = new PointerScrollEventDocument[events.size()];
		int start = 0;
		int end = 0;


		Set<PointerScrollEventDocument> detectedOutliers = new HashSet<>();

		for (PointerScrollEventDocument current : events) {
			window[end++] = current;

			// 오래된 이벤트 제거
			start = removeOldEvents(window, start, end, current);

			// 현재 윈도우에서 이상 이벤트 분석
			List<PointerScrollEventDocument> windowList = extractWindowList(window, start, end);

			int rageWithinWindow = processCurrentWindow(windowList, detectedOutliers);

			if (rageWithinWindow >= rageThresholdPerWindow) {
				log.info("Rage Scroll 이상치 감지 - 이벤트 수: {}, 윈도우 범위: {}~{}", windowList.size(), start, end);
				start = end; // 윈도우 초기화 (중복 감지 방지)
			}
		}

		// 이상치로 판별된 이벤트에 markAsOutlier 호출
		markOutliers(detectedOutliers);
	}

	/**
	 * 현재 윈도우에서 이벤트 목록을 추출합니다.
	 */
	private List<PointerScrollEventDocument> extractWindowList(PointerScrollEventDocument[] window,
		int start,
		int end) {

		List<PointerScrollEventDocument> windowList = new ArrayList<>(end - start);
		for (int i = start; i < end; i++) {
			windowList.add(window[i]);
		}
		return windowList;
	}

	/**
	 * 현재 윈도우를 처리하고 rage scroll 수를 반환합니다.
	 */
	private int processCurrentWindow(List<PointerScrollEventDocument> windowList,
		Set<PointerScrollEventDocument> detectedOutliers) {

		List<PointerScrollEventDocument> windowOutliers = new ArrayList<>();
		int rageWithinWindow = countRageScrolls(windowList, windowOutliers);

		if (rageWithinWindow >= rageThresholdPerWindow) {
			detectedOutliers.addAll(windowOutliers);
		}

		return rageWithinWindow;
	}

	/**
	 * 오래된 이벤트를 제거하고 새로운 시작 인덱스를 반환합니다.
	 */
	private int removeOldEvents(PointerScrollEventDocument[] window, int start, int end,
		PointerScrollEventDocument current) {
		while (start < end && isOutOfTimeRange(window[start], current)) {
			start++;
		}
		return start;
	}

	/**
	 * rage scroll을 감지합니다.
	 * 짫은 시간 내 여러번 rage scroll이 있는 경우
	 * @param window PointerScrollEvent 리스트
	 * @return TIME_WINDOW_MS 동안 rage scroll 횟수 반환
	 */
	private int countRageScrolls(List<PointerScrollEventDocument> window, List<PointerScrollEventDocument> outliers) {
		int count = 0;
		int index = 0;

		while (index < window.size()) {
			List<PointerScrollEventDocument> subList = initializeSubList(window, index);
			int jumpIndex = findMatchingScrollEventsAndGetJumpIndex(window, subList, index, outliers);

			if (jumpIndex > index) {
				count++;
				index = jumpIndex; // 점프 처리
			} else {
				index++;
			}
		}

		return count;
	}

	/**
	 * 초기 subList를 생성합니다.
	 */
	private List<PointerScrollEventDocument> initializeSubList(List<PointerScrollEventDocument> window, int index) {
		List<PointerScrollEventDocument> subList = new ArrayList<>();
		subList.add(window.get(index));
		return subList;
	}

	/**
	 * 일치하는 스크롤 이벤트를 찾습니다.
	 */
	private int findMatchingScrollEventsAndGetJumpIndex(List<PointerScrollEventDocument> window,
		List<PointerScrollEventDocument> subList,
		int startIndex,
		List<PointerScrollEventDocument> outliers) {
		for (int j = startIndex + 1; j < window.size(); j++) {
			PointerScrollEventDocument next = window.get(j);
			subList.add(next);

			if (isRageScrollPattern(subList)) {
				log.debug("Rage Scroll 조건 만족 - 이벤트 수: {}, 스크롤 변화량: {}",
					subList.size(), scrollDelta(subList));
				outliers.addAll(subList);
				return j;
			}
		}
		return startIndex; // 조건 안 맞으면 점프 안 함
	}

	/**
	 * Rage 스크롤 패턴인지 확인합니다.
	 */
	private boolean isRageScrollPattern(List<PointerScrollEventDocument> subList) {
		return subList.size() >= minEventCount && scrollDelta(subList) >= minScrollDelta;
	}

	private boolean isOutOfTimeRange(PointerScrollEventDocument first, PointerScrollEventDocument current) {
		return Duration.between(first.getTimestamp(), current.getTimestamp()).toMillis() > timeWindowMs;
	}

	private int scrollDelta(List<PointerScrollEventDocument> events) {
		int min = events.stream().mapToInt(PointerScrollEventDocument::getScrollY).min().orElse(0);
		int max = events.stream().mapToInt(PointerScrollEventDocument::getScrollY).max().orElse(0);
		return Math.abs(max - min);
	}

	/**
	 * 이상치로 표시된 문서들을 마킹합니다.
	 */
	private void markOutliers(Set<PointerScrollEventDocument> outliers) {
		for (PointerScrollEventDocument outlier : outliers) {
			markAsOutlier(outlier);
		}
	}

	/**
	 * 단일 문서를 이상치로 마킹합니다.
	 */
	private void markAsOutlier(PointerScrollEventDocument outlier) {
		try {
			outlier.markAsOutlier();
		} catch (PointerEventException e) {
			log.debug("이미 이상치로 처리된 데이터입니다: {}", outlier.getId());
		}
	}

	/**
	 * 왕복 스크롤 여부를 감지합니다.
	 *
	 * @param events 시간순으로 정렬된 PointerScrollEvent 리스트
	 * @return 왕복 스크롤이 감지되면 true
	 */
	public void findBackAndForthScrollOutliers(List<PointerScrollEventDocument> events) {
		if (events == null || events.size() < 2) {
			log.debug("왕복 스크롤 분석 생략 - 이벤트 수 부족");
			return;
		}

		log.info("왕복 스크롤 분석 시작");

		Set<PointerScrollEventDocument> outliers = detectDirectionChanges(events);

		if (outliers.size() >= minDirectionChanges) {
			log.info("왕복 스크롤 이상치 감지 - 변경 횟수: {}", outliers.size());
			markOutliers(outliers);
		}
	}

	/**
	 * 방향 변경을 감지하고 관련 이상치를 반환합니다.
	 */
	private Set<PointerScrollEventDocument> detectDirectionChanges(List<PointerScrollEventDocument> events) {
		Set<PointerScrollEventDocument> outliers = new HashSet<>();
		Integer prevY = null;
		Integer prevDirection = null; // 1: down, -1: up

		for (PointerScrollEventDocument event : events) {
			int currentY = event.getScrollY();

			if (prevY != null) {
				int delta = currentY - prevY;
				int direction = Integer.compare(delta, 0); // 1: down, -1: up, 0: no move

				if (isDirectionChange(direction, prevDirection)) {
					outliers.add(event);
				}

				if (direction != 0) {
					prevDirection = direction;
				}
			}

			prevY = currentY;
		}

		return outliers;
	}

	/**
	 * 방향 변경이 발생했는지 확인합니다.
	 */
	private boolean isDirectionChange(int direction, Integer prevDirection) {
		return direction != 0 && prevDirection != null && direction != prevDirection;
	}

	/**
	 * 컨텐츠 소모율 감지해 일정 비율 이하인 경우 컨텐츠 소모를 충분히 하지 못한것으로 간주하며
	 * 간주한 데이터 중 가장 scrollY값이 큰 데이터를 outlier로 마킹합니다.
	 * @param events 스크롤 이벤트 목록 (시간순 정렬 가정)
	 * @return void
	 */
	public void findTopRepeatScrollOutliers(List<PointerScrollEventDocument> events) {
		if (events == null || events.isEmpty()) {
			log.debug("Top Scroll 분석 생략 - 이벤트 없음");
			return;
		}

		PointerScrollEventDocument maxScrollEvent = findMaxScrollEvent(events);

		if (maxScrollEvent != null) {
			log.info("컨텐츠 소모율 낮은 이상치 감지 - ID: {}, 소모율: {}",
				maxScrollEvent.getId(), calculateConsumedRatio(maxScrollEvent));
			markAsOutlier(maxScrollEvent);
		}
	}

	/**
	 * 가장 스크롤 소모율이 높은 이벤트를 찾습니다.
	 */
	private PointerScrollEventDocument findMaxScrollEvent(List<PointerScrollEventDocument> events) {
		double maxConsumedRatio = -1.0;
		PointerScrollEventDocument maxScrollEvent = null;

		for (PointerScrollEventDocument e : events) {
			if (e.getScrollHeight() == null || e.getScrollHeight() == 0) {
				continue;
			}

			double ratio = calculateConsumedRatio(e);

			if (ratio > maxConsumedRatio) {
				maxConsumedRatio = ratio;
				maxScrollEvent = e;
			}
		}

		// 소모율이 기준 이하인 경우에만 반환
		return maxConsumedRatio < contentConsumedThreshold ? maxScrollEvent : null;
	}

	/**
	 * 컨텐츠 소모율을 계산합니다.
	 */
	private double calculateConsumedRatio(PointerScrollEventDocument event) {
		int bottom = event.getScrollY() + event.getViewportHeight();
		return (double) bottom / event.getScrollHeight();
	}
}
