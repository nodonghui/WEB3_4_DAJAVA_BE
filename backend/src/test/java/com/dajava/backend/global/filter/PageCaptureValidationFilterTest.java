package com.dajava.backend.global.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockPart;

import com.dajava.backend.domain.register.service.RegisterCacheService;

import jakarta.servlet.FilterChain;

@ExtendWith(MockitoExtension.class)
class PageCaptureValidationFilterTest {

	private PageCaptureValidationFilter filter;

	@Mock
	private RegisterCacheService registerCacheService;

	@Mock
	private FilterChain filterChain;

	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		filter = new PageCaptureValidationFilter(registerCacheService);
		response = new MockHttpServletResponse();
	}

	private MockHttpServletRequest buildRequest(String uri, String method, String contentType, String body) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod(method);
		request.setRequestURI(uri);
		request.setContentType(contentType);
		request.setContent(body.getBytes(StandardCharsets.UTF_8));
		return request;
	}

	@Test
	@DisplayName("URI가 대상이 아니면 필터 우회")
	void testBypassUri() throws Exception {
		MockHttpServletRequest request = buildRequest("/v1/other", "POST", "application/json", "");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertEquals(200, response.getStatus());
	}

	@Test
	@DisplayName("multipart 파싱 실패 시 400 반환")
	void testMultipartParsingFailure() throws Exception {
		MockHttpServletRequest request = spy(buildRequest("/v1/register/page-capture", "POST", "multipart/form-data", ""));
		doThrow(new IOException("파싱 실패")).when(request).getParts(); // multipart 처리 강제 실패

		filter.doFilter(request, response, filterChain);

		assertEquals(400, response.getStatus());
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("multipart serialNumber가 유효하지 않으면 401 반환")
	void testInvalidSerialInMultipart() throws Exception {
		MockPart part = new MockPart("serialNumber", "invalid".getBytes());
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setMethod("POST");
		request.setRequestURI("/v1/register/page-capture");
		request.setContentType("multipart/form-data");
		request.addPart(part);

		when(registerCacheService.isValidSerialNumber("invalid")).thenReturn(false);

		filter.doFilter(request, response, filterChain);

		assertEquals(401, response.getStatus());
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("x-www-form-urlencoded에서 serialNumber 누락 시 401")
	void testMissingSerialInForm() throws Exception {
		MockHttpServletRequest request = buildRequest(
			"/v1/register/page-capture",
			"POST",
			"application/x-www-form-urlencoded",
			"" // no serialNumber
		);

		filter.doFilter(request, response, filterChain);

		assertEquals(401, response.getStatus());
		verify(filterChain, never()).doFilter(any(), any());
	}

}
