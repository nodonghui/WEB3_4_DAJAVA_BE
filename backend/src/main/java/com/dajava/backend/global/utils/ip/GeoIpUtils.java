package com.dajava.backend.global.utils.ip;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

import org.json.JSONObject;

public class GeoIpUtils {

	public static String getCountryCode(String ip) {
		try {
			URL url = new URL("https://ipwho.is/" + ip);
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(1000);
			conn.setReadTimeout(1000);

			try (InputStream is = conn.getInputStream();
				 Scanner scanner = new Scanner(is)) {
				String json = scanner.useDelimiter("\\A").next();
				JSONObject obj = new JSONObject(json);
				if (obj.getBoolean("success")) {
					return obj.getString("country_code"); // ex: "KR"
				}
			}
		} catch (Exception e) {
			// 실패 시 기본값 또는 로그만 남기고 허용 처리 가능
		}
		return null; // 실패 시 처리 전략 필요
	}
}