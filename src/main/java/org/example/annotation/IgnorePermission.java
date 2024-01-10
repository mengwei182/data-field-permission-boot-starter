package org.example.annotation;

import java.lang.annotation.*;

/**
 * 注解该配置的方法或类会在AOP中被忽略
 *
 * @author lihui
 * @since 2023/8/8
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface IgnorePermission {
}