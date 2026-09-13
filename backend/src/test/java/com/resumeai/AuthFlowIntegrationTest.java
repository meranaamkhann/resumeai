package com.resumeai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    @DisplayName("the full Spring context loads without any missing-bean or missing-dependency errors")
    void contextLoads() {
        assertThat(restTemplate).isNotNull();
    }

    @Test
    @DisplayName("register issues a working access token that can access a protected endpoint")
    void registerLoginAndAccessProtectedResource() {
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";

        Map<String, String> registerBody = Map.of(
                "email", uniqueEmail,
                "password", "a-valid-password-123",
                "fullName", "Integration Test User"
        );

        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/auth/register", registerBody, Map.class);

        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(registerResponse.getBody()).isNotNull();
        String accessToken = (String) registerResponse.getBody().get("accessToken");
        String refreshToken = (String) registerResponse.getBody().get("refreshToken");
        assertThat(accessToken).isNotBlank();
        assertThat(refreshToken).isNotBlank();

        HttpHeaders authHeaders = new HttpHeaders();
        authHeaders.setBearerAuth(accessToken);
        ResponseEntity<Map> dashboardResponse = restTemplate.exchange(
                "/api/dashboard", HttpMethod.GET, new HttpEntity<>(authHeaders), Map.class);

        assertThat(dashboardResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    @DisplayName("a protected endpoint rejects a request with no token as 401, not a bare 403")
    void protectedEndpointRejectsMissingToken() {
        ResponseEntity<Map> response = restTemplate.getForEntity("/api/dashboard", Map.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("logging in with the wrong password is rejected with 401, never a stack trace")
    void loginWithWrongPasswordIsRejected() {
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
        Map<String, String> registerBody = Map.of(
                "email", uniqueEmail,
                "password", "the-correct-password-123",
                "fullName", "Another Test User"
        );
        restTemplate.postForEntity("/api/auth/register", registerBody, Map.class);

        Map<String, String> loginBody = Map.of("email", uniqueEmail, "password", "the-wrong-password-999");
        ResponseEntity<Map> loginResponse = restTemplate.postForEntity("/api/auth/login", loginBody, Map.class);

        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("a refresh token can be exchanged for a new access token")
    void refreshTokenIssuesNewAccessToken() {
        String uniqueEmail = "test-" + UUID.randomUUID() + "@example.com";
        Map<String, String> registerBody = Map.of(
                "email", uniqueEmail,
                "password", "a-valid-password-123",
                "fullName", "Refresh Test User"
        );
        ResponseEntity<Map> registerResponse = restTemplate.postForEntity("/api/auth/register", registerBody, Map.class);
        String refreshToken = (String) registerResponse.getBody().get("refreshToken");

        ResponseEntity<Map> refreshResponse = restTemplate.postForEntity(
                "/api/auth/refresh", Map.of("refreshToken", refreshToken), Map.class);

        assertThat(refreshResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(refreshResponse.getBody().get("accessToken")).isNotNull();
    }
}
