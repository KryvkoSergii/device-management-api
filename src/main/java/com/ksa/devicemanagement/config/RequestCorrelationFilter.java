package com.ksa.devicemanagement.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestCorrelationFilter extends OncePerRequestFilter {

    static final String REQUEST_ID_HEADER = "X-Request-ID";
    static final String TRACE_PARENT_HEADER = "traceparent";

    private static final Pattern REQUEST_ID_PATTERN = Pattern.compile("[A-Za-z0-9._:-]{1,128}");
    private static final Pattern TRACE_PARENT_PATTERN = Pattern.compile(
            "(?i)^[0-9a-f]{2}-([0-9a-f]{32})-[0-9a-f]{16}-[0-9a-f]{2}(?:-.*)?$");
    private static final String INVALID_TRACE_ID = "00000000000000000000000000000000";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String requestId = requestId(request.getHeader(REQUEST_ID_HEADER));
        String traceId = traceId(request.getHeader(TRACE_PARENT_HEADER));

        response.setHeader(REQUEST_ID_HEADER, requestId);
        try (MDC.MDCCloseable ignoredRequestId = MDC.putCloseable("requestId", requestId);
             MDC.MDCCloseable ignoredTraceId = MDC.putCloseable("traceId", traceId)) {
            filterChain.doFilter(request, response);
        }
    }

    private String requestId(String candidate) {
        return candidate != null && REQUEST_ID_PATTERN.matcher(candidate).matches()
                ? candidate
                : UUID.randomUUID().toString();
    }

    private String traceId(String traceParent) {
        if (traceParent != null) {
            Matcher matcher = TRACE_PARENT_PATTERN.matcher(traceParent);
            if (matcher.matches() && !INVALID_TRACE_ID.equals(matcher.group(1))) {
                return matcher.group(1).toLowerCase(Locale.ROOT);
            }
        }
        return UUID.randomUUID().toString().replace("-", "");
    }
}
