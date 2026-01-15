package com.example.commonserviceofficial.security.servlet;

import com.example.commonserviceofficial.security.JwtClaims;
import com.example.commonserviceofficial.security.JwtTokenProvider;
import com.example.commonserviceofficial.security.RoleAuthorityMapper;
import com.example.commonserviceofficial.security.jwt.JwtConstants;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * JWT Authentication Filter for Servlet-based (Spring MVC) applications
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider tokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);

            if (authHeader != null && authHeader.startsWith(JwtConstants.TOKEN_PREFIX)) {
                String token = authHeader.substring(7);

                if (tokenProvider.validateToken(token)) {
                    JwtClaims claims = tokenProvider.parseToken(token);

                    if (claims != null && claims.getUsername() != null) {
                        UsernamePasswordAuthenticationToken authentication =
                                new UsernamePasswordAuthenticationToken(
                                        claims.getUsername(),
                                        null,
                                        RoleAuthorityMapper.map(claims.getRoleCodes())
                                );

                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    }
                }
            }
        } catch (Exception ex) {
            // Log the exception but continue the chain
            logger.error("Could not set user authentication in security context", ex);
        }

        filterChain.doFilter(request, response);
    }
}
