package com.dajava.backend.domain.register.service.pageCapture;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Base64;
import java.util.Comparator;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.dajava.backend.domain.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.image.service.pageCapture.LocalFileStorageService;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.repository.PageCaptureDataRepository;

@SpringBootTest
public class LocalFileStorageServiceTest {

	@Value("${image.path}")
	String path;

	@Autowired
	private PageCaptureDataRepository pageCaptureDataRepository;

	// 테스트 종료 후 "backend/images/page-capture" 하위의 모든 파일/디렉터리 삭제
	@AfterAll
	static void cleanup() throws IOException {
		Path baseDir = Paths.get("backend/images/page-capture");
		if (Files.exists(baseDir)) {
			Files.walk(baseDir)
				.sorted(Comparator.reverseOrder())
				.forEach(p -> {
					try {
						Files.deleteIfExists(p);
					} catch (IOException e) {
						System.err.println("삭제 실패: " + p);
					}
				});
		}
	}

	@Test
	@DisplayName("1. 신규 파일 업로드 시 이미지 저장 및 파일 생성 테스트")
	void t001() throws Exception {
		// Given
		LocalFileStorageService localFileStorageService = new LocalFileStorageService(path);
		String originalData = "테스트 이미지 데이터";
		// Base64 인코딩, data:image/png;base64, 접두어 포함 (포함해도 무방)
		String base64Encoded = Base64.getEncoder().encodeToString(originalData.getBytes(StandardCharsets.UTF_8));
		String base64Image = "data:image/png;base64," + base64Encoded;
		String originalFilename = "test-image.png";

		// When
		ImageSaveResponse saveResponse = localFileStorageService.storeBase64Image(base64Image, originalFilename);
		String fileName = saveResponse.fileName();

		// Then
		assertNotNull(fileName, "파일명이 null이어서는 안됩니다.");
		assertTrue(fileName.endsWith(".png"), "파일명이 .png 확장자로 끝나야 합니다.");

		// 실제 파일이 저장되었는지 확인
		Path filePath = Paths.get(path).resolve(fileName);
		assertTrue(Files.exists(filePath), "파일이 실제로 저장되어야 합니다.");

		// 저장된 파일의 내용이 일치하는지 확인
		byte[] storedContent = Files.readAllBytes(filePath);
		assertArrayEquals(originalData.getBytes(StandardCharsets.UTF_8),
			storedContent, "파일의 내용이 기대한 값과 일치해야 합니다.");
	}

	@Test
	@DisplayName("2. 기존 파일 덮어쓰기(Override) 이미지 업데이트 테스트")
	void t002() throws Exception {
		// Given
		LocalFileStorageService localFileStorageService = new LocalFileStorageService(path);

		// 먼저 신규 업로드로 파일 생성
		String originalData = "원본 파일 데이터";
		String base64Original = "data:image/png;base64," +
			Base64.getEncoder().encodeToString(originalData.getBytes(StandardCharsets.UTF_8));
		String originalFilename = "test-image.png";

		ImageSaveResponse initialResponse = localFileStorageService.storeBase64Image(base64Original, originalFilename);
		String initialFileName = initialResponse.fileName();
		Path filePath = Paths.get(path).resolve(initialFileName);

		assertTrue(Files.exists(filePath), "신규 업로드한 파일이 존재해야 합니다.");
		byte[] originalContent = Files.readAllBytes(filePath);
		assertArrayEquals(originalData.getBytes(StandardCharsets.UTF_8),
			originalContent, "저장된 원본 파일의 내용이 일치해야 합니다.");

		// 기존 엔티티(PageCaptureData)에 원래 파일명이 설정된 상태 생성
		PageCaptureData pageData = PageCaptureData.builder()
			.captureFileName(initialFileName)
			.pageUrl("http://localhost:3000/myPage")
			.build();

		// When: updateBase64Image 를 호출하여 기존 파일 덮어쓰기 수행
		String updatedData = "업데이트된 파일 데이터";
		String base64Updated = "data:image/png;base64," +
			Base64.getEncoder().encodeToString(updatedData.getBytes(StandardCharsets.UTF_8));
		// 원본 파일명과 단순히 확장자 추출을 위한 파일명 전달
		String newOriginalFilename = "test-image-updated.png";
		ImageSaveResponse updateResponse = localFileStorageService.updateBase64Image(base64Updated, pageData,
			newOriginalFilename);
		String updatedFileName = updateResponse.fileName();

		// Then
		assertNotNull(updatedFileName, "업데이트 후 파일명이 null이어서는 안 됩니다.");
		assertEquals(initialFileName, updatedFileName, "기존 파일명과 업데이트 후 파일명이 동일해야 합니다.");

		// 실제 파일 내용이 업데이트되었는지 확인
		byte[] updatedContent = Files.readAllBytes(filePath);
		assertArrayEquals(updatedData.getBytes(StandardCharsets.UTF_8),
			updatedContent, "파일 내용이 업데이트된 데이터와 일치해야 합니다.");
	}
}
