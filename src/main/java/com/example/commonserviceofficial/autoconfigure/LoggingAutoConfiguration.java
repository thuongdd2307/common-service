package com.example.commonserviceofficial.autoconfigure;

import com.example.commonserviceofficial.logging.TraceIdFilter;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LoggingAutoConfiguration {

    @Bean
    @ConditionalOnProperty(
            prefix = "common.logging.request",
            name = "enabled",
            havingValue = "true",
            matchIfMissing = true
    )
    public TraceIdFilter traceIdFilter() {
        return new TraceIdFilter();
    }
}