package com.sonrisa.alerting.app.plugin;

/**
 * The enabled plugins do not fit together (duplicate key, invalid key, subscriber type without its
 * channel). Thrown at start-up; {@link PluginConfigurationFailureAnalyzer} turns it into a clear report.
 */
public class PluginConfigurationException extends IllegalStateException {

    private static final long serialVersionUID = 1L;

    public PluginConfigurationException(String message) {
        super(message);
    }
}
