package com.example.commonserviceofficial.logging;

import org.slf4j.MDC;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "common.logging.trace", name = "enabled", havingValue = "true", matchIfMissing = true)
public class TraceIdFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String traceId = UUID.randomUUID().toString();
        MDC.put("traceId", traceId);
        
        ServerHttpResponse response = exchange.getResponse();
        response.getHeaders().add("X-Trace-Id", traceId);

        return chain.filter(exchange)
                .doFinally(signalType -> MDC.clear());
    }
}
