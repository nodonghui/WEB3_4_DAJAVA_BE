package com.dajava.backend.global.filter;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletResponse;

@ExtendWith(MockitoExtension.class)
public class CorsFilterTest {

	private CorsFilter corsFilter;

	@Mock
	private FilterChain filterChain;

	private MockHttpServletRequest request;
	private MockHttpServletResponse response;

	@BeforeEach
	void setUp() {
		corsFilter = new CorsFilter();
		request = new MockHttpServletRequest();
		response = new MockHttpServletResponse();
	}

	@Test
	@DisplayName("OPTIONS 요청은 체인 호출 없이 200 OK 반환")
	void testPreflightRequest() throws ServletException, IOException {
		request.setMethod("OPTIONS");
		request.addHeader("Origin", "http://localhost:3000");

		corsFilter.doFilter(request, response, filterChain);

		assertEquals(HttpServletResponse.SC_OK, response.getStatus());
		verify(filterChain, never()).doFilter(any(), any());
	}

	@Test
	@DisplayName("CORS 헤더가 올바르게 추가되고 필터 체인이 호출됨")
	void testCorsHeadersSetAndChainCalled() throws ServletException, IOException {
		request.setMethod("GET");
		request.addHeader("Origin", "http://example.com");

		corsFilter.doFilter(request, response, filterChain);

		assertEquals("http://example.com", response.getHeader("Access-Control-Allow-Origin"));
		assertEquals("true", response.getHeader("Access-Control-Allow-Credentials"));
		assertEquals("GET,POST,PUT,DELETE,OPTIONS", response.getHeader("Access-Control-Allow-Methods"));
		assertEquals("*", response.getHeader("Access-Control-Allow-Headers"));

		verify(filterChain).doFilter(request, response);
	}

	@Test
	@DisplayName("Origin 헤더가 없을 경우 Access-Control-Allow-Origin은 *로 설정됨")
	void testOriginHeaderAbsent() throws ServletException, IOException {
		request.setMethod("GET"); // 일반 요청
		// Origin header 없음

		corsFilter.doFilter(request, response, filterChain);

		assertEquals("*", response.getHeader("Access-Control-Allow-Origin"));
	}
}
