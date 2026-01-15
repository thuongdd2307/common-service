package com.example.commonserviceofficial.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "security.jwt")
public class JwtProperties {
    private boolean enabled = true;
    private String secret;
    private String issuer;
    private long expirationSeconds = 3600;
    private long refreshExpirationSeconds = 86400;
    
    @Data
    public static class Header {
        private String authorization = "Authorization";
        private String prefix = "Bearer";
    }
    
    @Data
    public static class Claim {
        private String userId = "user_id";
        private String username = "username";
        private String roles = "role_codes";
    }
    
    private Header header = new Header();
    private Claim claim = new Claim();
}
