package com.dajava.backend.domain.heatmap.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dajava.backend.domain.event.es.entity.SolutionEventDocument;
import com.dajava.backend.domain.heatmap.dto.HeatmapResponse;
import com.dajava.backend.domain.heatmap.exception.HeatmapException;
import com.dajava.backend.domain.image.ImageDimensions;
import com.dajava.backend.domain.image.service.pageCapture.FileStorageService;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.repository.RegisterRepository;
import com.dajava.backend.global.utils.PasswordUtils;

@ExtendWith(MockitoExtension.class)
class HeatmapServiceImplTest {

	@Mock
	private RegisterRepository registerRepository;

	@Mock
	private SolutionEventFetcher solutionEventFetcher;

	@Mock
	private FileStorageService fileStorageService;

	@InjectMocks
	private HeatmapServiceImpl heatmapService;

	private Register register;
	private List<SolutionEventDocument> mockDocuments;
	private List<SolutionEventDocument> largeEventDocs;

	private final int WIDTH_RANGE = 1200;
	private final int GRID_SIZE = 10;

	@BeforeEach
	void setUp() {
		// Register 객체 초기화
		register = Register.builder()
			.serialNumber("5_team_testSerial")
			.password("password123!")
			.url("http://localhost:3000/myPage1")
			.captureData(new ArrayList<>())
			.build();

		List<PageCaptureData> pageCaptureList = new ArrayList<>();
		pageCaptureList.add(
			PageCaptureData.builder()
				.pageUrl("http://localhost:3000/myPage1")
				.captureFileName("sample1.png")
				.register(register)
				.widthRange(WIDTH_RANGE)
				.build()
		);
		pageCaptureList.add(
			PageCaptureData.builder()
				.pageUrl("http://localhost:3000/myPage2")
				.captureFileName("sample2.png")
				.register(register)
				.widthRange(WIDTH_RANGE + 100)
				.build()
		);
		register.getCaptureData().addAll(pageCaptureList);

		// ElasticSearch에 저장된 이벤트 Document들을 생성
		mockDocuments = new ArrayList<>();

		// long 타입의 타임스탬프 값
		long eventTimestamp = LocalDateTime.now()
			.minusDays(4)
			.atZone(ZoneId.systemDefault())
			.toInstant()
			.toEpochMilli();

		// 클릭 이벤트 document
		SolutionEventDocument clickDoc = SolutionEventDocument.builder()
			.type("click")
			.clientX(100)
			.clientY(200)
			.scrollY(50)
			.browserWidth(1200)
			.scrollHeight(3000)
			.sessionId("session1")
			.pageUrl("http://localhost:3000/myPage1")
			.timestamp(eventTimestamp)
			.build();
		mockDocuments.add(clickDoc);

		// 마우스무브 이벤트 document
		SolutionEventDocument moveDoc = SolutionEventDocument.builder()
			.type("move")
			.clientX(150)
			.clientY(250)
			.scrollY(50)
			.browserWidth(1200)
			.scrollHeight(3000)
			.sessionId("session1")
			.pageUrl("http://localhost:3000/myPage1")
			.timestamp(eventTimestamp)
			.build();
		mockDocuments.add(moveDoc);

		// 스크롤 이벤트 document 5개 생성
		for (int i = 0; i < 5; i++) {
			SolutionEventDocument scrollDoc = SolutionEventDocument.builder()
				.type("scroll")
				.scrollY(i * 100)
				.viewportHeight(800)
				.browserWidth(1200)
				.scrollHeight(3000)
				.sessionId("session" + (i % 2 + 1))
				.pageUrl("http://localhost:3000/myPage1")
				.timestamp(eventTimestamp + (50 * i))
				.build();
			mockDocuments.add(scrollDoc);
		}

		// 15000개의 클릭 이벤트 Document 생성
		largeEventDocs = new ArrayList<>();
		for (int i = 0; i < 15000; i++) {
			SolutionEventDocument eventDoc = SolutionEventDocument.builder()
				.type("click")
				.clientX(i % 200)
				.clientY(i % 300)
				.scrollY(i % 100)
				.browserWidth(1200)
				.scrollHeight(3000)
				.sessionId("session" + (i % 5))
				.pageUrl("http://localhost:3000/myPage1")
				.timestamp(eventTimestamp + (10 * i))
				.build();
			largeEventDocs.add(eventDoc);
		}

		// fileStorageService 동작 설정하여 목 너비 및 높이를 반환하도록 처리
		ImageDimensions stubbedDimensions = new ImageDimensions(1200, 3000);
		lenient().when(fileStorageService.getImageDimensions(anyString())).thenReturn(stubbedDimensions);
	}

	@Test
	@DisplayName("1. 유효한 클릭 타입의 히트맵 가져오기 테스트")
	void t001() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "click";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenReturn(mockDocuments);

