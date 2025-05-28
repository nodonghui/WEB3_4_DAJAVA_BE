package com.dajava.backend.global.sentry;

import io.sentry.Sentry;
import io.sentry.SentryLevel;

// public class SentryUtil {
//
// 	/**
// 	 * 예외를 Sentry에 기록
// 	 * @param e 예외 객체
// 	 * @param component 컴포넌트 이름 (태그로 사용)
// 	 * @param level 심각도 레벨
// 	 */
// 	public static void captureException(Exception e, String component, SentryLevel level) {
// 		Sentry.withScope(scope -> {
// 			scope.setTag("component", component);
//
// 			scope.setLevel(level);
// 			Sentry.captureException(e);
// 		});
// 	}
//
// 	/**
// 	 * FATAL 레벨 예외 - 즉시 알림
// 	 */
// 	public static void captureFatal(Exception e, String component) {
// 		captureException(e, component, SentryLevel.FATAL);
// 	}
//
// 	/**
// 	 * ERROR 레벨 예외 - 높은 우선순위
// 	 */
// 	public static void captureError(Exception e, String component) {
// 		captureException(e, component, SentryLevel.ERROR);
// 	}
//
// 	/**
// 	 * WARNING 레벨 예외 - 주의 필요
// 	 */
// 	public static void captureWarning(Exception e, String component) {
// 		captureException(e, component, SentryLevel.WARNING);
// 	}
//
// 	public static void captureMessage(String component, String message, SentryLevel level) {
// 		Sentry.withScope(scope -> {
// 			scope.setTag("component", component);
//
// 			scope.setLevel(level);
// 			Sentry.captureMessage(message);
// 		});
// 	}
//
// 	/**
// 	 * FATAL 레벨 예외 - 즉시 알림
// 	 */
// 	public static void messageFatal(String message, String component) {
// 		captureMessage(message, component, SentryLevel.FATAL);
// 	}
//
// 	/**
// 	 * ERROR 레벨 예외 - 높은 우선순위
// 	 */
// 	public static void messageError(String message, String component) {
// 		captureMessage(message, component, SentryLevel.ERROR);
// 	}
//
// 	/**
// 	 * WARNING 레벨 예외 - 주의 필요
// 	 */
// 	public static void messageWarning(String message, String component) {
// 		captureMessage(message, component, SentryLevel.WARNING);
// 	}
// }
