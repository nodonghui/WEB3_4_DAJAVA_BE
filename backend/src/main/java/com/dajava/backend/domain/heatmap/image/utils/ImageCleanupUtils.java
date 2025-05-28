package com.dajava.backend.domain.heatmap.image.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.repository.PageCaptureDataRepository;
import com.dajava.backend.domain.register.repository.RegisterRepository;

@Component
public class ImageCleanupUtils {

	private final RegisterRepository registerRepository;
	private final PageCaptureDataRepository pageCaptureDataRepository;

	public ImageCleanupUtils(RegisterRepository registerRepository,
		PageCaptureDataRepository pageCaptureDataRepository) {
		this.registerRepository = registerRepository;
		this.pageCaptureDataRepository = pageCaptureDataRepository;
	}

	public Set<String> getRegisterUrls() {
		List<Register> registers = registerRepository.findAll();

		return registers.stream()
			.map(Register::getUrl)
			.collect(Collectors.toSet());
	}

	public Set<String> getPageCaptureUrls() {
		List<PageCaptureData> pageCaptureData = pageCaptureDataRepository.findAll();

		return pageCaptureData.stream()
			.map(PageCaptureData::getPageUrl)
			.collect(Collectors.toSet());
	}
}
