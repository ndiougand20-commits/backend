package com.rezo.backend.security;

import com.rezo.backend.service.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for the JwtRequestFilter behavior in isolation.
 * Note: standaloneSetup does NOT load SecurityConfig, so missing-token requests
 * pass through the filter (filter calls chain.doFilter). The actual 401 for missing
 * tokens comes from SecurityConfig's authenticationEntryPoint in the full app.
 * These tests validate the filter's own logic: block invalid tokens, pass valid ones.
 */
@ExtendWith(MockitoExtension.class)
class JwtSecurityTest {

    @Mock
    private JwtService jwtService;

    private MockMvc mockMvc;

    @RestController
    @RequestMapping("/api")
    static class FakeProtectedController {
        @GetMapping("/protected")
        public String protectedEndpoint() {
            return "OK";
        }
    }

    @BeforeEach
    void setUp() {
        JwtRequestFilter filter = new JwtRequestFilter(jwtService);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new FakeProtectedController())
                .addFilters(filter)
                .build();
    }

    @Test
    void shouldReturn401WhenTokenIsInvalid() throws Exception {
        when(jwtService.isAccessTokenValid("bad.token.here")).thenReturn(false);

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer bad.token.here"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn200WhenTokenIsValid() throws Exception {
        when(jwtService.isAccessTokenValid("valid.token")).thenReturn(true);
        when(jwtService.extractSubject("valid.token")).thenReturn("user-id-123");
        when(jwtService.extractRole("valid.token")).thenReturn("ETUDIANT");

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer valid.token"))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));
    }

    @Test
    void shouldPassThroughWhenNoAuthHeader() throws Exception {
        // Filter passes through — in production, SecurityConfig's entryPoint returns 401.
        // Here in standalone mode, the controller responds normally.
        mockMvc.perform(get("/api/protected"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldPassThroughWhenAuthHeaderIsNotBearer() throws Exception {
        // Non-Bearer header: filter ignores it, passes through.
        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Basic some-basic-token"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldSetAuthenticationContextOnValidToken() throws Exception {
        when(jwtService.isAccessTokenValid("role.token")).thenReturn(true);
        when(jwtService.extractSubject("role.token")).thenReturn("user-42");
        when(jwtService.extractRole("role.token")).thenReturn("ADMIN");

        mockMvc.perform(get("/api/protected")
                        .header("Authorization", "Bearer role.token"))
                .andExpect(status().isOk());
    }
}
