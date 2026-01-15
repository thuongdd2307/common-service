package com.example.commonserviceofficial.logging.util;

import com.example.commonserviceofficial.contract.HeaderConstant;
import org.slf4j.MDC;

import java.util.UUID;

public final class TraceIdUtil {

    public static String getOrCreate() {
        String traceId = MDC.get(HeaderConstant.TRACE_ID);
        if (traceId == null) {
            traceId = UUID.randomUUID().toString();
            MDC.put(HeaderConstant.TRACE_ID, traceId);
        }
        return traceId;
    }

    private TraceIdUtil() {}
}