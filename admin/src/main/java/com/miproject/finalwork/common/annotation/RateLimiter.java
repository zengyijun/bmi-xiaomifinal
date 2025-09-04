package com.miproject.finalwork.common.annotation;

import java.lang.annotation.*;
import java.util.concurrent.TimeUnit;

/**
 * 限流注解
 */
@Inherited
@Documented
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimiter {
    /**
     * 限流key
     */
    String key() default "";

    /**
     * 最大请求数
     */
    int maxRequests() default 100;

    /**
     * 时间窗口（秒）
     */
    int timeWindow() default 60;
    
    /**
     * 时间单位
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;
}