package com.dajava.backend.global.sentry;


import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import io.sentry.Sentry;

@Aspect
@Component
public class SentryAspect {

	@AfterThrowing(
		pointcut = "@annotation(sentryMonitored)",
		throwing = "exception")
	public void captureException(JoinPoint joinPoint, Exception exception, SentryMonitored sentryMonitored) {
		Object[] args = joinPoint.getArgs();

		Sentry.withScope(scope -> {

			// Method 이름 설정
			String operation = sentryMonitored.operation().isEmpty()
				? joinPoint.getSignature().getName()
				: sentryMonitored.operation();
			scope.setTag("operation", operation);

			// 사용된 파라미터 저장
			for (Object arg : args) {
				scope.setExtra(arg.getClass().getName(), arg.toString());
			}

			Sentry.setLevel(sentryMonitored.level());
			Sentry.captureException(exception);
		});
	}
}

