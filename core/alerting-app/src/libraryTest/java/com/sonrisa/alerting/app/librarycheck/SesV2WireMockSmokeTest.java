package com.sonrisa.alerting.app.librarycheck;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.net.URI;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sesv2.SesV2Client;
import software.amazon.awssdk.services.sesv2.model.SendEmailResponse;

/**
 * WireMock runs next to Spring Boot 4.1's dependency versions, and the AWS SDK v2 SESv2 client
 * sends an email request to it (no AWS account needed).
 */
class SesV2WireMockSmokeTest {

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance()
            .options(wireMockConfig().dynamicPort())
            .build();

    @Test
    void sesV2ClientSendsEmailToStubbedEndpoint() {
        wireMock.stubFor(post(urlEqualTo("/v2/email/outbound-emails"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"MessageId\":\"smoke-1\"}")));

        try (SesV2Client client = SesV2Client.builder()
                .endpointOverride(URI.create(wireMock.baseUrl()))
                .region(Region.EU_CENTRAL_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .build()) {

            SendEmailResponse response = client.sendEmail(request -> request
                    .fromEmailAddress("alerts@example.org")
                    .destination(d -> d.toAddresses("reader@example.org"))
                    .content(c -> c.simple(m -> m
                            .subject(s -> s.data("Smoke"))
                            .body(b -> b.text(t -> t.data("Hello"))))));

            assertThat(response.messageId()).isEqualTo("smoke-1");
        }
        wireMock.verify(postRequestedFor(urlEqualTo("/v2/email/outbound-emails")));
    }
}
