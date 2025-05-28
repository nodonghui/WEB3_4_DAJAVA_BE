package com.dajava.backend.global.sentry;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import io.sentry.SentryLevel;

// // 커스텀 어노테이션
// @Retention(RetentionPolicy.RUNTIME)
// @Target(ElementType.METHOD)
// public @interface SentryMonitored {
//
// 	SentryLevel level() default SentryLevel.ERROR;
// 	String operation() default "";
// }
