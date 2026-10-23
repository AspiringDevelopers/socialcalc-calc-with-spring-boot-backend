package com.aspiringinvestments.backend;

import io.jsonwebtoken.Jwts;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end tests over HTTP against a disposable PostgreSQL container:
 * register, login, save, list, get, and per-user isolation.
 */
@Testcontainers
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "jwt.secret=test-only-jwt-secret-0123456789abcdef"
)
class FileApiIntegrationTests {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    private static final ParameterizedTypeReference<Map<String, Object>> JSON =
        new ParameterizedTypeReference<>() {};

    @Autowired
    private TestRestTemplate rest;

    private static String uniqueEmail(String name) {
        return name + "-" + UUID.randomUUID() + "@example.com";
    }

    private static String fixture(String name) throws IOException {
        return new String(new ClassPathResource("fixtures/" + name).getContentAsByteArray(), StandardCharsets.UTF_8);
    }

    private ResponseEntity<Map<String, Object>> call(HttpMethod method, String path, String token, Object body) {
        HttpHeaders headers = new HttpHeaders();
        if (token != null) {
            headers.setBearerAuth(token);
        }
        return rest.exchange(path, method, new HttpEntity<>(body, headers), JSON);
    }

    @SuppressWarnings("unchecked")
    private static <T> T data(ResponseEntity<Map<String, Object>> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("result", "ok");
        return (T) response.getBody().get("data");
    }

    private String register(String email, String password) {
        Map<String, Object> data = data(call(HttpMethod.POST, "/api/auth/register", null,
            Map.of("email", email, "password", password)));
        return (String) data.get("token");
    }

    private String login(String email, String password) {
        Map<String, Object> data = data(call(HttpMethod.POST, "/api/auth/login", null,
            Map.of("email", email, "password", password)));
        return (String) data.get("token");
    }

    @Test
    void registerLoginSaveListGetRoundTripsSocialCalcFileByteIdentical() throws IOException {
        String email = uniqueEmail("alice");
        register(email, "alice-password");
        String token = login(email, "alice-password");
        assertThat(token).isNotBlank();

        String sheet = fixture("invoice.xlsx.sc");
        assertThat(data(call(HttpMethod.POST, "/api/files/save", token,
            Map.of("fname", "invoice", "data", sheet))).equals("Done")).isTrue();

        List<Map<String, Object>> files = data(call(HttpMethod.GET, "/api/files/save", token, null));
        assertThat(files).extracting(f -> f.get("fname")).containsExactly("invoice");

        Map<String, Object> file = data(call(HttpMethod.POST, "/api/files/insert", token,
            Map.of("filename", "invoice")));
        assertThat(((String) file.get("data")).getBytes(StandardCharsets.UTF_8))
            .isEqualTo(sheet.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void webappSaveListGetRoundTripsSocialCalcFileByteIdentical() throws IOException {
        String token = register(uniqueEmail("carol"), "carol-password");
        String sheet = fixture("invoice.xlsx.sc");

        Map<String, Object> saved = data(call(HttpMethod.POST, "/api/webapp", token,
            Map.of("action", "savefile", "appname", "a6app", "fname", "invoice", "data", sheet)));
        assertThat(saved).containsEntry("result", "ok");

        Map<String, Object> listed = data(call(HttpMethod.POST, "/api/webapp", token,
            Map.of("action", "listdir", "appname", "a6app")));
        assertThat((List<String>) listed.get("data")).containsExactly("invoice");

        Map<String, Object> file = data(call(HttpMethod.POST, "/api/webapp", token,
            Map.of("action", "getfile", "appname", "a6app", "fname", "invoice")));
        assertThat(((String) file.get("data")).getBytes(StandardCharsets.UTF_8))
            .isEqualTo(sheet.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void secondUserCannotReadListOrOverwriteFirstUsersFile() throws IOException {
        String alice = register(uniqueEmail("alice"), "alice-password");
        String bob = register(uniqueEmail("bob"), "bob-password");
        String sheet = fixture("invoice.xlsx.sc");

        data(call(HttpMethod.POST, "/api/files/save", alice, Map.of("fname", "private", "data", sheet)));
        data(call(HttpMethod.POST, "/api/webapp", alice,
            Map.of("action", "savefile", "appname", "a6app", "fname", "private", "data", sheet)));

        List<?> bobFiles = data(call(HttpMethod.GET, "/api/files/save", bob, null));
        assertThat(bobFiles).isEmpty();

        ResponseEntity<Map<String, Object>> bobGet = call(HttpMethod.POST, "/api/files/insert", bob,
            Map.of("filename", "private"));
        assertThat(bobGet.getBody()).containsEntry("result", "fail").doesNotContainKey("data");

        ResponseEntity<Map<String, Object>> bobWebappGet = call(HttpMethod.POST, "/api/webapp", bob,
            Map.of("action", "getfile", "appname", "a6app", "fname", "private"));
        assertThat(bobWebappGet.getBody()).containsEntry("result", "fail").doesNotContainKey("data");

        Map<String, Object> bobList = data(call(HttpMethod.POST, "/api/webapp", bob,
            Map.of("action", "listdir", "appname", "a6app")));
        assertThat((List<?>) bobList.get("data")).isEmpty();

        // Bob saving under the same name creates his own file and leaves Alice's untouched.
        data(call(HttpMethod.POST, "/api/files/save", bob, Map.of("fname", "private", "data", "bob's sheet")));
        Map<String, Object> aliceFile = data(call(HttpMethod.POST, "/api/files/insert", alice,
            Map.of("filename", "private")));
        assertThat(aliceFile).containsEntry("data", sheet);
    }

    @Test
    void wrongPasswordIsRejected() {
        String email = uniqueEmail("dave");
        register(email, "dave-password");

        ResponseEntity<Map<String, Object>> response = call(HttpMethod.POST, "/api/auth/login", null,
            Map.of("email", email, "password", "not-the-password"));
        assertThat(response.getBody()).containsEntry("result", "fail").doesNotContainKey("data");
    }

    @Test
    void fileEndpointsRequireAToken() {
        assertThat(call(HttpMethod.GET, "/api/files/save", null, null).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(call(HttpMethod.GET, "/api/files/save", "not-a-jwt", null).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        String forged = Jwts.builder()
            .subject(UUID.randomUUID().toString())
            .claim("email", "mallory@example.com")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(Jwts.SIG.HS256.key().build())
            .compact();

        assertThat(call(HttpMethod.GET, "/api/files/save", forged, null).getStatusCode())
            .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
