package com.miproject.finalwork.handler.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
// 为不同的类打上标签，标注其属于的类型：voltage等
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface AlarmType {
    String value();
}
