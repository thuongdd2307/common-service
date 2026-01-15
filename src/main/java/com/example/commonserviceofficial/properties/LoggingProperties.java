package com.example.commonserviceofficial.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "common.logging")
public class LoggingProperties {
    private boolean enabled = true;
    
    @Data
    public static class Request {
        private boolean enabled = true;
        private boolean includeHeaders = true;
        private boolean includeBody = false;
        private int maxBodySize = 2048;
    }
    
    @Data
    public static class Response {
        private boolean enabled = true;
        private boolean includeBody = false;
    }
    
    @Data
    public static class Trace {
        private boolean enabled = true;
        private String headerName = "X-Trace-Id";
    }
    
    private Request request = new Request();
    private Response response = new Response();
    private Trace trace = new Trace();
}