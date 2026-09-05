package com.ksa.devicemanagement.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class RequestCorrelationFilterTest {

    private final RequestCorrelationFilter filter = new RequestCorrelationFilter();

    @Test
    @DisplayName("propagates valid request and trace identifiers")
    void propagatesValidRequestAndTraceIdentifiers() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var loggedRequestId = new AtomicReference<String>();
        var loggedTraceId = new AtomicReference<String>();
        request.addHeader("X-Request-ID", "gateway-request-42");
        request.addHeader("traceparent", "00-4bf92f3577b34da6a3ce929d0e0e4736-00f067aa0ba902b7-01");

        filter.doFilter(request, response, (servletRequest, servletResponse) -> {
            loggedRequestId.set(MDC.get("requestId"));
            loggedTraceId.set(MDC.get("traceId"));
        });

        assertThat(response.getHeader("X-Request-ID")).isEqualTo("gateway-request-42");
        assertThat(loggedRequestId).hasValue("gateway-request-42");
        assertThat(loggedTraceId).hasValue("4bf92f3577b34da6a3ce929d0e0e4736");
        assertThat(MDC.get("requestId")).isNull();
        assertThat(MDC.get("traceId")).isNull();
    }

    @Test
    @DisplayName("replaces unsafe or missing identifiers")
    void replacesUnsafeOrMissingIdentifiers() throws Exception {
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var generatedTraceId = new AtomicReference<String>();
        request.addHeader("X-Request-ID", "invalid request id\nforged-log-entry");

        filter.doFilter(request, response, (servletRequest, servletResponse) ->
                generatedTraceId.set(MDC.get("traceId")));

        assertThat(response.getHeader("X-Request-ID"))
                .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        assertThat(generatedTraceId.get()).matches("[0-9a-f]{32}");
    }
}
