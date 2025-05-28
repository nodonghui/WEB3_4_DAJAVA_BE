package com.dajava.backend.domain.register.service;

import static com.dajava.backend.domain.register.converter.RegisterConverter.*;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.dajava.backend.domain.register.email.EmailService;
import com.dajava.backend.domain.heatmap.image.dto.ImageSaveResponse;
import com.dajava.backend.domain.heatmap.image.service.pageCapture.FileStorageService;
import com.dajava.backend.domain.register.RegisterInfo;
import com.dajava.backend.domain.register.converter.RegisterConverter;
import com.dajava.backend.domain.register.dto.pageCapture.PageCaptureRequest;
import com.dajava.backend.domain.register.dto.pageCapture.PageCaptureResponse;
import com.dajava.backend.domain.register.dto.register.RegisterCheckRequest;
import com.dajava.backend.domain.register.dto.register.RegisterCheckResponse;
import com.dajava.backend.domain.register.dto.register.RegisterCreateRequest;
import com.dajava.backend.domain.register.dto.register.RegisterCreateResponse;
import com.dajava.backend.domain.register.dto.register.RegisterDeleteResponse;
import com.dajava.backend.domain.register.dto.register.RegisterModifyRequest;
import com.dajava.backend.domain.register.dto.register.RegisterModifyResponse;
import com.dajava.backend.domain.register.dto.register.RegistersInfoRequest;
import com.dajava.backend.domain.register.dto.register.RegistersInfoResponse;
import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.exception.RegisterException;
import com.dajava.backend.domain.register.implement.RegisterValidator;
import com.dajava.backend.domain.register.repository.RegisterRepository;
import com.dajava.backend.global.exception.ErrorCode;
import com.dajava.backend.utils.PasswordUtils;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * RegisterService
 * 솔루션 관련 비즈니스 로직을 처리하는 클래스
 *
 * @author ChoiHyunSan
 * @since 2025-03-24
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RegisterService {

	private final RegisterRepository registerRepository;
	private final RegisterValidator registerValidator;
	private final FileStorageService localFileStorageService;
	private final RegisterCacheService registerCacheService;
	private final EmailService emailService;

	/**
	 * 서비스 Register 생성 메서드
	 *
	 * @param request RegisterCreateRequest (DTO)
	 * @return RegisterCreateResponse (DTO)
	 */
	@Transactional
	public RegisterCreateResponse createRegister(final RegisterCreateRequest request) {
		RegisterCreateRequest validatedRequest = registerValidator.validateCreateRequest(request);
		return processCreateRegister(validatedRequest);
	}

	//@SentryMonitored(level = SentryLevel.FATAL, operation = "create_register")
	private RegisterCreateResponse processCreateRegister(RegisterCreateRequest validatedRequest) {
		Register newRegister = registerRepository.save(Register.create(validatedRequest));
		log.info("[RegisterService] Register 엔티티를 생성하였습니다. : {} ", newRegister);

		registerCacheService.refreshCacheAll();
		emailService.sendRegisterCreateEmail(
			newRegister.getEmail(),
			newRegister.getUrl(),
			newRegister.getSerialNumber()
		);

		return toRegisterCreateResponse(newRegister);
	}

	/**
	 * Register 수정 메서드
	 * Register 수정 가능 여부를 파악한 후, 수정한다.
	 *
	 * @param request RegisterModifyRequest (DTO)
	 * @param solutionId 대상 솔루션 ID
	 * @return RegisterModifyResponse (DTO)
	 */
	@Transactional
	public RegisterModifyResponse modifySolution(RegisterModifyRequest request, Long solutionId) {

		Register targetSolution = registerValidator.validateModifyRequest(request, solutionId);
		return processModifyRegister(targetSolution, request, solutionId);
	}

	//@SentryMonitored(level = SentryLevel.FATAL, operation = "modify_register")
	private RegisterModifyResponse processModifyRegister(Register targetSolution, RegisterModifyRequest request, Long solutionId) {
		targetSolution.updateEndDate(request.solutionCompleteDate());

		log.info("[RegisterService] Solution endDate 수정 성공, Target Solution : {}, New endDate : {}",
			solutionId, targetSolution.getEndDate());
		return RegisterModifyResponse.create();
	}

	/**
	 * Register 삭제 메서드 (TODO)
	 * **** 현재 스프린트 상 껍데기만 존재 *****
	 *
	 * @param solutionId 대상 솔루션 ID
	 * @return RegisterDeleteResponse (DTO)
	 */
	@Transactional
	public RegisterDeleteResponse deleteSolution(Long solutionId) {
		registerValidator.validateDeleteRequest(solutionId);

		log.info("[RegisterService] Solution endDate 삭제 성공, Target Solution : {} ", solutionId);
		return RegisterDeleteResponse.create();
	}

	/**
	 * Register 리스트 조회 메서드
	 *
	 * @param request RegisterInfoRequest (DTO)
	 * @return RegistersInfoResponse (DTO)
	 */
	@Transactional(readOnly = true)
	public RegistersInfoResponse getRegisterList(RegistersInfoRequest request) {
		registerValidator.validateInfoRequest(request);

		Pageable pageable = PageRequest.of(request.pageNum(), request.pageSize());
		List<RegisterInfo> registerInfos = registerRepository.findAll(pageable).stream()
			.map(RegisterConverter::toRegisterInfo)
			.toList();

		long registersSize = registerRepository.count();
		long totalPages = (long)Math.ceil((double)registersSize / request.pageSize());

		log.info("[RegisterService] Solution 등록 리스트를 조회합니다. PageNum: {}, PageSize: {}, Search Count: {}",
			request.pageNum(), request.pageSize(), registerInfos.size());

		return RegistersInfoResponse.create(registerInfos, registersSize, totalPages, request.pageNum(),
			request.pageSize());
	}

	/**
	 * 페이지 캡쳐 데이터를 업데이트합니다.
	 *
	 * @param request serialNumber, pageUrl, imagefile 을 가진 요청 DTO 입니다.
	 * @return 처리 결과 메시지
	 */
	@Transactional
	public PageCaptureResponse createPageCapture(PageCaptureRequest request) {
		String serialNumber = request.serialNumber();
		String pageUrl = request.pageUrl();
		MultipartFile imageFile = request.imageFile();

		Register register = registerRepository.findBySerialNumber(serialNumber)
			.orElseThrow(() -> new RegisterException(ErrorCode.REGISTER_NOT_FOUND));

		List<PageCaptureData> captureDataList = register.getCaptureData();

		ImageSaveResponse response;

		try {
			// Base64 이미지 처리 및 저장
			String content = new String(imageFile.getBytes(), StandardCharsets.UTF_8);

			if (content.contains(",")) {
				content = content.substring(content.indexOf(",") + 1);
			}

			byte[] imageData = java.util.Base64.getDecoder().decode(content);
			BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageData));
			if (bufferedImage == null) {
				log.warn("[RegisterService] 유효하지 않은 이미지 정보를 요청하였습니다. "
					+ "serialNumber : {} , pageUrl : {}", serialNumber, pageUrl);
				throw new RuntimeException("유효하지 않은 이미지 입니다.");
			}
			int widthRange = (bufferedImage.getWidth() / 100) * 100;

			// widthRange 가 799 이하인 경우 (매우 작은 데스크톱 뷰 및 모바일 뷰) 요청을 튕겨냄
			if (widthRange <= 700) {
				log.warn("[RegisterService] 유효하지 않은 widthRange 입니다. Input width : {}", widthRange);
				throw new RegisterException(ErrorCode.MOBILE_VIEW_NOT_SUPPORTED);
			}

			Optional<PageCaptureData> optionalData = captureDataList.stream()
				.filter(data -> data.getPageUrl().equals(pageUrl)
					&& data.getWidthRange() == widthRange)
				.findFirst();

			if (optionalData.isPresent()) {
				PageCaptureData existingData = optionalData.get();
				response = localFileStorageService.updateBase64Image(content, existingData, imageFile.getOriginalFilename());
			} else {
				response = localFileStorageService.storeBase64Image(content, imageFile.getOriginalFilename());
				PageCaptureData newData = PageCaptureData.builder()
					.pageUrl(pageUrl)
					.captureFileName(response.fileName())
					.widthRange(widthRange)
					.register(register)
					.build();
				captureDataList.add(newData);
			}
		} catch (IOException e) {
			log.error("[RegisterService] 이미지를 처리하는 과정에서 오류가 발생하였습니다. e : {}", e.getMessage());
			throw new RuntimeException("이미지 처리 중 오류가 발생했습니다", e);
		}

		registerRepository.save(register);

		return new PageCaptureResponse(
			true,
			"페이지 캡쳐 데이터가 성공적으로 저장되었습니다.",
			response.fileName()
		);
	}

	/**
	 * 솔루션 시리얼 번호를 통해 조회된 Register의 이벤트 수집 기간을 강제로 만료한다.
	 * @param serialNumber 시리얼 번호
	 */
	@Transactional
	public void expireRegister(String serialNumber) {
		Register register = registerRepository.findBySerialNumber(serialNumber).orElseThrow(
			() -> new RegisterException(ErrorCode.REGISTER_NOT_FOUND));

		log.info("[RegisterService] Register 엔티티를 강제 만료시킵니다. serialNumber : {}", serialNumber);
		register.expire();
	}
	/**
	 * 일련번호와 비밀번호로 신청 내역이 있는지 확인합니다.
	 * @param registerCheckRequest 시리얼 번호
	 */
	@Transactional
	public RegisterCheckResponse getSolutionCheck(RegisterCheckRequest registerCheckRequest) {
		Optional<Register> optionalRegister = registerRepository.findBySerialNumber(registerCheckRequest.serialNumber());

		if (optionalRegister.isEmpty()) {
			log.warn("[RegisterService] 시리얼 번호 '{}' 로 등록된 사용자를 찾을 수 없습니다.", registerCheckRequest.serialNumber());
			return new RegisterCheckResponse(false);
		}

		Register findRegister = optionalRegister.get();

		boolean isValidPassword = PasswordUtils.verifyPassword(
			registerCheckRequest.password(), findRegister.getPassword());

		if (!isValidPassword) {
			log.warn("[RegisterService] 비밀번호 불일치 - 시리얼 번호: '{}'", registerCheckRequest.serialNumber());
		}

		return new RegisterCheckResponse(isValidPassword);
	}

}
