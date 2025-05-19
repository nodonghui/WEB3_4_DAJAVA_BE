package com.dajava.backend.domain.image.service.pageCapture;

import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import com.dajava.backend.domain.image.utils.ImageCleanupUtils;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.repository.PageCaptureDataRepository;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@Slf4j
@Profile("prod")
@Service
public class S3FileCleanupService implements FileCleanupService {
	private final S3Client s3Client;
	private final String bucketName;
	private final String folderName;
	private final PageCaptureDataRepository pageCaptureDataRepository;
	private final ImageCleanupUtils imageCleanupUtils;

	public S3FileCleanupService(
		@Value("${aws.region}") String region,
		@Value("${aws.s3.bucket-name}") String bucketName,
		@Value("${aws.s3.folder}") String folderName,
		PageCaptureDataRepository pageCaptureDataRepository, ImageCleanupUtils imageCleanupUtils) {

		this.s3Client = S3Client.builder()
			.region(Region.of(region))
			.build();
		this.bucketName = bucketName;
		this.folderName = folderName;
		this.pageCaptureDataRepository = pageCaptureDataRepository;
		this.imageCleanupUtils = imageCleanupUtils;
	}

	// S3 Key 생성
	private String getS3Key(String fileName) {
		return folderName + fileName;
	}

	/**
	 * 지정된 파일명을 사용하여 S3 버킷에서 파일을 삭제합니다.
	 *
	 * @param fileName 삭제할 파일의 이름 (UUID 기반 파일명 + 확장자)
	 */
	@Override
	public void deleteFile(String fileName) {
		try {
			// 파일 존재 여부 확인 (선택 사항)
			if (fileExists(fileName)) {
				DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
					.bucket(bucketName)
					.key(getS3Key(fileName))
					.build();

				s3Client.deleteObject(deleteObjectRequest);
				log.info("S3 에서 이미지를 성공적으로 삭제하였습니다: {}", getS3Key(fileName));
			} else {
				log.warn("S3 에 존재하지 않는 이미지 파일: {}", getS3Key(fileName));
			}
		} catch (Exception ex) {
			log.error("S3 에 요청 중 오류가 발생했습니다: {}", fileName, ex);
			throw new RuntimeException("파일 삭제에 실패했습니다: " + fileName, ex);
		}
	}

	/**
	 * 현재 Register에 등록되있지 않은 URL과 연관된 이미지 파일을 지웁니다.
	 * 해당 로직은 서버 시작시 최초 1회 기동합니다. (추후 스케줄링할 여지 있음)
	 */
	@Override
	public void deleteNonLinkedFile() {
		log.info("연관되지 않은 모든 S3 이미지 파일 삭제 시작");

		// Register에 있는 모든 URL Set
		Set<String> registerUrls = imageCleanupUtils.getRegisterUrls();

		// PageCaptureData에 있는 모든 URL Set
		Set<String> pageCaptureUrls = imageCleanupUtils.getPageCaptureUrls();

		// 제거 대상인 url의 정보만 남김
		pageCaptureUrls.removeAll(registerUrls);

		int deletedCount = 0;
		for (String pageCaptureUrl : pageCaptureUrls) {
			PageCaptureData targetData = pageCaptureDataRepository.findByPageUrl(pageCaptureUrl);
			if (targetData != null && targetData.getCaptureFileName() != null) {
				String fileName = targetData.getCaptureFileName();
				try {
					deleteFile(fileName);
					deletedCount++;
				} catch (Exception e) {
					log.error("Error deleting file {} for URL {}", fileName, pageCaptureUrl, e);
				}
			}
		}

		log.info("S3에서 연관되지 않은 파일 {} 개 삭제 완료", deletedCount);
	}

	// 파일 존재 여부 확인 메서드
	private boolean fileExists(String fileName) {
		try {
			HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
				.bucket(bucketName)
				.key(getS3Key(fileName))
				.build();

			s3Client.headObject(headObjectRequest);
			return true;
		} catch (NoSuchKeyException e) {
			return false;
		}
	}
}
