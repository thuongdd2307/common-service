package com.example.commonserviceofficial.security;

import lombok.Data;

import java.util.List;

@Data
public class JwtClaims {
    private String username;
    private List<String> roleCodes;
}