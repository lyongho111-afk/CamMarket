package com.campus.remarket;

import java.net.CookieManager;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment=SpringBootTest.WebEnvironment.RANDOM_PORT, properties={
    "spring.datasource.url=jdbc:h2:mem:loginsecurity;MODE=MySQL;DB_CLOSE_DELAY=-1",
    "campus.notifications.initial-delay-ms=3600000"})
class LoginSecurityTests {
    @LocalServerPort int port;
    final JsonMapper json = JsonMapper.builder().build();
    HttpClient browser() { return HttpClient.newBuilder().cookieHandler(new CookieManager()).build(); }
    HttpResponse<String> raw(HttpClient client, String method, String path, String body, String token) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path));
        if (body != null) request.header("Content-Type", "application/json");
        if (token != null) request.header("X-Account-CSRF", token);
        return client.send(request.method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
    }
    String token(HttpClient client) throws Exception {
        return json.readTree(raw(client, "GET", "/api/security/csrf", null, null).body()).get("token").asText();
    }
    String credentials(String id, String password, String role) {
        return json.writeValueAsString(java.util.Map.of("loginId", id, "password", password, "role", role));
    }
    @Test void loginRequiresSameSessionTokenAndRotatesItAfterAuthentication() throws Exception {
        var client = browser(); var other = browser();
        String body = credentials("owner1", "campus1234!", "OWNER");
        assertEquals(403, raw(client, "POST", "/api/members/login", body, null).statusCode());
        String before = token(client);
        assertEquals(403, raw(other, "POST", "/api/members/login", body, before).statusCode());
        var response = raw(client, "POST", "/api/members/login", body, before);
        assertEquals(200, response.statusCode());
        assertEquals("nosniff", response.headers().firstValue("X-Content-Type-Options").orElse(""));
        assertEquals(403, raw(client, "POST", "/api/members/logout", null, before).statusCode());
        assertEquals(200, raw(client, "POST", "/api/members/logout", null, token(client)).statusCode());
        assertEquals(401, raw(client, "GET", "/api/members/me", null, null).statusCode());
    }
    @Test void securityFilterEnforcesCurrentMemberRolesAcrossPortals() throws Exception {
        var manager = browser();
        assertEquals(200, raw(manager,"POST","/api/members/login",credentials("manager1","campus1234!","MANAGER"),token(manager)).statusCode());
        assertEquals(200, raw(manager,"GET","/api/owner/dashboard",null,null).statusCode());
        assertEquals(403, raw(manager,"GET","/api/admin/dashboard",null,null).statusCode());
        assertEquals(403, raw(manager,"GET","/api/members/managers",null,null).statusCode());
        assertEquals(401, raw(browser(),"GET","/api/admin/dashboard",null,null).statusCode());
        var owner = browser();
        assertEquals(403, raw(owner,"POST","/api/members/login",credentials("owner1","campus1234!","ADMIN"),token(owner)).statusCode());
        assertEquals(401, raw(owner,"GET","/api/admin/dashboard",null,null).statusCode());
    }
    @Test void repeatedBadCredentialsAreRateLimited() throws Exception {
        var client = browser();
        for (int i=0; i<5; i++) assertEquals(401, raw(client,"POST","/api/members/login",credentials("securityMissingAccount","wrong","OWNER"),token(client)).statusCode());
        assertEquals(429, raw(client,"POST","/api/members/login",credentials("securityMissingAccount","wrong","OWNER"),token(client)).statusCode());
        // A different member can still log in from the same computer.
        assertEquals(200, raw(client,"POST","/api/members/login",credentials("owner1","campus1234!","OWNER"),token(client)).statusCode());
    }
    @Test void resetAndPasswordChangeCannotBeSubmittedWithoutCsrf() throws Exception {
        var client = browser();
        assertEquals(403, raw(client,"POST","/api/account/code","{\"email\":\"nobody@example.com\",\"purpose\":\"RESET\"}",null).statusCode());
        assertEquals(403, raw(client,"POST","/api/account/password","{}",null).statusCode());
        assertEquals(401, raw(client,"POST","/api/account/password","{}",token(client)).statusCode());
    }
}
