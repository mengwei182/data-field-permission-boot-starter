package org.example.annotation;

import java.lang.annotation.*;

/**
 * 数据注解，表示一个数据实体
 *
 * @author lihui
 */
@Documented
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataPermission {
    /**
     * 数据名称，多个相同名称会被整合为一个实例
     *
     * @return
     */
    String value();
}