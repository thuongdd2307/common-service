package com.example.commonserviceofficial.contract;

/**
 * Cấu hình thuộc tính header
 */
public final class HeaderConstant {
    
    // Authentication
    public static final String AUTHORIZATION = "Authorization";
    
    // Tracing
    public static final String TRACE_ID = "X-Trace-Id";
    
    // User Information
    public static final String USER_ID = "X-User-Id";
    public static final String USERNAME = "X-Username";
    public static final String ROLE_CODES = "X-Role-Codes";
    
    // Client Information
    public static final String X_FORWARDED_FOR = "X-Forwarded-For";
    public static final String X_REAL_IP = "X-Real-IP";
    public static final String USER_AGENT = "User-Agent";
    
    // Request Information
    public static final String CONTENT_TYPE = "Content-Type";
    public static final String ACCEPT = "Accept";
    
    private HeaderConstant() {
        throw new UnsupportedOperationException("This is a utility class and cannot be instantiated");
    }
}
