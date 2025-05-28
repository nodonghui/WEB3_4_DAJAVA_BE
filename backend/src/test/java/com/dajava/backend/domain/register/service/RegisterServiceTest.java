package com.dajava.backend.domain.register.service;

import static org.mockito.Mockito.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import com.dajava.backend.domain.register.email.AsyncEmailSender;
import com.dajava.backend.domain.heatmap.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.heatmap.image.service.pageCapture.LocalFileStorageService;
import com.dajava.backend.domain.register.dto.pageCapture.PageCaptureRequest;
import com.dajava.backend.domain.register.dto.pageCapture.PageCaptureResponse;
import com.dajava.backend.domain.register.dto.register.RegisterCreateRequest;
import com.dajava.backend.domain.register.dto.register.RegisterCreateResponse;
import com.dajava.backend.domain.register.dto.register.RegisterModifyRequest;
import com.dajava.backend.domain.register.dto.register.RegistersInfoRequest;
import com.dajava.backend.domain.register.dto.register.RegistersInfoResponse;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.repository.RegisterRepository;

@SpringBootTest
@Transactional
class RegisterServiceTest {

	@Autowired
	RegisterService service;

	@Autowired
	RegisterRepository repository;

	@MockitoBean
	LocalFileStorageService localFileStorageService;

	@MockitoBean
	private AsyncEmailSender asyncEmailSender;

	@BeforeEach
	void setUp() throws Exception {
		// Register Repository 전부 삭제
		repository.deleteAll();

		// email 관련 작업 실제로 송신이 이루어지지 않게 방지
		doNothing().when(asyncEmailSender).sendEmail(anyString(), anyString(), anyString());
	}

	/**
	 * 테스트를 위한 더미 PNG 이미지를 생성하여 Base64 문자열("data:image/png;base64,..." 포함)로 반환합니다.
	 * 서비스에서는 이 문자열을 읽어 디코딩하므로, 실제 유효한 이미지인지 확인할 수 있습니다.
	 *
	 * @param width  이미지 너비 (예: 800 이상이어야 MOBILE_VIEW_NOT_SUPPORTED 예외가 발생하지 않습니다)
	 * @param height 이미지 높이
	 * @return "data:image/png;base64," 접두어 포함된 Base64 문자열
	 * @throws IOException 만약 이미지 생성에 실패할 경우
	 */
	private String createDummyBase64Image(int width, int height) throws IOException {
		BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(image, "png", baos);
		byte[] imageBytes = baos.toByteArray();
		String base64 = Base64.getEncoder().encodeToString(imageBytes);
		return "data:image/png;base64," + base64;
	}

