package com.sonrisa.alerting.app.security;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * Forwarded headers and the session cookie need the real server (Tomcat's RemoteIpValve and session
 * cookie configuration are not part of MockMvc). The test client connects from localhost, which the
 * valve trusts like the edge proxy on the internal network.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(BehindProxyTest.ProxyTestController.class)
class BehindProxyTest {

    static final String URL_PATH = "/api/v1/test/proxy/url";
    static final String SESSION_PATH = "/api/v1/test/proxy/session";

    @LocalServerPort
    int port;

    HttpResponse<String> get(String path, String... headers) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path));
        if (headers.length > 0) {
            request.headers(headers);
        }
        return HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void generatedUrlsUseTheSchemeAndHostOfTheEdge() throws Exception {
        HttpResponse<String> response = get(URL_PATH,
                "X-Forwarded-Proto", "https", "X-Forwarded-Host", "alerting.example.org");

        assertThat(response.body()).isEqualTo("https://alerting.example.org" + URL_PATH + " secure=true");
    }

    @Test
    void withoutForwardedHeadersTheRequestIsPlainHttp() throws Exception {
        assertThat(get(URL_PATH).body()).isEqualTo("http://localhost:" + port + URL_PATH + " secure=false");
    }

    @Test
    void csrfCookieIsSecureAndLaxBehindTheEdge() throws Exception {
        HttpResponse<String> response = get(CsrfTokenController.PATH, "X-Forwarded-Proto", "https");

        assertThat(response.statusCode()).isEqualTo(204);
        assertThat(setCookie(response, "XSRF-TOKEN"))
                .contains("Secure", "SameSite=Lax", "Path=/")
                .doesNotContain("HttpOnly");
    }

    @Test
    void sessionCookieIsSecureHttpOnlyAndLax() throws Exception {
        HttpResponse<String> response = get(SESSION_PATH, "X-Forwarded-Proto", "https");

        assertThat(setCookie(response, "JSESSIONID"))
                .contains("Secure", "HttpOnly")
                .containsIgnoringCase("SameSite=Lax");
    }

    private static String setCookie(HttpResponse<String> response, String name) {
        List<String> cookies = response.headers().allValues("Set-Cookie");
        return cookies.stream().filter(cookie -> cookie.startsWith(name + "=")).findFirst()
                .orElseThrow(() -> new AssertionError("no " + name + " cookie in " + cookies));
    }

    @RestController
    static class ProxyTestController {

        @GetMapping(URL_PATH)
        String url(HttpServletRequest request) {
            return ServletUriComponentsBuilder.fromCurrentRequest().toUriString() + " secure=" + request.isSecure();
        }

        /** Stands in for the admin login (BE-20), which is the only code that will create sessions. */
        @GetMapping(SESSION_PATH)
        String session(HttpServletRequest request) {
            return request.getSession(true).getId().isEmpty() ? "no session" : "session";
        }
    }
}
