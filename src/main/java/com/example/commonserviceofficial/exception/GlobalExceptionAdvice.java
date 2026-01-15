package com.example.commonserviceofficial.exception;

import com.example.commonserviceofficial.contract.BaseResponse;
import com.example.commonserviceofficial.logging.util.TraceIdUtil;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionAdvice {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handle(BusinessException ex) {
        BaseResponse<?> response = new BaseResponse<>(
                TraceIdUtil.getOrCreate(),
                null,
                ex.getErrorCode(),
                ex.getMessage()
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handle(Exception ex) {
        BaseResponse<?> response = new BaseResponse<>(
                TraceIdUtil.getOrCreate(),
                null,
                "INTERNAL_ERROR",
                "Internal server error"
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
