package com.dailymark.config;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.security.core.context.SecurityContextHolder;

class ApiKeyFilterTest {
    private static final String KEY = "01234567890123456789012345678901";

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void protectsPatchRequests() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(KEY);
        MockHttpServletRequest request = request("PATCH", "/api/docs/" + java.util.UUID.randomUUID() + "/uploaded-at");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, new MockFilterChain());

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).contains("Authentication required");
    }

    @Test
    void allowsPatchRequestsWithValidBearerKey() throws Exception {
        ApiKeyFilter filter = new ApiKeyFilter(KEY);
        MockHttpServletRequest request = request("PATCH", "/api/docs/" + java.util.UUID.randomUUID() + "/uploaded-at");
        request.addHeader("Authorization", "Bearer " + KEY);
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicBoolean called = new AtomicBoolean();
        FilterChain chain = (req, res) -> called.set(true);

        filter.doFilter(request, response, chain);

        assertThat(called).isTrue();
        assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
        assertThat(SecurityContextHolder.getContext().getAuthentication().getAuthorities())
                .extracting(Object::toString)
                .contains("ROLE_UPLOADER");
    }

    private MockHttpServletRequest request(String method, String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, uri);
        return request;
    }
}
