package com.dajava.backend.domain.image.service.pageCapture;

import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.dajava.backend.domain.image.ImageDimensions;
import com.dajava.backend.domain.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.register.entity.PageCaptureData;

import jakarta.servlet.http.HttpServletRequest;

public interface FileStorageService {

	Resource getImage(String fileName);

	ImageDimensions getImageDimensions(String fileName);

	String determineContentType(Resource resource, HttpServletRequest request);

	ImageSaveResponse storeBase64Image(String base64Image, String originalFilename);

	ImageSaveResponse updateBase64Image(String base64Image, PageCaptureData pageData, String originalFilename);

	ImageSaveResponse saveBase64ImageToFile(String base64Image, String fileName);

	// MultipartFile 에서 UUID 기반 파일명을 생성합니다.
	default String generateUniqueFileName(MultipartFile file) {
		return UUID.randomUUID().toString() + getExtension(file.getOriginalFilename());
	}

	// 파일명에서 확장자를 추출합니다.
	default String getExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		String ext = FilenameUtils.getExtension(originalFilename);
		return (ext != null && !ext.isEmpty()) ? "." + ext : "";
	}

	// 기존 파일 URL 에서 파일명만 추출하는 유틸 메서드
	default String extractFileName(String existingFileUrl) {
		if (existingFileUrl == null || existingFileUrl.isEmpty()) {
			throw new IllegalArgumentException("기존 파일 URL이 유효하지 않습니다.");
		}
		return existingFileUrl.substring(existingFileUrl.lastIndexOf("/") + 1);
	}
}
