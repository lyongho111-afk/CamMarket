package com.campus.remarket.config;

import com.campus.remarket.service.AccountService;
import demo.campus.campusremarketmember.member.repository.MemberRepository;
import demo.campus.campusremarketmember.member.util.LoginSession;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import org.springframework.security.web.context.NullSecurityContextRepository;
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

/** Security owns login/account CSRF and portal roles; existing content CSRF guards remain active. */
@Configuration
public class LoginSecurityConfig {
    @Bean UserDetailsService existingMemberAuthenticationOnly() {
        return username -> { throw new UsernameNotFoundException("Use the member login endpoint"); };
    }
    @Bean HttpSessionCsrfTokenRepository accountCsrfRepository() {
        var repository = new HttpSessionCsrfTokenRepository();
        repository.setHeaderName("X-Account-CSRF");
        return repository;
    }

    @Bean SecurityFilterChain campusSecurity(HttpSecurity http, MemberRepository members,
                                             HttpSessionCsrfTokenRepository csrf) throws Exception {
        RequestMatcher accountMutation = request -> !List.of("GET", "HEAD", "OPTIONS", "TRACE").contains(request.getMethod())
                && (request.getServletPath().startsWith("/api/members/") || request.getServletPath().startsWith("/api/account/"));
        http.csrf(config -> config.csrfTokenRepository(csrf).requireCsrfProtectionMatcher(accountMutation))
            .securityContext(config -> config.securityContextRepository(new NullSecurityContextRepository()))
            .requestCache(config -> config.disable())
            .formLogin(config -> config.disable()).httpBasic(config -> config.disable()).logout(config -> config.disable())
            .authorizeHttpRequests(config -> config
                .requestMatchers("/api/admin/**", "/api/members/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/owner/**").hasAnyRole("OWNER", "MANAGER")
                .requestMatchers("/api/members/managers", "/api/members/managers/**").hasRole("OWNER")
                .requestMatchers("/api/members/me", "/api/account/password").authenticated()
                .anyRequest().permitAll())
            .exceptionHandling(config -> config
                .authenticationEntryPoint((request, response, error) -> jsonError(response, 401, "로그인이 필요해요."))
                .accessDeniedHandler((request, response, error) -> jsonError(response, 403, "권한 또는 인증 정보가 올바르지 않습니다. 화면을 새로고침해 주세요.")))
            .headers(config -> config.frameOptions(frame -> frame.sameOrigin()))
            .addFilterBefore(new MemberSessionAuthentication(members), AnonymousAuthenticationFilter.class);
        return http.build();
    }

    private static void jsonError(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
    }

    /** Resolve the current DB role on every request, including deleted managers and revoked passwords. */
    private static final class MemberSessionAuthentication extends OncePerRequestFilter {
        private final MemberRepository members;
        MemberSessionAuthentication(MemberRepository members) { this.members = members; }
        @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                                  FilterChain chain) throws ServletException, IOException {
            var session = request.getSession(false);
            if (session != null && LoginSession.memberId(session) != null) {
                var id = LoginSession.memberId(session);
                long revoked = AccountService.REVOKED.getOrDefault(id, 0L);
                Object started = session.getAttribute("credentialStart");
                var member = members.findById(id);
                if (member.isEmpty() || member.get().isWithdrawn() || revoked > 0 && (!(started instanceof Long time) || time < revoked)) {
                    session.invalidate();
                } else {
                    var authentication = UsernamePasswordAuthenticationToken.authenticated(id, null,
                            List.of(new SimpleGrantedAuthority("ROLE_" + member.get().getRole().name())));
                    var context = SecurityContextHolder.createEmptyContext();
                    context.setAuthentication(authentication);
                    SecurityContextHolder.setContext(context);
                }
            }
            chain.doFilter(request, response);
        }
    }
}
