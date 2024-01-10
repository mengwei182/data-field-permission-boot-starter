package org.example;

import org.example.aop.DataPermissionAop;
import org.example.properties.DataInformationProperties;
import org.example.start.DataPermissionStarter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author lihui
 */
@Configuration
public class DataPermissionAutoConfiguration {
    @Bean
    public DataInformationProperties dataInformationProperties() {
        return new DataInformationProperties();
    }

    @Bean
    public DataPermissionStarter dataPermissionStarter() {
        return new DataPermissionStarter();
    }

    @Bean
    public DataPermissionAop dataPermissionAop() {
        return new DataPermissionAop();
    }
}