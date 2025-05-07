package com.dajava.backend.domain.image.service.pageCapture;

import org.springframework.core.io.Resource;

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
}
