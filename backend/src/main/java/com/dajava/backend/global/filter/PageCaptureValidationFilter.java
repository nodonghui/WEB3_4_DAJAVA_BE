package com.dajava.backend.global.filter;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.dajava.backend.domain.register.service.RegisterCacheService;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;

@Component
@Order(2)
public class PageCaptureValidationFilter implements Filter {

	private final RegisterCacheService registerCacheService;

	public PageCaptureValidationFilter(RegisterCacheService registerCacheService) {
		this.registerCacheService = registerCacheService;
	}

	@Override
	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
		throws IOException, ServletException {

		HttpServletRequest httpRequest = (HttpServletRequest) request;
		HttpServletResponse httpResponse = (HttpServletResponse) response;

		if (!"/v1/register/page-capture".equals(httpRequest.getRequestURI())) {
			chain.doFilter(request, response);
			return;
		}

		String contentType = httpRequest.getContentType();
		String serial = null;

		if (contentType != null && contentType.toLowerCase().contains("multipart/form-data")) {
			try {
				for (Part part : httpRequest.getParts()) {
					if ("serialNumber".equals(part.getName())) {
						try (InputStream is = part.getInputStream()) {
							serial = IOUtils.toString(is, StandardCharsets.UTF_8).trim();
						}
						break;
					}
				}
			} catch (Exception e) {
				httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
				httpResponse.getWriter().write("multipart 파싱 실패");
				return;
			}
		} else {
			CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
			serial = cachedRequest.getParameter("serialNumber");
		}

		if (serial == null || !registerCacheService.isValidSerialNumber(serial)) {
			httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
			httpResponse.getWriter().write("유효하지 않은 serialNumber 입니다.");
			return;
		}

		chain.doFilter(request, response);
	}
}
