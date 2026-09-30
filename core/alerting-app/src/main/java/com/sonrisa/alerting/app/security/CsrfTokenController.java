package com.sonrisa.alerting.app.security;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * CSRF bootstrap for the SPA (OP-01). Nginx serves the UI, so the browser has no {@code XSRF-TOKEN}
 * cookie before its first call to the Core. Spring Security's SPA support sets the cookie on any response
 * that lacks it, but the UI needs one defined call without side effects (no session) that it can make at
 * start-up, before its first POST.
 */
@RestController
@Tag(name = "Security")
class CsrfTokenController {

    static final String PATH = "/api/v1/csrf";

    @GetMapping(PATH)
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Issue the CSRF cookie",
            description = "Sets the XSRF-TOKEN cookie. Send its value in the X-XSRF-TOKEN header with every "
                    + "POST, PUT, PATCH and DELETE request.")
    @ApiResponse(responseCode = "204", description = "Cookie set")
    void csrf(@Parameter(hidden = true) CsrfToken token) {
        // Reading the deferred token makes the repository generate it and write the cookie.
        token.getToken();
    }
}
