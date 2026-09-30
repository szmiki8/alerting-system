package com.sonrisa.alerting.app.config;

/** Helpers that keep secret values out of logs and messages. */
final class Secrets {

    private Secrets() {
    }

    /** Tells whether a secret is set, never what it is. */
    static String mask(String secret) {
        return secret == null || secret.isBlank() ? "<not set>" : "<set>";
    }
}
