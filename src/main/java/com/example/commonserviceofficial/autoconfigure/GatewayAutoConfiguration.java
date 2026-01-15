package com.example.commonserviceofficial.autoconfigure;

import com.example.commonserviceofficial.gateway.JwtGatewayFilter;
import com.example.commonserviceofficial.properties.JwtProperties;
import com.example.commonserviceofficial.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnClass(GlobalFilter.class)
@ConditionalOnProperty(prefix = "gateway.security", name = "enabled", havingValue = "true", matchIfMissing = false)
@RequiredArgsConstructor
public class GatewayAutoConfiguration {

    private final JwtProperties jwtProperties;
    private final JwtTokenProvider jwtTokenProvider;

    @Bean
    public JwtGatewayFilter jwtGatewayFilter() {
        return new JwtGatewayFilter(jwtTokenProvider, jwtProperties);
    }
}