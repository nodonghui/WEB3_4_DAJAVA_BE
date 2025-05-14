package com.dajava.backend.domain.image.service.pageCapture;

import java.util.UUID;

import org.apache.commons.io.FilenameUtils;
import org.springframework.core.io.Resource;

import com.dajava.backend.domain.image.dto.ImageDimensions;
import com.dajava.backend.domain.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.register.entity.PageCaptureData;

import jakarta.servlet.http.HttpServletRequest;

public interface FileStorageService {

	Resource getImage(String fileName);

	ImageDimensions getImageDimensions(String fileName);

	ImageSaveResponse saveBase64ImageToFile(String base64Image, String fileName);

	/**
	 * Base64로 인코딩된 이미지 데이터를 저장하고 파일명을 반환합니다.
	 *
	 * @param base64Image Base64로 인코딩된 이미지 데이터 (data:image/jpeg;base64, 포함 가능)
	 * @param originalFilename 원본 파일명 (확장자 추출용)
	 * @return 저장된 파일명
	 */
	default ImageSaveResponse storeBase64Image(String base64Image, String originalFilename) {
		String fileExtension = getExtension(originalFilename);
		String fileName = UUID.randomUUID().toString() + fileExtension;

		return saveBase64ImageToFile(base64Image, fileName);
	}

	/**
	 * Base64로 인코딩된 이미지 데이터 오버라이딩을 위해 기존 파일명을 반환합니다.
	 *
	 * @param base64Image Base64로 인코딩된 이미지 데이터
	 * @param pageData 저장된 파일명 (오버라이딩 작업시 이름 추출용)
	 * @param originalFilename 원본 파일명 (확장자 추출용)
	 * @return 저장된 파일명
	 */
	default ImageSaveResponse updateBase64Image(String base64Image, PageCaptureData pageData, String originalFilename) {
		String fileExtension = getExtension(originalFilename);
		String fileName = pageData.getCaptureFileName();

		// 기존 파일명이 없는 경우 새로 생성
		if (fileName == null || fileName.isEmpty()) {
			fileName = UUID.randomUUID().toString() + fileExtension;
		} else {
			// 기존 파일명의 확장자 검사 및 조정
			if (!fileName.endsWith(fileExtension)) {
				fileName = fileName.substring(0, fileName.lastIndexOf('.')) + fileExtension;
			}
		}

		return saveBase64ImageToFile(base64Image, fileName);
	}

	/**
	 * 파일명에서 확장자를 추출하는 메서드입니다.
	 *
	 * @param originalFilename 확장자를 포함한 파일명
	 * @return String 확장자
	 */
	default String getExtension(String originalFilename) {
		if (originalFilename == null) {
			return "";
		}
		String ext = FilenameUtils.getExtension(originalFilename);
		return (ext != null && !ext.isEmpty()) ? "." + ext : "";
	}

	/**
	 * MIME 타입을 반환하는 메서드입니다.
	 *
	 * @param resource 이미지 Resource
	 * @param request 이미지 저장 요청 데이터
	 * @return String 요청에서 추출한 MIME 타입
	 */
	default String determineContentType(Resource resource, HttpServletRequest request) {
		try {
			String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
			return (contentType != null && !contentType.isEmpty())
				? contentType
				: "application/octet-stream";
		} catch (Exception ex) {
			// MIME 타입 결정 실패 시 기본 값 반환
			return "application/octet-stream";
		}
	}
}
