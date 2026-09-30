package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.util.List;

/** Minimal test subscriber type that accepts any address. */
public class TestSubscriberType implements SubscriberType {

    private final String key;
    private final String channelKey;

    public TestSubscriberType(String key, String channelKey) {
        this.key = key;
        this.channelKey = channelKey;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public String channelKey() {
        return channelKey;
    }

    @Override
    public boolean addressIsSecret() {
        return false;
    }

    @Override
    public List<FieldError> validate(SubscriberInput input) {
        return List.of();
    }

    @Override
    public NormalisedAddress normalise(SubscriberInput input) {
        return new NormalisedAddress(String.valueOf(input.address()));
    }

    @Override
    public String mask(NormalisedAddress address) {
        return address.value();
    }
}
