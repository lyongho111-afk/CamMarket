package com.campus.remarket.service;

import java.time.Clock;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/** Five failed credentials per account and remote address in ten minutes. */
@Service
public class LoginAttempts {
    private record Failure(int count, long firstAt) {}
    private final Map<String, Failure> failures = new HashMap<>();
    private final Clock clock = Clock.systemUTC();
    private static final long WINDOW = 600_000;
    private String key(String remote, Long school, String login) {
        return remote + ":" + school + ":" + (login == null ? "" : login.trim().toLowerCase(Locale.ROOT));
    }
    public synchronized void check(String remote, Long school, String login) {
        long now = clock.millis();
        failures.entrySet().removeIf(entry -> now - entry.getValue().firstAt() >= WINDOW);
        var failure = failures.get(key(remote, school, login));
        if (failure != null && failure.count() >= 5 || failures.size() >= 10_000)
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "로그인 시도가 반복되어 잠시 제한됐어요. 10분 뒤 다시 시도해 주세요.");
    }
    public synchronized void failed(String remote, Long school, String login) {
        String key = key(remote, school, login);
        var failure = failures.get(key);
        failures.put(key, failure == null ? new Failure(1, clock.millis()) : new Failure(failure.count() + 1, failure.firstAt()));
    }
    public synchronized void succeeded(String remote, Long school, String login) {
        failures.remove(key(remote, school, login));
    }
}
