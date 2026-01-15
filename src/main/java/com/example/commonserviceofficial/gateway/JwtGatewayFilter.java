package com.example.commonserviceofficial.gateway;

import com.example.commonserviceofficial.contract.HeaderConstant;
import com.example.commonserviceofficial.properties.JwtProperties;
import com.example.commonserviceofficial.security.JwtClaims;
import com.example.commonserviceofficial.security.JwtTokenProvider;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

public class JwtGatewayFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider tokenProvider;
    private final JwtProperties jwtProperties;

    public JwtGatewayFilter(JwtTokenProvider tokenProvider, JwtProperties jwtProperties) {
        this.tokenProvider = tokenProvider;
        this.jwtProperties = jwtProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange,
                             GatewayFilterChain chain) {

        if (!jwtProperties.isEnabled()) {
            return chain.filter(exchange);
        }

        String authHeader =
                exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            try {
                // Validate token first
                if (!tokenProvider.validateToken(token)) {
                    return chain.filter(exchange);
                }
                
                JwtClaims claims = tokenProvider.parseToken(token);
                
                if (claims == null || claims.getUsername() == null) {
                    return chain.filter(exchange);
                }

                ServerHttpRequest mutatedRequest = exchange.getRequest().mutate()
                        .header(HeaderConstant.USER_ID, claims.getUsername())
                        .header(HeaderConstant.USERNAME, claims.getUsername())
                        .header(HeaderConstant.ROLE_CODES, String.join(",", claims.getRoleCodes()))
                        .build();

                ServerWebExchange mutatedExchange = exchange.mutate()
                        .request(mutatedRequest)
                        .build();

                return chain.filter(mutatedExchange);
            } catch (io.jsonwebtoken.security.SecurityException |
                     io.jsonwebtoken.MalformedJwtException |
                     io.jsonwebtoken.ExpiredJwtException |
                     io.jsonwebtoken.UnsupportedJwtException |
                     IllegalArgumentException e) {
                // Log the error and continue without authentication
                return chain.filter(exchange);
            } catch (Exception e) {
                // Log unexpected errors and continue without authentication
                return chain.filter(exchange);
            }
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return -1;
    }
}
