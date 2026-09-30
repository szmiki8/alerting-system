package com.sonrisa.alerting.app.plugin;

import org.springframework.boot.diagnostics.AbstractFailureAnalyzer;
import org.springframework.boot.diagnostics.FailureAnalysis;

/**
 * Shows a {@link PluginConfigurationException} as a short "APPLICATION FAILED TO START" report instead
 * of a long bean-creation stack trace. Registered in {@code META-INF/spring.factories}.
 */
public class PluginConfigurationFailureAnalyzer extends AbstractFailureAnalyzer<PluginConfigurationException> {

    @Override
    protected FailureAnalysis analyze(Throwable rootFailure, PluginConfigurationException cause) {
        return new FailureAnalysis(
                "Invalid plugin configuration: " + cause.getMessage(),
                "Enable or disable plugins with alerting.sources.<key>.enabled and"
                        + " alerting.channels.<key>.enabled so that every key is unique and every enabled"
                        + " subscriber type has its channel.",
                cause);
    }
}
