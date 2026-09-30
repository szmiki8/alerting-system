package com.sonrisa.alerting.app.security;

import static org.springframework.security.config.Customizer.withDefaults;

import com.sonrisa.alerting.app.config.AlertingManagementProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.security.autoconfigure.actuate.web.servlet.EndpointRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Actuator endpoints on the management port (ADR-16, Section 13.4). The health endpoint and its
 * probe groups ({@code /actuator/health/liveness}, {@code /actuator/health/readiness}) are open for
 * load balancers; every other endpoint needs the operator credential with HTTP Basic.
 */
@Configuration(proxyBeanMethods = false)
public class ManagementSecurityConfiguration {

    static final String OPERATOR_ROLE = "OPERATOR";

    private static final Logger log = LoggerFactory.getLogger(ManagementSecurityConfiguration.class);

    @Bean
    @Order(1)
    SecurityFilterChain managementSecurityFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher(EndpointRequest.toAnyEndpoint())
                .authorizeHttpRequests(requests -> requests
                        .requestMatchers(EndpointRequest.to("health")).permitAll()
                        .anyRequest().hasRole(OPERATOR_ROLE))
                .httpBasic(withDefaults())
                // Called by operator tools and monitoring, not by browsers: no session, no CSRF token.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(csrf -> csrf.disable());
        return http.build();
    }

    /**
     * The single operator account. The password comes from configuration
     * ({@code ALERTING_MANAGEMENT_OPERATOR_PASSWORD}); it is hashed in memory and never logged.
     * Without a password there is no account, so only the health endpoint answers.
     */
    @Bean
    UserDetailsService operatorUserDetailsService(AlertingManagementProperties management) {
        if (!management.hasOperatorPassword()) {
            log.warn("No operator password configured (ALERTING_MANAGEMENT_OPERATOR_PASSWORD): "
                    + "management endpoints other than health are not accessible");
            return new InMemoryUserDetailsManager();
        }
        var passwordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder();
        return new InMemoryUserDetailsManager(User.withUsername(management.operatorUsername())
                .password(passwordEncoder.encode(management.operatorPassword()))
                .roles(OPERATOR_ROLE)
                .build());
    }
}
