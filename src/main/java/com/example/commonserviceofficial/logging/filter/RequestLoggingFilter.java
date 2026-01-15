package com.example.commonserviceofficial.logging.filter;

import com.example.commonserviceofficial.contract.HeaderConstant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
@ConditionalOnProperty(prefix = "common.logging.request", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RequestLoggingFilter implements WebFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        
        String traceId = request.getHeaders().getFirst(HeaderConstant.TRACE_ID);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
        }

        final long start = System.currentTimeMillis();
        final String finalTraceId = traceId;
        
        return chain.filter(exchange)
                .doFinally(signalType -> {
                    long cost = System.currentTimeMillis() - start;
                    logger.info(
                            "[TRACE={}] {} {} → {} ({}ms)",
                            finalTraceId,
                            request.getMethod(),
                            request.getURI(),
                            exchange.getResponse().getStatusCode(),
                            cost
                    );
                });
    }
}
