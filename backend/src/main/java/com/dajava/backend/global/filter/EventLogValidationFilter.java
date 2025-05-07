package com.dajava.backend.global.filter;

import java.io.IOException;

import org.json.JSONObject;
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

// @Component
// @Order(1)
// public class EventLogValidationFilter implements Filter {
//
// 	private final RegisterCacheService registerCacheService;
//
// 	public EventLogValidationFilter(RegisterCacheService registerCacheService) {
// 		this.registerCacheService = registerCacheService;
// 	}
//
// 	@Override
// 	public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
// 		throws IOException, ServletException {
//
// 		HttpServletRequest httpRequest = (HttpServletRequest) request;
// 		HttpServletResponse httpResponse = (HttpServletResponse) response;
//
// 		String path = httpRequest.getRequestURI();
// 		if (!(path.endsWith("/click") || path.endsWith("/movement") || path.endsWith("/scroll"))) {
// 			chain.doFilter(request, response);
// 			return;
// 		}
//
// 		CachedBodyHttpServletRequest cachedRequest = new CachedBodyHttpServletRequest(httpRequest);
// 		try {
// 			JSONObject body = new JSONObject(cachedRequest.getBody());
//
// 			if (!body.has("memberSerialNumber")) {
// 				httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
// 				httpResponse.getWriter().write("memberSerialNumber 가 존재하지 않습니다.");
// 				return;
// 			}
//
// 			String serial = body.getString("memberSerialNumber");
// 			if (!registerCacheService.isValidSerialNumber(serial)) {
// 				httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
// 				httpResponse.getWriter().write("유효하지 않은 memberSerialNumber 입니다.");
// 				return;
// 			}
//
// 			chain.doFilter(cachedRequest, response);
// 		} catch (Exception e) {
// 			httpResponse.setStatus(HttpServletResponse.SC_BAD_REQUEST);
// 			httpResponse.getWriter().write("요청 파싱 중 오류가 발생했습니다.");
// 		}
// 	}
// }
