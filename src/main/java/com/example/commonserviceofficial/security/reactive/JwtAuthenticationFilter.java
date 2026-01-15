package com.example.commonserviceofficial.security.reactive;

import com.example.commonserviceofficial.security.JwtClaims;
import com.example.commonserviceofficial.security.JwtTokenProvider;
import com.example.commonserviceofficial.security.RoleAuthorityMapper;
import com.example.commonserviceofficial.security.jwt.JwtConstants;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * JWT Authentication Filter for Reactive (WebFlux) applications
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter implements WebFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

        if (authHeader != null && authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
            String token = authHeader.substring(7);
            
            try {
                // Validate token first
                if (!tokenProvider.validateToken(token)) {
                    return chain.filter(exchange);
                }
                
                JwtClaims claims = tokenProvider.parseToken(token);
                
                if (claims != null && claims.getUsername() != null) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(
                                    claims.getUsername(),
                                    null,
                                    RoleAuthorityMapper.map(claims.getRoleCodes())
                            );

                    SecurityContext context = new SecurityContextImpl(authentication);
                    return chain.filter(exchange)
                            .contextWrite(contextPut -> contextPut.put(SecurityContext.class, context));
                }
            } catch (Exception ex) {
                // Log the exception but continue the chain
                // In a reactive filter, we don't send error responses directly
                // The security framework will handle unauthorized access
            }
        }

        return chain.filter(exchange);
    }
}