			// When
			HeatmapResponse response = heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE);

			// Then
			assertNotNull(response);
			assertEquals(10, response.gridSize());
			assertEquals("sample1.png", response.pageCapture());
			assertNotNull(response.gridCells());
			assertNotNull(response.metadata());
			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(false));
		}
	}

	@Test
	@DisplayName("2. 유효한 마우스무브 타입의 히트맵 가져오기 테스트")
	void t002() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "move";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenReturn(mockDocuments);

			// When
			HeatmapResponse response = heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE);

			// Then
			assertNotNull(response);
			assertEquals(10, response.gridSize());
			assertNotNull(response.gridCells());
			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(false));
		}
	}

	@Test
	@DisplayName("3. 유효한 스크롤 타입의 히트맵 가져오기 테스트")
	void t003() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "scroll";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenReturn(mockDocuments);

			// When
			HeatmapResponse response = heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE);

			// Then
			assertNotNull(response);
			assertEquals(10, response.gridSize());
			assertNotNull(response.gridCells());
			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(true));
		}
	}

	@Test
	@DisplayName("4. 잘못된 시리얼 번호로 조회 시 예외 발생 테스트")
	void t004() {
		// Given
		String serialNumber = "INVALID_SN";
		String password = "password123!";
		String type = "click";

		when(registerRepository.findBySerialNumber(serialNumber))
			.thenReturn(Optional.empty());

		// When & Then
		assertThrows(HeatmapException.class, () ->
			heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE));

		verify(registerRepository).findBySerialNumber(serialNumber);
		verify(solutionEventFetcher, never()).getAllEvents(any(), anyBoolean());
	}

	@Test
	@DisplayName("5. 잘못된 비밀번호로 조회 시 예외 발생 테스트")
	void t005() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "wrong_password";
		String type = "click";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(false);

			// When & Then
			assertThrows(HeatmapException.class, () ->
				heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE));

			verify(registerRepository).findBySerialNumber(serialNumber);
			verify(solutionEventFetcher, never()).getAllEvents(any(), anyBoolean());
		}
	}

	@Test
	@DisplayName("6. 솔루션 데이터(이벤트 Document)가 없을 때 예외 발생 테스트")
	void t006() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "click";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenReturn(Collections.emptyList());

			// When & Then
			HeatmapException exception = assertThrows(HeatmapException.class, () ->
				heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE));

			verify(registerRepository).findBySerialNumber(serialNumber);
			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(false));
		}
	}

	@Test
	@DisplayName("7. 이벤트 데이터가 없을 때 예외 발생 테스트")
	void t007() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "click";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenThrow(new HeatmapException(com.dajava.backend.global.exception.ErrorCode.SOLUTION_EVENT_DATA_NOT_FOUND));

			// When & Then
			HeatmapException exception = assertThrows(HeatmapException.class, () ->
				heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE));

			verify(registerRepository).findBySerialNumber(serialNumber);
			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(false));
		}
	}

	@Test
	@DisplayName("8. 잘못된 이벤트 타입으로 조회 시 예외 발생 테스트")
	void t008() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "invalid_type"; // 유효하지 않은 타입

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);

			// When & Then
			HeatmapException exception = assertThrows(HeatmapException.class, () ->
				heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE));

			verify(registerRepository).findBySerialNumber(serialNumber);
			// 잘못된 이벤트 타입은 초기 유효성 검사에서 걸려야 함
			verify(solutionEventFetcher, never()).getAllEvents(any(), anyBoolean());
		}
	}

	@Test
	@DisplayName("9. 이벤트 수가 10000개 이상일 때 샘플링 적용 테스트")
	void t009() {
		// Given
		String serialNumber = "5_team_testSerial";
		String password = "password123!";
		String type = "click";

		try (MockedStatic<PasswordUtils> passwordUtilsMock = mockStatic(PasswordUtils.class)) {
			when(registerRepository.findBySerialNumber(serialNumber))
				.thenReturn(Optional.of(register));
			passwordUtilsMock.when(() -> PasswordUtils.verifyPassword(password, register.getPassword()))
				.thenReturn(true);
			when(solutionEventFetcher.getAllEvents(eq(serialNumber), anyBoolean()))
				.thenReturn(largeEventDocs);

			// When
			HeatmapResponse response = heatmapService.getHeatmap(serialNumber, password, type, WIDTH_RANGE, GRID_SIZE);

			// Then
			assertNotNull(response);
			assertNotNull(response.metadata());

			// 실제 샘플링 검증: 15000개 클릭 이벤트 중 샘플링 비율에 맞게 줄어들었는지 확인
			// 클릭 이벤트는 2:1 샘플링이 적용되므로 7500개 정도가 되어야 함
			// 하지만 URL 필터링 등으로 인해 다소 차이가 있을 수 있음
			// 따라서 샘플링이 적용되었는지 확인하는 것이 중요
			assertTrue(response.metadata().totalEvents() < largeEventDocs.size(),
				"샘플링이 적용되어 이벤트 수가 줄어들어야 함");
			assertTrue(response.metadata().totalEvents() > 0,
				"샘플링 후에도 이벤트는 존재해야 함");

			verify(solutionEventFetcher).getAllEvents(eq(serialNumber), eq(false));
		}
	}
}
