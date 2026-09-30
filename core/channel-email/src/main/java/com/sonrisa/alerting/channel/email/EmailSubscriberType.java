package com.sonrisa.alerting.channel.email;

import com.sonrisa.alerting.spi.subscriber.FieldError;
import com.sonrisa.alerting.spi.subscriber.NormalisedAddress;
import com.sonrisa.alerting.spi.subscriber.SubscriberInput;
import com.sonrisa.alerting.spi.subscriber.SubscriberType;
import java.net.IDN;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * The {@code email} subscriber type (FR-01, FR-02, FR-05, architecture Section 7.2): a required name and an
 * email address, delivered by the {@code email} channel. The address is not a secret.
 *
 * <p><b>Rules</b> (the UI and the sign-up endpoint mirror them):
 * <ul>
 *   <li>Both fields are trimmed of leading and trailing whitespace before any check. Lengths are counted in
 *       UTF-16 characters, as the browser's {@code maxlength} and Bean Validation's {@code @Size} count.</li>
 *   <li>Name: required, at most {@value #NAME_MAX_LENGTH} characters, no control characters (line breaks,
 *       tabs), because the name may end up in an email header.</li>
 *   <li>Email: required, at most {@value #ADDRESS_MAX_LENGTH} characters (RFC 5321 path limit), local part at
 *       most {@value #LOCAL_PART_MAX_LENGTH} characters. The local part is an RFC 5322 dot-atom in ASCII (no
 *       quoted strings, no comments, no non-ASCII characters). The domain has at least two labels, the last one
 *       starting with a letter; IP literals are refused.</li>
 *   <li>Internationalised domains are accepted in Unicode or in Punycode and stored in their ASCII (Punycode)
 *       form, converted with {@link IDN#toASCII(String, int)} (IDNA 2003, RFC 3490). Domains with the four
 *       characters that IDNA 2003 and IDNA 2008 treat differently (ß, final sigma, zero-width joiner and
 *       non-joiner) are refused, because IDNA 2003 would map them to a different domain.</li>
 * </ul>
 *
 * <p><b>Normalisation:</b> the trimmed address in lower case (local part included) with the domain in ASCII.
 * {@code Alice@Example.COM }, {@code alice@example.com} and {@code alice@EXAMPLE.com} are the same subscriber;
 * {@code alice+news@example.com} is a different one (sub-addresses are kept as entered).
 */
public final class EmailSubscriberType implements SubscriberType {

    /** The type key and the key of the channel that delivers to it. */
    public static final String KEY = "email";

    public static final int NAME_MAX_LENGTH = 100;
    public static final int ADDRESS_MAX_LENGTH = 254;
    public static final int LOCAL_PART_MAX_LENGTH = 64;

    /** Longest domain shown in full by {@link #mask}; keeps the masked form within its 200-character column. */
    static final int MASK_DOMAIN_MAX_LENGTH = 190;

    static final String CODE_REQUIRED = "required";
    static final String CODE_TOO_LONG = "too-long";
    static final String CODE_INVALID_FORMAT = "invalid-format";
    static final String CODE_INVALID_CHARACTERS = "invalid-characters";

    private static final String ATOM = "[A-Za-z0-9!#$%&'*+/=?^_`{|}~-]+";
    private static final Pattern LOCAL_PART = Pattern.compile(ATOM + "(?:\\." + ATOM + ")*");
    private static final String LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
    private static final Pattern ASCII_DOMAIN =
            Pattern.compile("(?:" + LABEL + "\\.)+[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?");
    /** ß, final sigma, ZWNJ, ZWJ: IDNA 2003 maps them away, IDNA 2008 keeps them (UTS #46 deviations). */
    private static final Pattern IDNA_DEVIATIONS = Pattern.compile("[\\u00DF\\u03C2\\u200C\\u200D]");

    private static final String INVALID_FORMAT_MESSAGE = "Enter a valid email address, for example name@example.com.";

    @Override
    public String key() {
        return KEY;
    }

    @Override
    public String channelKey() {
        return KEY;
    }

    @Override
    public boolean addressIsSecret() {
        return false;
    }

    @Override
    public List<FieldError> validate(SubscriberInput input) {
        List<FieldError> errors = new ArrayList<>(2);
        FieldError nameError = checkName(input.displayName());
        if (nameError != null) {
            errors.add(nameError);
        }
        Parsed address = parse(input.address());
        if (address.error != null) {
            errors.add(address.error);
        }
        return List.copyOf(errors);
    }

    @Override
    public NormalisedAddress normalise(SubscriberInput input) {
        List<FieldError> errors = validate(input);
        if (!errors.isEmpty()) {
            // Only the codes: the message must not echo the input.
            throw new IllegalArgumentException("Invalid email subscriber input: "
                    + errors.stream().map(e -> e.field() + "=" + e.code()).toList());
        }
        return new NormalisedAddress(parse(input.address()).value);
    }

    /** Shows the first character of the local part and the domain, for example {@code a***@example.com}. */
    @Override
    public String mask(NormalisedAddress address) {
        String value = address.value();
        int at = value.lastIndexOf('@');
        if (at < 1) {
            return "***";
        }
        String domain = value.substring(at + 1);
        if (domain.length() > MASK_DOMAIN_MAX_LENGTH) {
            domain = "..." + domain.substring(domain.length() - (MASK_DOMAIN_MAX_LENGTH - 3));
        }
        return value.charAt(0) + "***@" + domain;
    }

    private static @Nullable FieldError checkName(@Nullable String raw) {
        String name = raw == null ? "" : raw.strip();
        if (name.isEmpty()) {
            return nameError(CODE_REQUIRED, "Enter your name.");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            return nameError(CODE_TOO_LONG, "Name must be " + NAME_MAX_LENGTH + " characters or fewer.");
        }
        if (name.chars().anyMatch(Character::isISOControl)) {
            return nameError(CODE_INVALID_CHARACTERS, "Name must not contain line breaks or control characters.");
        }
        return null;
    }

    private static Parsed parse(@Nullable String raw) {
        String address = raw == null ? "" : raw.strip();
        if (address.isEmpty()) {
            return Parsed.invalid(addressError(CODE_REQUIRED, "Enter your email address."));
        }
        if (address.length() > ADDRESS_MAX_LENGTH) {
            return Parsed.invalid(tooLong());
        }
        int at = address.lastIndexOf('@');
        if (at < 1 || at == address.length() - 1 || address.indexOf('@') != at) {
            return Parsed.invalid(invalidFormat());
        }
        String localPart = address.substring(0, at);
        if (localPart.length() > LOCAL_PART_MAX_LENGTH) {
            return Parsed.invalid(addressError(CODE_TOO_LONG,
                    "The part before @ must be " + LOCAL_PART_MAX_LENGTH + " characters or fewer."));
        }
        if (!LOCAL_PART.matcher(localPart).matches()) {
            return Parsed.invalid(invalidFormat());
        }
        String domain = asciiDomain(address.substring(at + 1));
        if (domain == null) {
            return Parsed.invalid(invalidFormat());
        }
        String normalised = localPart.toLowerCase(Locale.ROOT) + "@" + domain;
        if (normalised.length() > ADDRESS_MAX_LENGTH) {
            // Possible when the Punycode form of a Unicode domain is longer than the input.
            return Parsed.invalid(tooLong());
        }
        return Parsed.valid(normalised);
    }

    /** The domain in lower-case ASCII (Punycode for Unicode labels), or {@code null} when it is not valid. */
    private static @Nullable String asciiDomain(String domain) {
        if (IDNA_DEVIATIONS.matcher(domain).find()) {
            return null;
        }
        String ascii;
        try {
            // No flags: unassigned code points are refused; the ASCII rules are checked by the pattern below.
            ascii = IDN.toASCII(domain, 0).toLowerCase(Locale.ROOT);
        } catch (IllegalArgumentException e) {
            return null;
        }
        return ASCII_DOMAIN.matcher(ascii).matches() ? ascii : null;
    }

    private static FieldError tooLong() {
        return addressError(CODE_TOO_LONG, "Email address must be " + ADDRESS_MAX_LENGTH + " characters or fewer.");
    }

    private static FieldError invalidFormat() {
        return addressError(CODE_INVALID_FORMAT, INVALID_FORMAT_MESSAGE);
    }

    private static FieldError nameError(String code, String message) {
        return new FieldError(SubscriberInput.FIELD_DISPLAY_NAME, code, message);
    }

    private static FieldError addressError(String code, String message) {
        return new FieldError(SubscriberInput.FIELD_ADDRESS, code, message);
    }

    /** Result of parsing the address: exactly one of the two fields is set. */
    private static final class Parsed {

        final @Nullable String value;
        final @Nullable FieldError error;

        private Parsed(@Nullable String value, @Nullable FieldError error) {
            this.value = value;
            this.error = error;
        }

        static Parsed valid(String value) {
            return new Parsed(value, null);
        }

        static Parsed invalid(FieldError error) {
            return new Parsed(null, error);
        }
    }
}
