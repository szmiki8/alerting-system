package com.sonrisa.alerting.channel.slack;

import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jspecify.annotations.Nullable;

/**
 * A Slack incoming webhook URL that passed the strict format check (FR-04, architecture Section 13.1).
 * It is the only way to address {@link SlackWebhookClient}, so no other URL can ever be called (SSRF
 * protection).
 *
 * <p>Format (Slack docs, "Sending messages using incoming webhooks":
 * {@code https://hooks.slack.com/services/T00000000/B00000000/<secret>}): workspace ID
 * ({@code T...}), webhook ID ({@code B...}) and secret. The whole input must match; there is no URL
 * parser in between whose interpretation could differ from the check (user info, encoded characters,
 * backslashes and similar tricks simply do not match).
 *
 * <p>Equivalent spellings are accepted and normalised to one {@link #canonical() canonical form}:
 * surrounding white space, upper- or mixed-case scheme and host, the explicit default port {@code :443}
 * and one trailing slash. IDs and secret are case-sensitive and kept as they are.
 *
 * <p>{@link #toString()} hides the URL: the secret must never reach a log (NFR-04).
 */
public final class SlackWebhookUrl {

    /** Longest input that is checked at all; longer input is rejected before matching. */
    public static final int MAX_INPUT_LENGTH = 256;

    /**
     * The canonical form as a regular expression; the UI (FE-12) and the API (BE-19) mirror it. Accepted
     * input may differ only in the ways listed in the class comment.
     */
    public static final String CANONICAL_PATTERN =
            "^https://hooks\\.slack\\.com/services/T[A-Z0-9]{8,20}/B[A-Z0-9]{8,20}/[A-Za-z0-9]{20,64}$";

    /** The only host that is ever called. */
    static final String HOST = "hooks.slack.com";

    private static final Pattern INPUT = Pattern.compile(
            "(?i:https)://(?i:hooks\\.slack\\.com)(?::443)?"
                    + "/services/(T[A-Z0-9]{8,20})/(B[A-Z0-9]{8,20})/([A-Za-z0-9]{20,64})/?");

    private static final int VISIBLE_SECRET_CHARACTERS = 4;

    private final String workspaceId;
    private final String webhookId;
    private final String secret;

    private SlackWebhookUrl(String workspaceId, String webhookId, String secret) {
        this.workspaceId = workspaceId;
        this.webhookId = webhookId;
        this.secret = secret;
    }

    /**
     * Checks and normalises the input.
     *
     * @return the webhook URL, or empty when the input is not a Slack incoming webhook URL
     */
    public static Optional<SlackWebhookUrl> parse(@Nullable String input) {
        if (input == null) {
            return Optional.empty();
        }
        String trimmed = input.strip();
        if (trimmed.length() > MAX_INPUT_LENGTH) {
            return Optional.empty();
        }
        Matcher matcher = INPUT.matcher(trimmed);
        if (!matcher.matches()) {
            return Optional.empty();
        }
        return Optional.of(new SlackWebhookUrl(matcher.group(1), matcher.group(2), matcher.group(3)));
    }

    /** The canonical URL, for example {@code https://hooks.slack.com/services/T.../B.../...}. */
    public String canonical() {
        return "https://" + HOST + "/services/" + workspaceId + "/" + webhookId + "/" + secret;
    }

    /**
     * A form that is safe for the admin list and logs: host and the last four characters of the secret,
     * for example {@code https://hooks.slack.com/services/...Ab12}.
     */
    public String masked() {
        return "https://" + HOST + "/services/..." + secret.substring(secret.length() - VISIBLE_SECRET_CHARACTERS);
    }

    String workspaceId() {
        return workspaceId;
    }

    String webhookId() {
        return webhookId;
    }

    String secret() {
        return secret;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SlackWebhookUrl that
                && workspaceId.equals(that.workspaceId)
                && webhookId.equals(that.webhookId)
                && secret.equals(that.secret);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workspaceId, webhookId, secret);
    }

    @Override
    public String toString() {
        return "SlackWebhookUrl[" + masked() + "]";
    }
}
