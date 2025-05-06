package com.dajava.backend.global.log;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


// 메서드나 클래스에 붙입니다.
@Target({ElementType.METHOD, ElementType.TYPE})
// 런타임에 유지
@Retention(RetentionPolicy.RUNTIME)
public @interface Loggable {
	LogLevel level() default LogLevel.DEBUG;
	LogType type() default LogType.DEFAULT;
}
