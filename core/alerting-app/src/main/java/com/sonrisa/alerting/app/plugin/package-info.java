/**
 * Plugin registries (architecture Section 7, ADR-10): all beans of each extension interface from
 * {@code alerting-spi}, collected by key.
 *
 * <p><b>Rules checked at start-up</b> (the application does not start otherwise):
 * <ul>
 *   <li>every key matches {@code [a-z][a-z0-9-]*};</li>
 *   <li>keys are unique per extension point (two sources, two subscriber types or two channels with the
 *       same key stop the start-up);</li>
 *   <li>every enabled subscriber type has an enabled channel with its
 *       {@link com.sonrisa.alerting.spi.subscriber.SubscriberType#channelKey() channel key}. This is a
 *       failure, not a warning: otherwise the type's sign-up endpoint would accept subscribers who never
 *       receive anything.</li>
 * </ul>
 * The enabled keys of each registry are logged at INFO level (keys only).
 *
 * <p><b>Plugin module convention</b> (for {@code source-*} and {@code channel-*} modules):
 * <ul>
 *   <li>one {@code @AutoConfiguration} class per module that declares the plugin beans, listed in
 *       {@code src/main/resources/META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports};</li>
 *   <li>the class is annotated with {@code @ConditionalOnBooleanProperty("alerting.sources.<key>.enabled")}
 *       or {@code @ConditionalOnBooleanProperty("alerting.channels.<key>.enabled")}, so a plugin is off
 *       unless enabled; a channel module's subscriber type shares its channel's flag;</li>
 *   <li>the module's settings are a validated {@code @ConfigurationProperties} class under the same
 *       prefix, registered by that auto-configuration;</li>
 *   <li>the module depends on {@code alerting-spi} and Spring Boot's auto-configuration support, never on
 *       {@code alerting-app}.</li>
 * </ul>
 * Disabling a plugin therefore removes its beans, and with them its registry entry, without a code
 * change. The test plugins in {@code com.sonrisa.alerting.testplugin} (test sources) follow this
 * convention.
 */
package com.sonrisa.alerting.app.plugin;
