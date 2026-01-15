package com.example.commonserviceofficial.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

public final class RoleAuthorityMapper {


    private RoleAuthorityMapper() {}

    public static List<GrantedAuthority> map(List<String> roleCodes) {
        return roleCodes.stream()
                .map(code -> (GrantedAuthority)
                        new SimpleGrantedAuthority("ROLE_" + code))
                .toList();
    }
}