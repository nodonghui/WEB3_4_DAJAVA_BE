package com.dajava.backend.global.filter;

import java.io.IOException;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

// @Component
// @Slf4j
// @Order(Ordered.HIGHEST_PRECEDENCE)
// public class CorsFilter implements Filter {
//
// 	@Override
// 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
// 		throws IOException, ServletException {
//
// 		HttpServletRequest httpRequest = (HttpServletRequest) request;
// 		HttpServletResponse httpResponse = (HttpServletResponse) response;
//
// 		// Preflight 요청은 바로 통과
// 		if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
// 			httpResponse.setStatus(HttpServletResponse.SC_OK);
// 			return;
// 		}
//
// 		// CORS 헤더 설정
// 		String origin = httpRequest.getHeader("Origin");
// 		httpResponse.setHeader("Access-Control-Allow-Origin", origin != null ? origin : "*");
// 		httpResponse.setHeader("Access-Control-Allow-Credentials", "true");
// 		httpResponse.setHeader("Access-Control-Allow-Methods", "GET,POST,PUT,DELETE,OPTIONS");
// 		httpResponse.setHeader("Access-Control-Allow-Headers", "*");
//
//
//
// 		chain.doFilter(request, response);
// 	}
// }