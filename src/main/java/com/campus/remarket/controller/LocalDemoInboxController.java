package com.campus.remarket.controller;

import com.campus.remarket.config.LocalDemoMailConfig.Mailbox;
import jakarta.servlet.http.HttpServletRequest;
import java.net.InetAddress;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Profile("local-demo")
public class LocalDemoInboxController {
    private final Mailbox mailbox;
    public LocalDemoInboxController(Mailbox mailbox) { this.mailbox = mailbox; }

    private void localOnly(HttpServletRequest request) {
        try {
            String host = request.getServerName();
            boolean localHost = host.equals("localhost") || host.equals("127.0.0.1") || host.equals("[::1]") || host.equals("::1");
            if (localHost && InetAddress.getByName(request.getRemoteAddr()).isLoopbackAddress()
                    && request.getHeader("Forwarded") == null && request.getHeader("X-Forwarded-For") == null
                    && request.getHeader("X-Forwarded-Host") == null
                    && !"cross-site".equals(request.getHeader("Sec-Fetch-Site"))) return;
        } catch (Exception ignored) { }
        throw new ResponseStatusException(HttpStatus.NOT_FOUND);
    }

    @GetMapping("/api/local-demo/mailbox")
    public ResponseEntity<?> mailbox(HttpServletRequest request) {
        localOnly(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(Map.of("localOnly", true, "messages", mailbox.messages()));
    }

    @GetMapping(value="/local-demo/inbox", produces="text/html;charset=UTF-8")
    public ResponseEntity<?> page(HttpServletRequest request) {
        localOnly(request);
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(new ClassPathResource("local-demo/inbox.html"));
    }
}
