package com.lab.paperarchive.config;

import nz.net.ultraq.thymeleaf.layoutdialect.LayoutDialect;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(AppProperties.class)
public class AppConfig {

    /** Thymeleaf 레이아웃 상속 (layout:decorate) 활성화 */
    @Bean
    public LayoutDialect layoutDialect() {
        return new LayoutDialect();
    }
}
