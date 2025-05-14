package com.dajava.backend.domain.image.service.pageCapture;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.imageio.ImageIO;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.dajava.backend.domain.image.dto.ImageDimensions;
import com.dajava.backend.domain.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.image.exception.ImageException;
import com.dajava.backend.global.exception.ErrorCode;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Slf4j
@Profile("prod")
@Service
public class S3FileStorageService implements FileStorageService {

	private final S3Client s3Client;
	private final String bucketName;
	private final String folderName;
	private final String cloudFrontDomain;

	public S3FileStorageService(
		@Value("${aws.region}") String region,
		@Value("${aws.s3.bucket-name}") String bucketName,
		@Value("${aws.s3.folder}") String folderName,
		@Value("${aws.cloudfront.domain}") String cloudFrontDomain) {

		this.s3Client = S3Client.builder()
			.region(Region.of(region)
			).build();
		this.bucketName = bucketName;
		this.folderName = folderName;
		this.cloudFrontDomain = cloudFrontDomain;
	}

	// S3 Key 생성
	private String getS3Key(String fileName) {
		return folderName + fileName;
	}

	// CloudFront 대체 도메인 URL 생성
	private String getCloudFrontKey(String fileName) {
		return cloudFrontDomain + getS3Key(fileName);
	}

	@Override
	public Resource getImage(String fileName) {
		try {
			// S3에서 객체 가져오기
			GetObjectRequest getObjectRequest = GetObjectRequest.builder()
				.bucket(bucketName)
				.key(getS3Key(fileName))
				.build();

			ResponseBytes<GetObjectResponse> objectBytes = s3Client.getObject(
				getObjectRequest, ResponseTransformer.toBytes());

			byte[] bytes = objectBytes.asByteArray();
			return new ByteArrayResource(bytes);

		} catch (NoSuchKeyException ex) {
			throw new ResponseStatusException(HttpStatus.NOT_FOUND, "파일을 찾을 수 없습니다: " + fileName, ex);
		} catch (Exception ex) {
			throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
				"파일을 가져오는 중 오류가 발생했습니다: " + fileName, ex);
		}
	}

	@Override
	public ImageDimensions getImageDimensions(String fileName) {
		try {
			// 이미지 이상을 대비해 width, height 기본값 설정
			int width = 0;
			int height = 0;

			// S3에서 이미지 가져오기
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

	@Override
	public ImageSaveResponse saveBase64ImageToFile(String base64Image, String fileName) {
		try {
			// Mime 타입 기본값 지정
			String contentType = "image/jpeg";

			// base64 처리
			if (base64Image.startsWith("data:")) {
				int semiColonIndex = base64Image.indexOf(";");
				if (semiColonIndex != -1) {
					contentType = base64Image.substring(5, semiColonIndex);
				}
				int commaIndex = base64Image.indexOf(",");
				if (commaIndex != -1) {
					base64Image = base64Image.substring(commaIndex + 1);
				}
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

			// S3에 업로드
			Map<String, String> metadata = new HashMap<>();
			metadata.put("Content-Type", contentType);

			PutObjectRequest putObjectRequest = PutObjectRequest.builder()
				.bucket(bucketName)
				.key(getS3Key(fileName))
				.contentType(contentType)
				.metadata(metadata)
				.build();

			s3Client.putObject(putObjectRequest, RequestBody.fromBytes(imageData));

			log.info("S3 업로드 성공: {}", getS3Key(fileName));

			return new ImageSaveResponse(widthRange, fileName);

		} catch (IOException ex) {
			log.error("S3 업로드 실패", ex);
			throw new RuntimeException("Base64 이미지 저장에 실패했습니다: " + fileName, ex);
		}
	}
}
