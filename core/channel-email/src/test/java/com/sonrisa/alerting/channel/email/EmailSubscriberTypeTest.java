package com.sonrisa.alerting.channel.email;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import java.util.List;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class EmailSubscriberTypeTest {

    private static final String VALID_NAME = "Alice";
    private static final String VALID_EMAIL = "alice@example.com";

    private final EmailSubscriberType type = new EmailSubscriberType();

    @Test
    void describesItself() {
        assertThat(type.key()).isEqualTo("email");
        assertThat(type.channelKey()).isEqualTo("email");
        assertThat(type.addressIsSecret()).isFalse();
        assertThat(type.verify(new NormalisedAddress(VALID_EMAIL), VALID_NAME).passed()).isTrue();
    }

    @Nested
    class Name {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   ", "\t\n"})
        void isRequired(String name) {
            assertThat(errors(name, VALID_EMAIL)).containsExactly(nameError("required"));
        }

        @Test
        void isTrimmedAndLimitedTo100Characters() {
            assertThat(errors("n".repeat(100), VALID_EMAIL)).isEmpty();
            assertThat(errors("  " + "n".repeat(100) + "  ", VALID_EMAIL)).isEmpty();
            assertThat(errors("n".repeat(101), VALID_EMAIL)).containsExactly(nameError("too-long"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Ali\nce", "Ali\rce", "Ali\tce", "Ali\u0000ce", "Ali\u0085ce"})
        void refusesControlCharacters(String name) {
            assertThat(errors(name, VALID_EMAIL)).containsExactly(nameError("invalid-characters"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"Zoë Ångström", "O'Brien-Smith", "李小龙", "A"})
        void acceptsOrdinaryNames(String name) {
            assertThat(errors(name, VALID_EMAIL)).isEmpty();
        }
    }

    @Nested
    class Address {

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        void isRequired(String email) {
            assertThat(errors(VALID_NAME, email)).containsExactly(addressError("required"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "alice",
            "alice@",
            "@example.com",
            "alice@@example.com",
            "alice@bob@example.com",
            "alice@example",
            "alice@example.",
            "alice@example.com.",
            "alice@.example.com",
            "alice@example..com",
            "alice@-example.com",
            "alice@example-.com",
            "alice@exa_mple.com",
            "alice@example.123",
            "alice@[127.0.0.1]",
            "alice@127.0.0.1",
            ".alice@example.com",
            "alice.@example.com",
            "al..ice@example.com",
            "al ice@example.com",
            "alice@exam ple.com",
            "\"alice\"@example.com",
            "alice(comment)@example.com",
            "jösé@example.com",
            "alice@example.com\nBcc: bob@example.com"})
        void refusesMalformedAddresses(String email) {
            assertThat(errors(VALID_NAME, email)).containsExactly(addressError("invalid-format"));
        }

        @Test
        void isLimitedTo254CharactersAndTheLocalPartTo64() {
            String domain189 = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(61);
            String longest = "a".repeat(64) + "@" + domain189;
            assertThat(longest).hasSize(254);

            assertThat(errors(VALID_NAME, longest)).isEmpty();
            assertThat(errors(VALID_NAME, longest + "d")).containsExactly(addressError("too-long"));
            assertThat(errors(VALID_NAME, "a".repeat(65) + "@example.com")).containsExactly(addressError("too-long"));
            // Trimmed before the length check.
            assertThat(errors(VALID_NAME, "  " + longest + "  ")).isEmpty();
        }

        @Test
        void aDomainLabelIsLimitedTo63Characters() {
            assertThat(errors(VALID_NAME, "alice@" + "b".repeat(63) + ".com")).isEmpty();
            assertThat(errors(VALID_NAME, "alice@" + "b".repeat(64) + ".com"))
                    .containsExactly(addressError("invalid-format"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            "alice+news@example.com",
            "first.last@mail.sub.example.co.uk",
            "o'brien@example.ie",
            "a@b.co",
            "user_name-1@example-domain.org",
            "x!#$%&'*+/=?^_`{|}~-y@example.com"})
        void acceptsValidAddresses(String email) {
            assertThat(errors(VALID_NAME, email)).isEmpty();
        }
    }

    @Test
    void reportsBothFieldsAtOnce() {
        assertThat(errors(" ", "not-an-email"))
                .containsExactly(nameError("required"), addressError("invalid-format"));
    }

    @Test
    void messagesAreUserFacingAndDoNotEchoTheInput() {
        assertThat(messages("n".repeat(101), "secret-looking-input@"))
                .containsExactly("Name must be 100 characters or fewer.",
                        "Enter a valid email address, for example name@example.com.");
        assertThat(messages("Ali\nce", "a".repeat(65) + "@example.com"))
                .containsExactly("Name must not contain line breaks or control characters.",
                        "The part before @ must be 64 characters or fewer.");
        assertThat(messages(null, "a".repeat(250) + "@example.com"))
                .containsExactly("Enter your name.", "Email address must be 254 characters or fewer.");
        assertThat(messages(VALID_NAME, " ")).containsExactly("Enter your email address.");
    }

    private List<String> messages(String name, String email) {
        return type.validate(new SubscriberInput(name, email)).stream().map(FieldError::message).toList();
    }

    @Nested
    class Normalisation {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "Alice@Example.COM   | alice@example.com",
            "  alice@example.com | alice@example.com",
            "ALICE@EXAMPLE.COM   | alice@example.com",
            "Alice+News@Example.com | alice+news@example.com",
            "Bob@Mail.Sub.Example.co.UK | bob@mail.sub.example.co.uk"})
        void lowerCasesAndTrims(String email, String expected) {
            assertThat(normalise(email)).isEqualTo(expected);
        }

        @Test
        void sameAddressInDifferentCaseGivesTheSameValue() {
            assertThat(normalise("Alice@Example.COM ")).isEqualTo(normalise("alice@example.com"));
        }

        @Test
        void keepsSubAddressesApart() {
            assertThat(normalise("alice+news@example.com")).isNotEqualTo(normalise("alice@example.com"));
        }

        @Test
        void refusesInvalidInputWithoutEchoingIt() {
            assertThatThrownBy(() -> type.normalise(new SubscriberInput(VALID_NAME, "alice@bad@example.com")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("address=invalid-format")
                    .hasMessageNotContaining("alice");
        }
    }

    /** Internationalised domains: Unicode or Punycode in, Punycode out (java.net.IDN, IDNA 2003). */
    @Nested
    class InternationalisedDomains {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "alice@bücher.de           | alice@xn--bcher-kva.de",
            "Alice@BÜCHER.de           | alice@xn--bcher-kva.de",
            "alice@xn--bcher-kva.de    | alice@xn--bcher-kva.de",
            "alice@XN--BCHER-KVA.DE    | alice@xn--bcher-kva.de",
            "alice@münchen.example.com | alice@xn--mnchen-3ya.example.com",
            "alice@пример.рф           | alice@xn--e1afmkfd.xn--p1ai",
            "alice@example\u3002com    | alice@example.com"})
        void storesTheAsciiForm(String email, String expected) {
            assertThat(errors(VALID_NAME, email)).isEmpty();
            assertThat(normalise(email)).isEqualTo(expected);
        }

        @Test
        void unicodeAndPunycodeFormsAreTheSameSubscriber() {
            assertThat(normalise("alice@bücher.de")).isEqualTo(normalise("alice@xn--bcher-kva.de"));
        }

        @ParameterizedTest
        @ValueSource(strings = {
            // IDNA 2003 and 2008 disagree on these characters (ß would become "ss").
            "alice@straße.de",
            "alice@ς.example.com",
            "alice@exa\u200Dmple.com",
            "alice@exa\u200Cmple.com",
            // Not assigned in Unicode 3.2 (the IDNA 2003 tables), so java.net.IDN refuses it.
            "alice@exa\uD83D\uDE00mple.com"})
        void refusesDomainsThatWouldNotMapSafely(String email) {
            assertThat(errors(VALID_NAME, email)).containsExactly(addressError("invalid-format"));
        }

        @Test
        void checksTheLengthOfTheAsciiForm() {
            // A 60-character Unicode label is too long in Punycode (63-character label limit).
            String email = "alice@" + "ü".repeat(60) + ".de";
            assertThat(email).hasSizeLessThan(254);
            assertThat(errors(VALID_NAME, email)).containsExactly(addressError("invalid-format"));
        }
    }

    @Nested
    class Masking {

        @ParameterizedTest
        @CsvSource(delimiter = '|', value = {
            "alice@example.com         | a***@example.com",
            "a@example.com             | a***@example.com",
            "alice+news@mail.example.com | a***@mail.example.com",
            "alice@xn--bcher-kva.de    | a***@xn--bcher-kva.de"})
        void hidesTheLocalPart(String address, String masked) {
            assertThat(type.mask(new NormalisedAddress(address))).isEqualTo(masked);
        }

        @Test
        void keepsVeryLongDomainsWithin200Characters() {
            String domain = "b".repeat(63) + "." + "c".repeat(63) + "." + "d".repeat(63) + "." + "e".repeat(59);
            String address = normalise("a@" + domain);

            String masked = type.mask(new NormalisedAddress(address));

            assertThat(masked).hasSizeLessThanOrEqualTo(200).startsWith("a***@...").endsWith("e".repeat(59));
        }
    }

    /** The problems as {@code field:code}; the messages are checked in their own test. */
    private List<String> errors(String name, String email) {
        return type.validate(new SubscriberInput(name, email)).stream()
                .map(error -> error.field() + ":" + error.code())
                .toList();
    }

    private String normalise(String email) {
        return type.normalise(new SubscriberInput(VALID_NAME, email)).value();
    }

    private static String nameError(String code) {
        return SubscriberInput.FIELD_DISPLAY_NAME + ":" + code;
    }

    private static String addressError(String code) {
        return SubscriberInput.FIELD_ADDRESS + ":" + code;
    }
}
