package com.example.commonserviceofficial.contract;

import com.example.commonserviceofficial.logging.util.TraceIdUtil;

/**
 * Base Response Generic
 * @param traceId
 * @param data
 * @param errorCode
 * @param message
 * @param <T>
 */
public record BaseResponse<T>(
        String traceId,
        T data,
        String errorCode,
        String message
) {
    public static <T> BaseResponse<T> ok(String traceId, T data) {
        return new BaseResponse<>(traceId, data, null, "SUCCESS");
    }
    
    public static <T> BaseResponse<T> ok(T data) {
        return ok(TraceIdUtil.getOrCreate(), data);
    }
    
    public static <T> BaseResponse<T> ok() {
        return ok(TraceIdUtil.getOrCreate(), null);
    }
    
    public static <T> BaseResponse<T> error(String traceId, String errorCode, String message) {
        return new BaseResponse<>(traceId, null, errorCode, message);
    }
    
    public static <T> BaseResponse<T> error(String errorCode, String message) {
        return error(TraceIdUtil.getOrCreate(), errorCode, message);
    }
    
    public static <T> BaseResponse<T> error(String message) {
        return error("BUSINESS_ERROR", message);
    }
    
    public boolean isSuccess() {
        return errorCode == null;
    }
    
    public boolean isError() {
        return !isSuccess();
    }
}