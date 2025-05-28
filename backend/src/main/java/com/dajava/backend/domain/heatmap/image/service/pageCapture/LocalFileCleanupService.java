package com.dajava.backend.domain.heatmap.image.service.pageCapture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.heatmap.image.utils.ImageCleanupUtils;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.repository.PageCaptureDataRepository;
import com.dajava.backend.domain.register.repository.RegisterRepository;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
@Profile("!prod")
public class LocalFileCleanupService implements FileCleanupService {

	// 파일 저장 경로 (외부 설정에서 주입)
	private final Path fileStorageLocation;
	private final ImageCleanupUtils imageCleanupUtils;
	private final PageCaptureDataRepository pageCaptureDataRepository;

	public LocalFileCleanupService(@Value("${image.path}") String storagePath, RegisterRepository registerRepository,
		PageCaptureDataRepository pageCaptureDataRepository, ImageCleanupUtils imageCleanupUtils,
		PageCaptureDataRepository pageCaptureDataRepository1) {
		this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
		this.imageCleanupUtils = imageCleanupUtils;
		this.pageCaptureDataRepository = pageCaptureDataRepository1;
		try {
			Files.createDirectories(this.fileStorageLocation);
		} catch (IOException ex) {
			throw new RuntimeException("디렉토리를 생성하지 못했습니다: " + this.fileStorageLocation, ex);
		}
	}

	/**
	 * 지정된 파일명을 사용하여 파일 시스템에서 파일을 삭제합니다.
	 *
	 * @param fileName 삭제할 파일의 이름 (UUID 기반 파일명 + 확장자)
	 */
	public void deleteFile(String fileName) {
		try {
			Path filePath = this.fileStorageLocation.resolve(fileName).normalize();
			Files.deleteIfExists(filePath);
		} catch (IOException ex) {
			throw new RuntimeException("파일 삭제에 실패했습니다: " + fileName, ex);
		}
	}

	/**
	 * 현재 Register 에 등록되있지 않은 URL 과 연관된 이미지 파일을 지웁니다.
	 * 해당 로직은 서버 시작시 최초 1회 기동합니다. (추후 스케줄링할 여지 있음)
	 */
	public void deleteNonLinkedFile() {
		log.info("연관되지 않은 모든 이미지 파일 삭제 시작");

		// Register에 있는 모든 URL Set
		Set<String> registerUrls = imageCleanupUtils.getRegisterUrls();

		// PageCaptureData에 있는 모든 URL Set
		Set<String> pageCaptureUrls = imageCleanupUtils.getPageCaptureUrls();

		// 제거 대상인 url 의 정보만 남김
		pageCaptureUrls.removeAll(registerUrls);

		for (String pageCaptureUrl : pageCaptureUrls) {
			PageCaptureData targetData = pageCaptureDataRepository.findByPageUrl(pageCaptureUrl);
			String fileName = targetData.getCaptureFileName();
			deleteFile(fileName);
		}
	}
}
