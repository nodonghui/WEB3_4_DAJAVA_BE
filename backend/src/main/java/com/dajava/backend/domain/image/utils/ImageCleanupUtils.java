package com.dajava.backend.domain.image.utils;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.dajava.backend.domain.register.entity.PageCaptureData;
import com.dajava.backend.domain.register.entity.Register;
import com.dajava.backend.domain.register.repository.PageCaptureDataRepository;
import com.dajava.backend.domain.register.repository.RegisterRepository;

public class ImageCleanupUtils {

	private static RegisterRepository registerRepository;
	private static PageCaptureDataRepository pageCaptureDataRepository;

	public static Set<String> getRegisterUrls() {
		List<Register> registers = registerRepository.findAll();

		return registers.stream()
			.map(Register::getUrl)
			.collect(Collectors.toSet());
	}

	public static Set<String> getPageCaptureUrls() {
		List<PageCaptureData> pageCaptureData = pageCaptureDataRepository.findAll();

		return pageCaptureData.stream()
			.map(PageCaptureData::getPageUrl)
			.collect(Collectors.toSet());
	}
}
