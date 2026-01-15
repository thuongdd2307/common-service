package com.example.commonserviceofficial.util;

import com.example.commonserviceofficial.contract.HeaderConstant;
import org.springframework.web.server.ServerWebExchange;

/**
 * Web utilities for reactive applications
 */
public final class WebUtils {
    
    private static final String UNKNOWN = "unknown";
    
    private WebUtils() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
    
    /**
     * Get client IP address from ServerWebExchange
     * 
     * @param exchange ServerWebExchange
     * @return client IP address
     */
    public static String getClientIp(ServerWebExchange exchange) {
        if (exchange == null) {
            return UNKNOWN;
        }
        
        // Try X-Forwarded-For header first
        String ip = exchange.getRequest().getHeaders().getFirst(HeaderConstant.X_FORWARDED_FOR);
        if (isValidIp(ip)) {
            // X-Forwarded-For can contain multiple IPs, get the first one
            return ip.split(",")[0].trim();
        }
        
        // Try X-Real-IP header
        ip = exchange.getRequest().getHeaders().getFirst(HeaderConstant.X_REAL_IP);
        if (isValidIp(ip)) {
            return ip;
        }
        
        // Get from remote address
        if (exchange.getRequest().getRemoteAddress() != null) {
            return exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
        }
        
        return UNKNOWN;
    }
    
    /**
     * Get user agent from ServerWebExchange
     * 
     * @param exchange ServerWebExchange
     * @return user agent string
     */
    public static String getUserAgent(ServerWebExchange exchange) {
        if (exchange == null) {
            return null;
        }
        return exchange.getRequest().getHeaders().getFirst(HeaderConstant.USER_AGENT);
    }
    
    /**
     * Get request URL from ServerWebExchange
     * 
     * @param exchange ServerWebExchange
     * @return request URL
     */
    public static String getRequestUrl(ServerWebExchange exchange) {
        if (exchange == null) {
            return null;
        }
        return exchange.getRequest().getURI().toString();
    }
    
    /**
     * Get request method from ServerWebExchange
     * 
     * @param exchange ServerWebExchange
     * @return HTTP method
     */
    public static String getRequestMethod(ServerWebExchange exchange) {
        if (exchange == null) {
            return null;
        }
        return exchange.getRequest().getMethod().name();
    }
    
    /**
     * Check if IP address is valid
     * 
     * @param ip IP address
     * @return true if valid
     */
    private static boolean isValidIp(String ip) {
        return ip != null && !ip.isEmpty() && !UNKNOWN.equalsIgnoreCase(ip);
    }
}
