package com.sonrisa.alerting.app.security;

import com.sonrisa.alerting.app.api.ProblemResponseWriter;
import com.sonrisa.alerting.app.api.ProblemType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * Filter chain for the application port (BE-13; architecture Sections 9.1, 11, 13.1; ADR-02, ADR-03).
 *
 * <ul>
 *   <li>CSRF protection for every state-changing request with the cookie-to-header pattern that Angular
 *       supports: the token is in the {@code XSRF-TOKEN} cookie (readable by the SPA) and must be sent back
 *       in the {@code X-XSRF-TOKEN} header. {@code GET /api/v1/csrf} issues the cookie (OP-01).</li>
 *   <li>{@code /api/v1/admin/**} needs an authenticated user. The Google login and the allow-list follow in
 *       BE-20; until then no one can sign in. Everything else is public: the sign-up endpoints, the CSRF
 *       endpoint and the API docs. Unknown paths reach Spring MVC and get 404.</li>
 *   <li>API callers get Problem Details, never a redirect: 401 {@code unauthenticated} without a login,
 *       403 {@code csrf-token-invalid} for a missing or wrong CSRF token, 403 {@code access-denied}
 *       otherwise (OP-03).</li>
 *   <li>No request cache: a 401 does not create a session to remember the request (after login the UI
 *       always lands on {@code /admin}, OP-13).</li>
 *   <li>HSTS and the other edge headers (CSP) are set by the edge (BE-44), so the core does not send HSTS;
 *       Spring Security's other default headers stay.</li>
 * </ul>
 *
 * <p>Cookies: the CSRF cookie is {@code SameSite=Lax} and {@code Secure}, following the session cookie
 * setting {@code server.servlet.session.cookie.secure} (default {@code true}), so one switch controls both.
 * The session cookie flags are set in {@code application.yaml}.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationSecurityConfiguration {

    static final String ADMIN_API = "/api/v1/admin/**";

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http, ProblemResponseWriter problems,
            @Value("${server.servlet.session.cookie.secure:true}") boolean secureCookies) throws Exception {
        http.authorizeHttpRequests(requests -> requests
                        .requestMatchers(ADMIN_API).authenticated()
                        .anyRequest().permitAll())
                .csrf(csrf -> csrf.spa().csrfTokenRepository(csrfTokenRepository(secureCookies)))
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(unauthenticated(problems))
                        .accessDeniedHandler(accessDenied(problems)))
                .requestCache(cache -> cache.requestCache(new NullRequestCache()))
                .headers(headers -> headers.httpStrictTransportSecurity(hsts -> hsts.disable()));
        return http.build();
    }

    /** {@code XSRF-TOKEN} cookie, readable by JavaScript (that is the pattern), path {@code /}. */
    static CookieCsrfTokenRepository csrfTokenRepository(boolean secureCookies) {
        CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
        repository.setCookieCustomizer(cookie -> cookie.secure(secureCookies).sameSite("Lax"));
        return repository;
    }

    private static AuthenticationEntryPoint unauthenticated(ProblemResponseWriter problems) {
        return (request, response, exception) -> problems.write(request, response, ProblemType.UNAUTHENTICATED);
    }

    private static AccessDeniedHandler accessDenied(ProblemResponseWriter problems) {
        return (request, response, exception) -> problems.write(request, response,
                exception instanceof CsrfException ? ProblemType.CSRF_TOKEN_INVALID : ProblemType.ACCESS_DENIED);
    }
}
