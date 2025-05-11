package com.dajava.backend.domain.image.service.pageCapture;

import org.springframework.beans.factory.annotation.Value;

import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

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
		this.folderName = folderName.endsWith("/") ? folderName : folderName + "/";
		this.cloudFrontDomain = cloudFrontDomain.endsWith("/") ? cloudFrontDomain : cloudFrontDomain + "/";
	}
}
