package com.sonrisa.alerting.app.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Filter chain for the application port. Placeholder until BE-13: the skeleton has no API yet, so
 * requests pass and unknown paths (including {@code /actuator/**}) get 404. CSRF protection stays on
 * (Spring Security default); there is no login mechanism on this port yet.
 */
@Configuration(proxyBeanMethods = false)
public class ApplicationSecurityConfiguration {

    @Bean
    @Order(2)
    SecurityFilterChain applicationSecurityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(requests -> requests.anyRequest().permitAll());
        return http.build();
    }
}
