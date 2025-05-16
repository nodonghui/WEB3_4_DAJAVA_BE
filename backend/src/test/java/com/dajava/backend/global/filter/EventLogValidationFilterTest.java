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

import com.dajava.backend.domain.register.service.RegisterCacheService;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;

@ExtendWith(MockitoExtension.class)
class EventLogValidationFilterTest {

	private EventLogValidationFilter filter;

	@Mock
	private RegisterCacheService registerCacheService;

	@Mock
	private FilterChain filterChain;

	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		filter = new EventLogValidationFilter(registerCacheService);
		response = new MockHttpServletResponse();
	}

	private HttpServletRequest buildRequest(String uri, String body) throws IOException {
		MockHttpServletRequest req = new MockHttpServletRequest();
		req.setMethod("POST");
		req.setRequestURI(uri);
		req.setContentType("application/json");
		req.setContent(body.getBytes(StandardCharsets.UTF_8));
		return req;
	}

	@Test
	@DisplayName("path가 click, movement, scroll 이외면 필터 우회")
	void testBypassPath() throws Exception {
		HttpServletRequest request = buildRequest("/other", "{}");

		filter.doFilter(request, response, filterChain);

		verify(filterChain).doFilter(request, response);
		assertEquals(200, response.getStatus());
	}

	@Test
	@DisplayName("memberSerialNumber 누락 시 400 반환")
	void testMissingMemberSerialNumber() throws Exception {
		HttpServletRequest request = buildRequest("/click", "{\"data\":123}");

		filter.doFilter(request, response, filterChain);

		assertEquals(400, response.getStatus());
		assertTrue(response.getContentAsString().contains("memberSerialNumber"));
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("memberSerialNumber가 유효하지 않으면 401 반환")
	void testInvalidSerialNumber() throws Exception {
		when(registerCacheService.isValidSerialNumber("invalid123")).thenReturn(false);

		HttpServletRequest request = buildRequest("/movement", "{\"memberSerialNumber\":\"invalid123\"}");

		filter.doFilter(request, response, filterChain);

		assertEquals(401, response.getStatus());
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("정상 요청이면 체인 호출됨")
	void testValidRequest() throws Exception {
		when(registerCacheService.isValidSerialNumber("valid123")).thenReturn(true);

		HttpServletRequest request = buildRequest("/scroll", "{\"memberSerialNumber\":\"valid123\"}");

		filter.doFilter(request, response, filterChain);

		assertEquals(200, response.getStatus()); // 체인이 호출되면 상태코드는 실제로 세팅되지 않을 수 있음
		verify(filterChain).doFilter(any(CachedBodyHttpServletRequest.class), eq(response));
	}
}