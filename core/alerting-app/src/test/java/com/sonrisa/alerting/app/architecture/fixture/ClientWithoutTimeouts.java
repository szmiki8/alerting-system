package com.sonrisa.alerting.app.architecture.fixture;

import java.net.http.HttpClient;
import org.springframework.web.client.RestClient;

/** Fixture: outbound HTTP clients built without the integration's timeouts (forbidden). */
public class ClientWithoutTimeouts {

    RestClient created() {
        return RestClient.create();
    }

    RestClient built() {
        return RestClient.builder().build();
    }

    HttpClient jdkClient() {
        return HttpClient.newHttpClient();
    }
}
