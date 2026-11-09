package com.campus.remarket;

import java.net.URI;
import java.net.http.*;
import java.util.List;
import tools.jackson.databind.json.JsonMapper;

/** HTTP integration clients follow the same account token exchange as the browser. */
final class SecurityTestClient {
    static void csrf(HttpClient client, HttpRequest.Builder builder, String method, String path, int port) throws Exception {
        if (List.of("GET", "HEAD", "OPTIONS", "TRACE").contains(method)
            || !(path.startsWith("/api/members/") || path.startsWith("/api/account/"))) return;
        var response = client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/security/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new AssertionError("CSRF endpoint failed");
        var token = JsonMapper.builder().build().readTree(response.body());
        builder.header(token.get("headerName").asText(), token.get("token").asText());
    }
}
