package com.sonrisa.alerting.app.librarycheck;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Minimal application used only by the library smoke tests. */
@SpringBootApplication
class SmokeApplication {

    @RestController
    static class PingController {

        @GetMapping("/api/v1/ping")
        String ping() {
            return "pong";
        }
    }
}
