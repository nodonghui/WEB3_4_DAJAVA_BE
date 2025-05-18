package com.dajava.backend.global.utils.ip;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtils {

	private IpUtils() {
	}

	/**
	 * 프록시 환경에서 클라이언트의 실제 IP를 추출합니다.
	 * Nginx 또는 프록시 서버가 X-Forwarded-For 헤더를 설정한다고 가정합니다.
	 * 여러 IP가 있을 경우 가장 앞에 있는 IP가 원래 클라이언트입니다.
	 */
	public static String getClientIp(HttpServletRequest request) {
		String header = request.getHeader("X-Forwarded-For");

		if (header != null && !header.isEmpty() && !"unknown".equalsIgnoreCase(header)) {
			// 여러 프록시를 거친 경우: "원래IP, 중간프록시, 최종프록시"
			return header.split(",")[0].trim();
		}

		// 프록시 없이 직접 접근한 경우
		return request.getRemoteAddr();
	}

	public static boolean isPrivateIp(String ip) {
		return ip.startsWith("10.")||
			ip.startsWith("192.168.") ||
			ip.startsWith("172.16.") || ip.startsWith("172.17.") ||
			ip.startsWith("172.18.") || ip.startsWith("172.19.") ||
			ip.startsWith("172.20.") || ip.startsWith("172.21.") ||
			ip.startsWith("172.22.") || ip.startsWith("172.23.") ||
			ip.startsWith("172.24.") || ip.startsWith("172.25.") ||
			ip.startsWith("172.26.") || ip.startsWith("172.27.") ||
			ip.startsWith("172.28.") || ip.startsWith("172.29.") ||
			ip.startsWith("172.30.") || ip.startsWith("172.31.") ||
			ip.equals("127.0.0.1") || ip.equals("::1");
	}
}
