package com.sonrisa.alerting.app.librarycheck;

import static org.assertj.core.api.Assertions.assertThat;

import com.icegreen.greenmail.junit5.GreenMailExtension;
import com.icegreen.greenmail.util.ServerSetupTest;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSenderImpl;

/** GreenMail receives mail sent through Spring Mail (Jakarta Mail versions managed by Spring Boot 4.1). */
class GreenMailSmokeTest {

    @RegisterExtension
    static GreenMailExtension greenMail = new GreenMailExtension(ServerSetupTest.SMTP.dynamicPort());

    @Test
    void receivesMailSentWithSpringMail() throws Exception {
        var sender = new JavaMailSenderImpl();
        sender.setHost("localhost");
        sender.setPort(greenMail.getSmtp().getPort());

        var message = new SimpleMailMessage();
        message.setFrom("alerts@example.org");
        message.setTo("reader@example.org");
        message.setSubject("Smoke");
        message.setText("Hello");
        sender.send(message);

        MimeMessage[] received = greenMail.getReceivedMessages();
        assertThat(received).hasSize(1);
        assertThat(received[0].getSubject()).isEqualTo("Smoke");
    }
}
