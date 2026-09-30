package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.channel.DeliveryResult;
import com.sonrisa.alerting.spi.channel.NotificationChannel;
import com.sonrisa.alerting.spi.channel.NotificationContent;
import com.sonrisa.alerting.spi.channel.PacingLimits;
import com.sonrisa.alerting.spi.channel.Recipient;

/** Minimal test channel; renders the title and reports every message as delivered. */
public class TestChannel implements NotificationChannel<String> {

    private final String key;

    public TestChannel(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String render(NotificationContent content, Recipient recipient) {
        return content.title();
    }

    @Override
    public DeliveryResult send(String message, Recipient recipient) {
        return DeliveryResult.delivered();
    }

    @Override
    public PacingLimits pacingLimits() {
        return PacingLimits.unlimited();
    }
}
