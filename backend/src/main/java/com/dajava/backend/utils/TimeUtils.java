package com.dajava.backend.utils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TimeUtils {
	/**
	 * 두 LocalDateTime 사이의 시간 차이를 시간(hours) 단위의 int로 반환
	 *
	 * @param startTime 시작 시간
	 * @param endTime 끝 시간
	 * @return 두 시간 사이의 차이(시간 단위)
	 */
	public static int getDuration(LocalDateTime startTime, LocalDateTime endTime) {
		return (int)ChronoUnit.HOURS.between(startTime, endTime);
	}

	/**
	 * Epoch millisecond (Long) 값을 LocalDateTime으로 변환
	 *
	 * @param epochMillis 밀리초 단위의 timestamp
	 * @return LocalDateTime (UTC 기준)
	 */
	public static LocalDateTime toLocalDateTime(Long epochMillis) {
		if (epochMillis == null) {
			return null;
		}
		return LocalDateTime.ofEpochSecond(
			epochMillis / 1000,
			(int) (epochMillis % 1000) * 1_000_000,
			ZoneOffset.UTC
		);
	}

	/**
	 * 주어진 업데이트 시간들 중 가장 최근 값을 반환합니다.
	 * 모든 값이 null인 경우 null을 반환합니다.
	 */
	public static Long getLatestUpdate(Long... updates) {
		Long latest = null;

		for (Long update : updates) {
			if (update != null && (latest == null || update > latest)) {
				log.trace("업데이트 비교: 기존 = {}, 새로운 = {}", latest, update);
				latest = update;
			}
		}

		return latest;
	}

	/**
	 * LocalDateTime을 epoch milliseconds(Long)로 변환
	 *
	 * @param localDateTime 변환할 LocalDateTime
	 * @return epoch millisecond
	 */
	public static Long toEpochMillis(LocalDateTime localDateTime) {
		if (localDateTime == null) {
			return null;
		}
		return localDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
	}
}
