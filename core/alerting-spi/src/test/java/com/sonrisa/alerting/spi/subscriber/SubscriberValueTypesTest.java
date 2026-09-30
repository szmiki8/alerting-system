package com.sonrisa.alerting.spi.subscriber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import java.util.Locale;
import org.junit.jupiter.api.Test;

class SubscriberValueTypesTest {

    @Test
    void inputAcceptsMissingFieldsAndHidesTheAddress() {
        SubscriberInput input = new SubscriberInput(null, "https://hooks.slack.com/services/T0/B0/secret");

        assertThat(input.displayName()).isNull();
        assertThat(input.toString()).doesNotContain("secret").contains("***");
        assertThat(new SubscriberInput(null, null).toString()).contains("address=null");
    }

    @Test
    void fieldErrorNeedsFieldCodeAndMessage() {
        FieldError error = new FieldError(SubscriberInput.FIELD_ADDRESS, "invalid-format", "Enter a valid email.");
        assertThat(error.field()).isEqualTo("address");

        assertThatIllegalArgumentException().isThrownBy(() -> new FieldError(" ", "required", "Required."));
        assertThatIllegalArgumentException().isThrownBy(() -> new FieldError("address", null, "Required."));
        assertThatIllegalArgumentException().isThrownBy(() -> new FieldError("address", "required", ""));
    }

    @Test
    void normalisedAddressMustNotBeBlankAndHidesItsValue() {
        NormalisedAddress address = new NormalisedAddress("https://hooks.slack.com/services/T0/B0/secret");

        assertThat(address.toString()).doesNotContain("secret");
        assertThat(address).isEqualTo(new NormalisedAddress("https://hooks.slack.com/services/T0/B0/secret"));
        assertThatIllegalArgumentException().isThrownBy(() -> new NormalisedAddress(" "));
        assertThatIllegalArgumentException().isThrownBy(() -> new NormalisedAddress(null));
    }

    @Test
    void verificationResultHasAReasonExactlyWhenItFailed() {
        assertThat(VerificationResult.verified().passed()).isTrue();
        assertThat(VerificationResult.notRequired().passed()).isTrue();
        VerificationResult failed = VerificationResult.failed("Slack answered 404");
        assertThat(failed.passed()).isFalse();
        assertThat(failed.reason()).isEqualTo("Slack answered 404");

        assertThatIllegalArgumentException().isThrownBy(() -> VerificationResult.failed(" "));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new VerificationResult(VerificationResult.Outcome.VERIFIED, "why"));
    }

    @Test
    void verificationIsNotRequiredByDefault() {
        SubscriberType type = new SubscriberType() {
            @Override
            public String key() {
                return "plain";
            }

            @Override
            public String channelKey() {
                return "log";
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
                return new NormalisedAddress(input.address().trim().toLowerCase(Locale.ROOT));
            }

            @Override
            public String mask(NormalisedAddress address) {
                return address.value();
            }
        };

        NormalisedAddress address = type.normalise(new SubscriberInput("Anna", " A@Example.org "));
        assertThat(address.value()).isEqualTo("a@example.org");
        assertThat(type.verify(address, "Anna").outcome()).isEqualTo(VerificationResult.Outcome.NOT_REQUIRED);
    }
}
