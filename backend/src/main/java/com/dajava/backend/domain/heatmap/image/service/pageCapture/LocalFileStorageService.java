package com.dajava.backend.domain.heatmap.image.service.pageCapture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dajava.backend.domain.heatmap.image.dto.ImageDimensions;
import com.dajava.backend.domain.heatmap.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.heatmap.image.exception.ImageException;
import com.dajava.backend.global.exception.ErrorCode;

/**
 * 캡쳐 이미지를 생성하거나, 기존에 있는 이미지에 덮어쓰는 로직입니다.
 * 기존에 있는 이미지에 덮어쓰는 경우, 동일한 파일명으로 REPLACE 됩니다.
 * @author Metronon
 */
@Service
@Profile("!prod")
public class LocalFileStorageService implements FileStorageService {

	// 파일 저장 경로 (외부 설정에서 주입)
	private final Path fileStorageLocation;

	public LocalFileStorageService(@Value("${image.path}") String storagePath) {
		this.fileStorageLocation = Paths.get(storagePath).toAbsolutePath().normalize();
		try {
			Files.createDirectories(this.fileStorageLocation);
		} catch (IOException ex) {
			throw new RuntimeException("디렉토리를 생성하지 못했습니다: " + this.fileStorageLocation, ex);
		}
	}

	// 컨트롤러에서 사용하기 위해 파일 저장 위치를 노출하는 getter
	public Path getFileStorageLocation() {
		return this.fileStorageLocation;
	}

	/**
	 * 주어진 파일 이름과 HttpServletRequest 를 사용하여 파일 시스템에 저장된 이미지를
	 * Resource 형태로 로드합니다.
	 *
	 * @param fileName 이미지 파일 이름 (예: UUID 기반의 파일명 + 확장자)
	 * @return Resource 형태의 이미지 파일
	 */
	public Resource getImage(String fileName) {
		try {
			// 파일 저장 경로에서 파일명을 사용해 절대 파일 경로 계산
			Path filePath = this.fileStorageLocation.resolve(fileName).normalize();

			// 보안 체크: 계산된 경로가 fileStorageLocation 하위에 있는지 확인
			if (!filePath.startsWith(this.fileStorageLocation)) {
				throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.");
			}

			Resource resource = new UrlResource(filePath.toUri());

			if (!resource.exists() || !resource.isReadable()) {
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다.");
			}

			return resource;
		} catch (MalformedURLException ex) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "잘못된 파일 경로입니다.", ex);
		}
	}

	/**
	 * 주어진 파일 이름으로 이미지의 높이와 너비를 추출하는 메서드
	 * @param fileName 이미지 파일 이름 (예: UUID 기반의 파일명 + 확장자)
	 * @return 이미지의 높이와 너비를 담은 DTO 객체
	 */
	public ImageDimensions getImageDimensions(String fileName) {
		try {
			// 이미지 이상을 대비해 width, height 기본값 설정
			int width = 0;
			int height = 0;

			// 기존 getImage() 메서드 호출
			Resource imageResource = getImage(fileName);

			// 이미지 스트림을 BufferedImage 로 변환
			BufferedImage image = ImageIO.read(imageResource.getInputStream());

			// 불러온 이미지에 이상이 없다면 높이와 너비 추출
			if (image != null) {
				width = image.getWidth();
				height = image.getHeight();
			}

			return new ImageDimensions(width, height);
		} catch (IOException e) {
			throw new ImageException(ErrorCode.IMAGE_IO_ERROR);
		}
	}

	/**
	 * Base64 인코딩된 이미지를 디코딩하여 파일로 저장하는 공통 메서드
	 * @param base64Image Base64로 인코딩된 이미지 데이터
	 * @param fileName 저장할 파일명
	 * @return ImageSaveResponse 사진의 너비 시작 범위, 저장 이름
	 */
	public ImageSaveResponse saveBase64ImageToFile(String base64Image, String fileName) {
		try {
			// data:image/jpeg;base64, 형식 처리
			if (base64Image.contains(",")) {
				base64Image = base64Image.substring(base64Image.indexOf(",") + 1);
			}

			// Base64 디코딩
			byte[] imageData = java.util.Base64.getDecoder().decode(base64Image);

			ByteArrayInputStream bais = new ByteArrayInputStream(imageData);
			BufferedImage image = ImageIO.read(bais);

			int width = 0;
			int widthRange = 0;
			if (image != null) {
				width = image.getWidth();
				widthRange = (width / 100) * 100;
			}

			// 파일 저장
			Path targetLocation = this.fileStorageLocation.resolve(fileName).normalize();
			if (!targetLocation.startsWith(this.fileStorageLocation)) {
				throw new RuntimeException("잘못된 파일 경로입니다: " + fileName);
			}

			Files.write(targetLocation, imageData);

			return new ImageSaveResponse(widthRange, fileName);

		} catch (IOException ex) {
			throw new RuntimeException("Base64 이미지 저장에 실패했습니다: " + fileName, ex);
		}
	}
}



