/**
 * Slack plugin (architecture Sections 7.2, 7.3): the Slack incoming webhook client and the {@code slack}
 * subscriber type (BE-18); the Slack notification channel follows in BE-34.
 *
 * <p>Registered by {@link com.sonrisa.alerting.channel.slack.SlackAutoConfiguration}, off unless
 * {@code alerting.channels.slack.enabled=true}. Settings under {@code alerting.channels.slack.*}
 * ({@link com.sonrisa.alerting.channel.slack.SlackProperties}).
 */
@NullMarked
package com.sonrisa.alerting.channel.slack;

import org.jspecify.annotations.NullMarked;
