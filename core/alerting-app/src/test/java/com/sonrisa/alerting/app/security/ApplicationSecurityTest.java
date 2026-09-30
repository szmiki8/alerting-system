package com.sonrisa.alerting.app.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import jakarta.servlet.http.Cookie;
import java.security.Principal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

/** BE-13 acceptance criteria on the application port, with test-only endpoints. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(ApplicationSecurityTest.SecurityTestController.class)
class ApplicationSecurityTest {

    static final String PUBLIC_POST = "/api/v1/test/security/public";
    static final String ADMIN_GET = "/api/v1/admin/test/me";
    static final String CSRF_COOKIE = "XSRF-TOKEN";
    static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    MockMvc mvc;

    /** Does what the UI does at start-up: GET /api/v1/csrf and keep the cookie. */
    Cookie fetchCsrfCookie() throws Exception {
        Cookie cookie = mvc.perform(get(CsrfTokenController.PATH)).andReturn().getResponse().getCookie(CSRF_COOKIE);
        assertThat(cookie).isNotNull();
        return cookie;
    }

    @Test
    void csrfEndpointSetsTheCookieWithoutSideEffects() throws Exception {
        MvcResult result = mvc.perform(get(CsrfTokenController.PATH))
                .andExpect(status().isNoContent())
                .andExpect(cookie().exists(CSRF_COOKIE))
                .andExpect(cookie().httpOnly(CSRF_COOKIE, false))
                .andExpect(cookie().secure(CSRF_COOKIE, true))
                .andExpect(cookie().sameSite(CSRF_COOKIE, "Lax"))
                .andExpect(cookie().path(CSRF_COOKIE, "/"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).as("no session is created").isNull();
    }

    @Test
    void postWithoutTokenIsRejectedWithCsrfProblem() throws Exception {
        mvc.perform(post(PUBLIC_POST).contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.instance").value(PUBLIC_POST));
    }

    @Test
    void postWithCookieButWithoutHeaderIsRejected() throws Exception {
        mvc.perform(post(PUBLIC_POST).cookie(fetchCsrfCookie()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));
    }

    @Test
    void postWithWrongHeaderIsRejected() throws Exception {
        mvc.perform(post(PUBLIC_POST).cookie(fetchCsrfCookie()).header(CSRF_HEADER, "forged"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));
    }

    @Test
    void postWithMatchingCookieAndHeaderPasses() throws Exception {
        Cookie csrf = fetchCsrfCookie();

        mvc.perform(post(PUBLIC_POST).cookie(csrf).header(CSRF_HEADER, csrf.getValue()))
                .andExpect(status().isOk())
                .andExpect(content().string("accepted"));
    }

    @Test
    void everyStateChangingMethodNeedsTheToken() throws Exception {
        mvc.perform(delete("/api/v1/some/unknown/path"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:csrf-token-invalid"));
    }

    @Test
    void unauthenticatedAdminCallGets401ProblemWithoutRedirect() throws Exception {
        MvcResult result = mvc.perform(get("/api/v1/admin/anything"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentType(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:unauthenticated"))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().doesNotExist("Location"))
                .andExpect(header().doesNotExist("WWW-Authenticate"))
                .andReturn();

        assertThat(result.getRequest().getSession(false)).as("no session to remember the request").isNull();
    }

    @Test
    void authenticatedAdminCallPasses() throws Exception {
        mvc.perform(get(ADMIN_GET).with(user("admin@example.org")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin@example.org"));
    }

    @Test
    void publicReadsNeedNoLoginAndUnknownPathsStay404() throws Exception {
        mvc.perform(get("/api/v1/test/security/public"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/unknown"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("urn:alerting:problem:not-found"));
    }

    @Test
    void apiDocsArePublic() throws Exception {
        mvc.perform(get("/v3/api-docs"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString(CsrfTokenController.PATH)))
                .andExpect(content().string(not(containsString("CsrfToken"))));
    }

    @Test
    void coreDoesNotSendHsts() throws Exception {
        mvc.perform(get(CsrfTokenController.PATH).secure(true))
                .andExpect(header().doesNotExist("Strict-Transport-Security"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"));
    }

    @RestController
    static class SecurityTestController {

        @GetMapping(PUBLIC_POST)
        String publicRead() {
            return "public";
        }

        @PostMapping(PUBLIC_POST)
        String publicWrite() {
            return "accepted";
        }

        @GetMapping(ADMIN_GET)
        String admin(Principal principal) {
            return principal.getName();
        }
    }
}
