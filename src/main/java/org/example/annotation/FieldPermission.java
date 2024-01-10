package org.example.annotation;

import java.lang.annotation.*;

/**
 * 数据字段注解，代表数据中的一个列
 *
 * @author lihui
 */
@Documented
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface FieldPermission {
    /**
     * 字段名称
     *
     * @return
     */
    String value();
}