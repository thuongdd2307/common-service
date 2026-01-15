package com.example.commonserviceofficial.autoconfigure;

import com.example.commonserviceofficial.logging.filter.RequestLoggingFilter;
import com.example.commonserviceofficial.properties.JwtProperties;
import com.example.commonserviceofficial.properties.LoggingProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        JwtProperties.class,
        LoggingProperties.class
})
public class CommonAutoConfiguration {

    @Bean
    @ConditionalOnProperty(prefix = "common.logging.request", name = "enabled", havingValue = "true", matchIfMissing = true)
    RequestLoggingFilter requestLoggingFilter() {
        return new RequestLoggingFilter();
    }
}