	@Test
	@DisplayName("솔루션 생성")
	public void t1() {
		RegisterCreateRequest request = new RegisterCreateRequest(
			"example@example.com",
			"password123!",
			"localhost:3000/test123",
			LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1L),
			LocalDateTime.now().plusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0)
		);

		RegisterCreateResponse response = service.createRegister(request);
		Assertions.assertNotNull(response);
		Assertions.assertEquals(1, repository.findAll().size());
	}

	@Test
	@DisplayName("솔루션 수정")
	public void t2() {
		t1();

		Register solution = repository.findAll().get(0);
		Long solutionId = solution.getId();
		RegisterModifyRequest request = new RegisterModifyRequest(
			solution.getEndDate().plusDays(3).withHour(0).withMinute(0).withSecond(0).withNano(0), "abcd"
		);
		int curDuration = solution.getDuration();

		service.modifySolution(request, solutionId);
		Register modifiedSolution = repository.findById(solutionId).get();

		Assertions.assertNotNull(modifiedSolution);
		Assertions.assertEquals(curDuration + 72, modifiedSolution.getDuration());
	}

	@Test
	@DisplayName("솔루션 리스트 조회")
	public void t3() {
		for (int i = 0; i < 10; i++) {
			RegisterCreateRequest request = new RegisterCreateRequest(
				"example@example.com",
				"password123!",
				"localhost:3000/test123" + i,
				LocalDateTime.now().withHour(0).withMinute(0).withSecond(0).withNano(0).plusDays(1L),
				LocalDateTime.now().plusDays(7).withHour(0).withMinute(0).withSecond(0).withNano(0)
			);
			service.createRegister(request);
		}

		RegistersInfoRequest request = new RegistersInfoRequest(5, 1);
		RegistersInfoResponse registerList = service.getRegisterList(request);
		Assertions.assertNotNull(registerList);
		Assertions.assertEquals(5, registerList.registerInfos().size());
	}

	@Test
	@DisplayName("캡쳐 데이터 Post - 성공 (새 이미지)")
	public void t4() throws IOException {
		// Given
		t1();
		Register register = repository.findAll().get(0);
		String serialNumber = register.getSerialNumber();
		String pageUrl = "http://localhost:3000/test";

		// 더미 이미지를 생성 (너비 800이면 widthRange = (800/100)*100 = 800, 모바일/작은 뷰 방지)
		String base64ImageContent = createDummyBase64Image(800, 600);

		// 실제 서비스에서는 MultipartFile로 넘어오므로, Base64 문자열을 bytes로 감싼 MockMultipartFile 생성
		MockMultipartFile imageFile = new MockMultipartFile(
			"imageFile",
			"test-image.png",
			MediaType.IMAGE_PNG_VALUE,
			base64ImageContent.getBytes(StandardCharsets.UTF_8)
		);

		// fileStorageService.storeBase64Image() 모킹:
		// 호출 시 Base64 문자열과 원본파일명을 받아 임의 파일명(dynamicFileName)을 반환하는 ImageSaveResponse 반환
		String dynamicFileName = UUID.randomUUID().toString() + ".png";
		ImageSaveResponse storeResponse = ImageSaveResponse.builder()
			.fileName(dynamicFileName)
			.widthRange(800)
			.build();
		when(localFileStorageService.storeBase64Image(anyString(), anyString()))
			.thenReturn(storeResponse);

		// When: 페이지 캡쳐 데이터 생성 요청 (내부에서 Base64 문자열 추출 및 디코딩, 너비 계산 등 수행)
		PageCaptureRequest request = new PageCaptureRequest(serialNumber, pageUrl, imageFile);
		PageCaptureResponse response = service.createPageCapture(request);

		// Then: 응답 확인
		Assertions.assertTrue(response.success(), "응답 결과는 성공이어야 합니다.");
		Assertions.assertEquals("페이지 캡쳐 데이터가 성공적으로 저장되었습니다.", response.message());
		Assertions.assertEquals(dynamicFileName, response.captureFileName());

		// Repository 에서 해당 Register 의 캡쳐 데이터 업데이트 확인
		Optional<Register> updatedRegister = repository.findBySerialNumber(serialNumber);
		Assertions.assertTrue(updatedRegister.isPresent(), "업데이트된 Register가 존재해야 합니다.");
		Register modifiedRegister = updatedRegister.get();
		Assertions.assertFalse(modifiedRegister.getCaptureData().isEmpty(), "캡쳐 데이터가 존재해야 합니다.");

		// 새로 추가된 PageCaptureData 는 pageUrl, 저장된 파일명, 그리고 widthRange(800)를 포함해야 합니다.
		PageCaptureData newData = modifiedRegister.getCaptureData().get(0);
		Assertions.assertEquals(pageUrl, newData.getPageUrl());
		Assertions.assertEquals(dynamicFileName, newData.getCaptureFileName());
		Assertions.assertEquals(800, newData.getWidthRange());

		// storeBase64Image()가 한 번 호출되었는지 검증
		verify(localFileStorageService, times(1)).storeBase64Image(anyString(), anyString());
	}

	@Test
	@DisplayName("캡쳐 데이터 Post - 성공 (기존 이미지 오버라이드)")
	public void t5() throws IOException {
		// Given
		t1();
		Register register = repository.findAll().get(0);
		String serialNumber = register.getSerialNumber();
		String pageUrl = "http://localhost:3000/test";

		// 기존 캡쳐 데이터 추가 (widthRange 800일 때)
		String existingFileName = "existingImage.png";
		int widthRange = 800;
		PageCaptureData existingData = PageCaptureData.builder()
			.pageUrl(pageUrl)
			.captureFileName(existingFileName)
			.widthRange(widthRange)
			.register(register)
			.build();
		register.getCaptureData().add(existingData);
		repository.save(register);

		// 더미 이미지 생성 (업데이트된 이미지, 동일 너비 800)
		String base64ImageContent = createDummyBase64Image(800, 600);
		MockMultipartFile imageFile = new MockMultipartFile(
			"imageFile",
			"updated-image.png",
			MediaType.IMAGE_PNG_VALUE,
			base64ImageContent.getBytes(StandardCharsets.UTF_8)
		);

		// fileStorageService.updateBase64Image() 모킹:
		// 기존 이미지를 덮어쓸 때 기존 파일명(existingFileName)을 그대로 반환하도록 함.
		ImageSaveResponse updateResponse = ImageSaveResponse.builder()
			.fileName(existingFileName)
			.widthRange(800)
			.build();
		when(localFileStorageService.updateBase64Image(anyString(), any(PageCaptureData.class), anyString()))
			.thenReturn(updateResponse);

		// When: 페이지 캡쳐 데이터 업데이트 요청
		PageCaptureRequest request = new PageCaptureRequest(serialNumber, pageUrl, imageFile);
		PageCaptureResponse response = service.createPageCapture(request);

		// Then: 응답 검증
		Assertions.assertTrue(response.success(), "응답 결과는 성공이어야 합니다.");
		Assertions.assertEquals("페이지 캡쳐 데이터가 성공적으로 저장되었습니다.", response.message());
		// 업데이트 시에도 기존 파일명(existingFileName)이 그대로 사용되어야 함.
		Assertions.assertEquals(existingFileName, response.captureFileName());

		// Repository에서 Register의 캡쳐 데이터가 업데이트되었는지 확인
		Optional<Register> updatedRegister = repository.findBySerialNumber(serialNumber);
		Assertions.assertTrue(updatedRegister.isPresent(), "업데이트된 Register가 존재해야 합니다.");
		Register modifiedRegister = updatedRegister.get();
		Assertions.assertFalse(modifiedRegister.getCaptureData().isEmpty(), "캡쳐 데이터가 존재해야 합니다.");
		PageCaptureData data = modifiedRegister.getCaptureData().get(0);
		Assertions.assertEquals(pageUrl, data.getPageUrl());
		Assertions.assertEquals(existingFileName, data.getCaptureFileName());
		Assertions.assertEquals(widthRange, data.getWidthRange());

		// fileStorageService의 updateBase64Image()가 한 번 호출되었는지 검증
		verify(localFileStorageService, times(1))
			.updateBase64Image(anyString(), any(PageCaptureData.class), anyString());
	}
}
