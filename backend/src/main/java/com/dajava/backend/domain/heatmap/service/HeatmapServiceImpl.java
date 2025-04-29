package com.dajava.backend.domain.heatmap.service;

import static com.dajava.backend.global.exception.ErrorCode.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.heatmap.dto.GridCell;
import com.dajava.backend.domain.heatmap.dto.HeatmapMetadata;
import com.dajava.backend.domain.heatmap.dto.HeatmapResponse;
import com.dajava.backend.domain.heatmap.dto.HeatmapWidthsResponse;
import com.dajava.backend.domain.heatmap.exception.HeatmapException;
import com.dajava.backend.domain.image.ImageDimensions;
import com.dajava.backend.domain.image.service.pageCapture.FileStorageService;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.repository.RegisterRepository;
import com.dajava.backend.domain.solution.exception.SolutionException;
import com.dajava.backend.global.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 그리드 데이터 생성을 위한 서비스 로직
 * 모든 SolutionEvent 를 각 타입별로 구분하여 그리드 데이터를 누적합니다.
 * 5분 이내에 동일한 요청이 들어오면 캐싱된 데이터를 반환합니다.
 * @author Metronon
 * @since 2025-04-03
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HeatmapServiceImpl implements HeatmapService {

	private final RegisterRepository registerRepository;
	private final FileStorageService fileStorageService;
	private final SolutionEventFetcher solutionEventFetcher;

	// 현재 구현된 히트맵 타입
	private static final Set<String> EVENT_TYPES = Set.of("click", "move", "scroll");

	@Override
	@Cacheable(value = "heatmapCache", key = "{#serialNumber, #type, #widthRange}")
	@Transactional(readOnly = true)
	public HeatmapResponse getHeatmap(String serialNumber, String password, String type, int widthRange, int gridSize) {
		// 히트맵 데이터 생성 시작 시각
		long startTime = System.currentTimeMillis();

		// 타입값이 잘못되었다면 예외 발생
		String lowerType = type.toLowerCase();
		if (!EVENT_TYPES.contains(lowerType)) {
			throw new HeatmapException(INVALID_EVENT_TYPE);
		}

		// type 이 scroll 이면 정렬 플래그 변경
		boolean sortByTimestamp = type.equals("scroll");

		try {
			Register findRegister = registerRepository.findBySerialNumber(serialNumber)
				.orElseThrow(() -> new HeatmapException(SOLUTION_SERIAL_NUMBER_INVALID));

			// 현재 하나의 URL 만을 받기 때문에 이벤트 필터링을 위해 필요
			String targetUrl = findRegister.getUrl();

			// 해싱된 password 로 접근 권한 확인
			if (!PasswordUtils.verifyPassword(password, findRegister.getPassword())) {
				throw new HeatmapException(SOLUTION_PASSWORD_INVALID);
			}

			// ES에 접근 후 데이터 가져오도록 변경
			List<SolutionEventDocument> events = solutionEventFetcher.getAllEvents(serialNumber, sortByTimestamp);

			// targetUrl 과 일치하고, width 가 widthRange 조건에 충족한 이벤트만 필터링 (프로토콜 무시)
			List<SolutionEventDocument> filteredEvents = SolutionEventManager.getValidEvents(events, targetUrl, widthRange);

			// scroll 타입이 아니라면 타입에 따라 이벤트 필터링
			if (!sortByTimestamp) {
				filteredEvents = SolutionEventManager.getTargetEvents(filteredEvents, type);
			}

			// 샘플링 처리 전 totalEvents
			int totalEvents = filteredEvents.size();

			// 이벤트 샘플링으로 데이터가 방대한 경우 반환 시간 최적화
			if (filteredEvents.size() > 10000) {
				filteredEvents = sampleEvents(filteredEvents, type);
			}

			// 그리드 생성 로직으로 결과값 생성
			HeatmapResponse response;
			if (sortByTimestamp) {
				response = createScrollDepthHeatmap(filteredEvents, targetUrl, gridSize);
			} else {
				response = createCoordinateHeatmap(filteredEvents, targetUrl, gridSize);
			}

			// toBuilder 를 통해 pageCapture 경로값 추가
			List<PageCaptureData> captureDataList = findRegister.getCaptureData();

			Optional<PageCaptureData> optionalData = captureDataList.stream()
				.filter(data -> data.getPageUrl().equals(targetUrl)
					&& data.getWidthRange() == widthRange)
				.findFirst();

			if (optionalData.isPresent()) {
				String captureFileName = optionalData.get().getCaptureFileName();

				// 이미지의 높이, 너비를 BufferedImage 로 변환후 획득
				ImageDimensions imageDimensions = fileStorageService.getImageDimensions(captureFileName);
				int pageWidth = imageDimensions.pageWidth();
				int pageHeight = imageDimensions.pageHeight();

				if (pageWidth == 0 && pageHeight == 0) {
					// 이미지가 알 수 없는 이유로 손상된 경우 이벤트 기반 페이지 너비, 높이, 그리드 사이즈 그대로 사용
					response = response.toBuilder()
						.gridSize(gridSize)
						.pageCapture("")
						.build();
				} else {
					response = response.toBuilder()
						.gridSize(gridSize)
						.gridSizeX(pageWidth / gridSize)
						.gridSizeY(pageHeight / gridSize)
						.pageCapture(captureFileName)
						.pageWidth(pageWidth)
						.pageHeight(pageHeight)
						.build();
				}
			} else {
				// 이미지 관련 데이터가 없어도 반환된 데이터를 그대로 사용
				response = response.toBuilder()
					.gridSize(gridSize)
					.pageCapture("")
					.build();
			}

			// 소요 시간 측정
			long endTime = System.currentTimeMillis();
			log.info("히트맵 생성 성능 분석 결과: 일련 번호={}, type={}, totalEvent={}, afterSamplingEvent={} 소요시간={}ms",
				serialNumber, type, totalEvents, events.size(), (endTime - startTime)
			);

			return response;
		} catch (SolutionException e) {
			long endTime = System.currentTimeMillis();
			log.info("히트맵 생성 성능 분석 결과: 일련 번호={}, type={}, 소요시간={}ms, 오류={}",
				serialNumber, type, (endTime - startTime), e.getMessage(), e
			);
			throw e;
		}
	}

	/**
	 * 빈 히트맵 응답을 생성하는 로직입니다.
	 * 솔루션에 대해 이벤트가 존재하지 않는 경우 빈 응답을 반환합니다.
	 */
	private HeatmapResponse createEmptyHeatmapResponse() {
		return HeatmapResponse.builder()
			.gridSizeX(103)
			.gridSizeY(103)
			.pageWidth(1024)
			.pageHeight(1024) // 기본값
			.gridCells(Collections.emptyList())
			.metadata(HeatmapMetadata.builder()
				.maxCount(0)
				.totalEvents(0)
				.pageUrl("unknown")
				.totalSessions(0)
				.build())
			.build();
	}

	/**
	 * 이벤트 목록에서 샘플링을 수행합니다.
	 * 이벤트 수가 많을 경우 모든 이벤트를 처리하는 대신 일부만 샘플링하여 효율성을 높힐 수 있습니다.
	 */
	private List<SolutionEventDocument> sampleEvents(List<SolutionEventDocument> events, String eventType) {
		int sampleRate;

		if ("move".equalsIgnoreCase(eventType)) {
			sampleRate = events.size() > 10000 ? 10 : 5; // 이동 이벤트가 10000개 이상 ? 20 : 1 / 10 : 1
		} else if ("scroll".equalsIgnoreCase(eventType)) {
			sampleRate = 5; // 스크롤 이벤트 5 : 1
		} else {
			sampleRate = 2; // 클릭 이벤트 2 : 1
		}

		List<SolutionEventDocument> sampledEvents = new ArrayList<>();
		for (int i = 0; i < events.size(); i++) {
			if (i % sampleRate == 0) {
				sampledEvents.add(events.get(i));
			}
		}

		log.info("이벤트 샘플링 적용: {} 이벤트 {} -> {}", eventType, events.size(), sampledEvents.size());
		return sampledEvents;
	}

	/**
	 * 클릭 및 이동 타입 히트맵 생성 로직
	 * 타입에 맞는 이벤트에 따라 분석해 히트맵 데이터를 반환합니다.
	 *
	 * @param events serialNumber 를 통해 가져온 세션 데이터
	 * @param type 세션 데이터에서 추출할 로그 데이터의 타입
	 * @return HeatmapResponse 그리드 데이터와 메타 데이터를 포함한 히트맵 응답 DTO
	 */
	private HeatmapResponse createCoordinateHeatmap(List<SolutionEventDocument> events, String targetUrl, int gridSize) {
		// 필터링 결과가 없으면 빈 히트맵 리턴
		if (events.isEmpty()) {
			return createEmptyHeatmapResponse();
		}

		// 전체 이벤트에서 max 값 사전 설정
		int maxPageWidth = SolutionEventManager.getMaxPageWidth(events);
		int maxPageHeight = SolutionEventManager.getMaxPageHeight(events);

		// 그리드 갯수 계산
		int totalGridsX = maxPageWidth / gridSize;
		int totalGridsY = maxPageHeight / gridSize;

		// 그리드 맵 - 좌표를 키로 사용하는 HashMap
		Map<Integer, Integer> gridMap = new HashMap<>();

		// 이벤트 시간 초기화
		LocalDateTime firstEventTime = null;
		LocalDateTime lastEventTime = null;

		// 세션을 구분하기 위한 고유 식별자 저장 HashSet
		Set<String> sessionIds = new HashSet<>();

		for (SolutionEventDocument event : events) {
			// 총 세션수를 count 하기 위해 HashSet 에 추가함
			if (event.getSessionId() != null) {
				sessionIds.add(event.getSessionId());
			}

			// 좌표값이 없다면 건너뜀
			if (event.getClientX() == null || event.getClientY() == null) {
				continue;
			}

			int x = event.getClientX();
			int y = event.getClientY() + event.getScrollY();

			// 상대 좌표로 변환
			// 강제 형변환에서 소수점이 버려지기 때문에 float 사용으로 메모리 사용 최적화
			float relativeX = (float) x / event.getBrowserWidth();
			float relativeY = (float) y / event.getScrollHeight();

			// 이벤트 시간 업데이트
			if (firstEventTime == null || event.getTimestamp().isBefore(firstEventTime)) {
				firstEventTime = event.getTimestamp();
			}
			if (lastEventTime == null || event.getTimestamp().isAfter(lastEventTime)) {
				lastEventTime = event.getTimestamp();
			}

			// 강제 형변환으로 그리드 할당
			int gridX = (int) (relativeX * totalGridsX);
			int gridY = (int) (relativeY * totalGridsY);

			// 그리드 범위 제한
			gridX = Math.clamp(gridX, 0, totalGridsX - 1);
			gridY = Math.clamp(gridY, 0, totalGridsY - 1);

			// String gridKey = gridX + ":" + gridY;
			Integer gridKey = gridY * totalGridsX + gridX;

			// 해당 그리드 셀 카운트 증가
			gridMap.put(gridKey, gridMap.getOrDefault(gridKey, 0) + 1);
		}

		// 최대 카운트 값
		int maxCount = gridMap.values().stream().max(Integer::compareTo).orElse(0);

		// 그리드 셀 리스트 생성
		List<GridCell> gridCells = new ArrayList<>();
		for (Map.Entry<Integer, Integer> entry : gridMap.entrySet()) {
			Integer gridKey = entry.getKey();
			int gridX = gridKey % totalGridsX;
			int gridY = (gridKey - gridX) / totalGridsX;
			// int gridX = Integer.parseInt(coordinates[0]);
			// int gridY = Integer.parseInt(coordinates[1]);
			int count = entry.getValue();

			// 최대 카운트 값 대비 강도 계산
			int intensity = maxCount > 0 ? (int)(((double)count / maxCount) * 100) : 0;

			gridCells.add(GridCell.builder()
				.gridX(gridX)
				.gridY(gridY)
				.count(count)
				.intensity(intensity)
				.build());
		}

		int totalSessions = sessionIds.size();

		// 메타데이터 생성
		HeatmapMetadata metadata = HeatmapMetadata.builder()
			.maxCount(maxCount)
			.totalEvents(events.size())
			.pageUrl(targetUrl)
			.totalSessions(totalSessions)
			.firstEventTime(firstEventTime)
			.lastEventTime(lastEventTime)
			.build();

		// Heatmap Response 생성
		return HeatmapResponse.builder()
			.gridSizeX(maxPageWidth / gridSize)
			.gridSizeY(maxPageHeight / gridSize)
			.pageWidth(maxPageWidth)
			.pageHeight(maxPageHeight)
			.gridCells(gridCells)
			.metadata(metadata)
			.build();
	}

	/**
	 * Scroll Depth 히트맵 생성 로직
	 * 전체 이벤트 타입의 로그에 대해 화면 체류 시간을 측정해 히트맵 데이터를 반환합니다.
	 *
	 * @param events serialNumber 를 통해 가져온 세션 데이터
	 * @return HeatmapResponse 그리드 데이터와 메타 데이터를 포함한 히트맵 응답 DTO
	 */
	private HeatmapResponse createScrollDepthHeatmap(List<SolutionEventDocument> events, String targetUrl, int gridSize) {
		// 필터링 결과가 없으면 빈 히트맵 리턴
		if (events.isEmpty()) {
			return createEmptyHeatmapResponse();
		}

		// 전체 이벤트에서 max 값 사전 설정
		int maxPageWidth = SolutionEventManager.getMaxPageWidth(events);
		int maxPageHeight = SolutionEventManager.getMaxPageHeight(events);

		// 시간순 정렬로 데이터를 가져오므로, 첫 데이터와 마지막 데이터로 시간 설정
		LocalDateTime firstEventTime = events.getFirst().getTimestamp();
		LocalDateTime lastEventTime = events.getLast().getTimestamp();

		// 화면 체류 시간 저장을 위한 HashMap
		Map<Integer, Long> durationByRelativeGridY = new HashMap<>();

		// 세션을 구분하기 위한 고유 식별자 저장 HashSet
		Set<String> sessionIds = new HashSet<>();

		// 시간 간격 비교를 위한 직전 이벤트
		SolutionEventDocument prevEvent = events.getFirst();

		// 첫번째 데이터 로그의 sessionId 를 HashSet 에 저장
		sessionIds.add(prevEvent.getSessionId());

		// event 리스트에서 전후 데이터의 타임스탬프를 비교해 grid 정보를 생성하는 로직
		for (int i = 1; i < events.size(); i++) {
			SolutionEventDocument cntEvent = events.get(i);

			// 총 세션수를 count 하기 위해 HashSet 에 추가함
			if (cntEvent.getSessionId() != null) {
				sessionIds.add(cntEvent.getSessionId());
			}

			// 두 이벤트 시간 간격 계산
			long duration = Duration.between(prevEvent.getTimestamp(), cntEvent.getTimestamp()).toMillis();

			// 이벤트 시간 간격이 30초 이상인 경우 5초로 재설정
			if (duration > 30000) {
				duration = 5000;
			}

			// 이전 이벤트 위치의 Y 좌표에 따른 화면 Top, Bottom 설정
			int viewportTop = prevEvent.getScrollY() != null ? prevEvent.getScrollY() : 0;
			int viewportHeight = prevEvent.getViewportHeight() != null ? prevEvent.getViewportHeight() : 1024;
			int viewportBottom = viewportTop + viewportHeight;

			int scrollHeight = prevEvent.getScrollHeight() != null ? prevEvent.getScrollHeight() : viewportHeight;

			// 상대적 위치 계산 (0~1 범위)
			double relativeTop = (double) viewportTop / scrollHeight;
			double relativeBottom = (double) viewportBottom / scrollHeight;

			// 상대 위치를 총 그리드 개수에 맞게 스케일링
			int totalGridsY = maxPageHeight / gridSize;
			int gridYStart = (int) (relativeTop * totalGridsY);
			int gridYEnd = (int) (relativeBottom * totalGridsY);

			// 그리드 범위 제한
			gridYStart = Math.max(0, gridYStart);
			gridYEnd = Math.min(totalGridsY - 1, gridYEnd);

			// 지정된 범위로 각 그리드에 체류 시간 설정
			for (int gridY = gridYStart; gridY <= gridYEnd; gridY++) {
				durationByRelativeGridY.put(gridY, durationByRelativeGridY.getOrDefault(gridY, 0L) + duration);
			}

			prevEvent = cntEvent;
		}

		// 최대 체류 시간
		long maxDuration = durationByRelativeGridY.values().stream().max(Long::compareTo).orElse(1L);

		// 그리드 셀 리스트 생성
		List<GridCell> gridCells = new ArrayList<>();
		for (Map.Entry<Integer, Long> entry : durationByRelativeGridY.entrySet()) {
			int gridY = entry.getKey();
			long duration = entry.getValue();

			// 페이지 width 를 그리드 단위로 계산
			int widthInGrids = Math.max(1, maxPageWidth / gridSize);

			// 최대 체류 시간 대비 강도 계산
			int intensity = (int)((duration * 100.0) / maxDuration);

			// 전체 페이지 width 로 히트맵 생성
			for (int gridX = 0; gridX < widthInGrids; gridX++) {
				gridCells.add(GridCell.builder()
					.gridX(gridX)
					.gridY(gridY)
					.count(duration > 100 ? (int)(duration / 100.0) : 1)
					.intensity(intensity)
					.build());
			}
		}

		int totalSessions = sessionIds.size();

		// 메타데이터 생성
		HeatmapMetadata metadata = HeatmapMetadata.builder()
			.maxCount((int)(maxDuration / 100))
			.totalEvents(events.size())
			.pageUrl(targetUrl)
			.totalSessions(totalSessions)
			.firstEventTime(firstEventTime)
			.lastEventTime(lastEventTime)
			.build();

		// Heatmap Response 생성
		return HeatmapResponse.builder()
			.gridSizeX(maxPageWidth / gridSize)
			.gridSizeY(maxPageHeight / gridSize)
			.pageWidth(maxPageWidth)
			.pageHeight(maxPageHeight)
			.gridCells(gridCells)
			.metadata(metadata)
			.build();
	}

	/**
	 * 현재 조회시 들어온 이미지 및 이벤트 로그로 인해 히트맵을 생성할 수 있는 widthRange 의 List 를 반환합니다.
	 * 이를 통해 사용자가 원하는 너비의 히트맵을 선택해 확인할 수 있게 합니다.
	 *
	 * @param serialNumber register 객체를 얻어오기 위한 serialNumber
	 * @param password 접근 권한을 확인하기 위한 인증 수단
	 * @return HeatmapWidthsResponse widthRange 리스트
	 */
	@Override
	public HeatmapWidthsResponse getWidths(String serialNumber, String password) {
		List<PageCaptureData> pageCaptureDatas = new ArrayList<>();

		Register findRegister = registerRepository.findBySerialNumber(serialNumber)
			.orElseThrow(() -> new HeatmapException(SOLUTION_SERIAL_NUMBER_INVALID));

		String targetUrl = findRegister.getUrl();

		// 해싱된 password 로 접근 권한 확인
		if (!PasswordUtils.verifyPassword(password, findRegister.getPassword())) {
			throw new HeatmapException(SOLUTION_PASSWORD_INVALID);
		}

		if (!findRegister.getCaptureData().isEmpty()) {
			pageCaptureDatas = findRegister.getCaptureData();
		}

		List<Integer> widthRanges = pageCaptureDatas.stream()
			.filter(data -> data.getPageUrl().equals(targetUrl))
			.map(PageCaptureData::getWidthRange)
			.toList();

		return new HeatmapWidthsResponse(widthRanges);
	}
}
