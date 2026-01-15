package com.example.commonserviceofficial.security.jwt;

public final class JwtConstants {

    public static final String HEADER_AUTHORIZATION = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    public static final String CLAIM_USERNAME = "sub";
    public static final String CLAIM_ROLE_CODES = "role_codes";

    private JwtConstants() {}
}
