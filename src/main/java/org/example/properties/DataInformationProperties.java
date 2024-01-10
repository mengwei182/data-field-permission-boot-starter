package org.example.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 动态数据权限配置类
 *
 * @author lihui
 */
@Data
@ConfigurationProperties(prefix = "data.permission")
public class DataInformationProperties {
    private boolean enable;
    /**
     * 指定被校验的类名
     */
    private String targetClass;
    /**
     * 指定被校验的类的成员变量，该变量存储被权限校验的数据，校验完成后会再次赋值给该参数，替换原有数据完成权限校验
     */
    private String targetField;
    /**
     * 扫描包路径
     */
    private String scanBasePackages;
}