package com.sonrisa.alerting.app.config;

import com.sonrisa.alerting.app.persistence.PostgresTestcontainer;
import java.util.Arrays;
import java.util.stream.Stream;

/** Command-line arguments for tests that start the real application with a deployed profile. */
public final class DeployedProfileArguments {

    /** Test-only encryption key (all zero bytes) in the form {@code <key-id>:<base64 of 32 bytes>}. */
    public static final String TEST_ENCRYPTION_KEY = "test:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    /** Test-only fingerprint key (32 zero bytes, base64). */
    public static final String TEST_FINGERPRINT_KEY = "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=";

    /** The keys the deployed profiles require (BE-09). */
    public static final String[] KEYS = {
        "--alerting.security.encryption-key=" + TEST_ENCRYPTION_KEY,
        "--alerting.security.fingerprint-key=" + TEST_FINGERPRINT_KEY};

    private DeployedProfileArguments() {
    }

    /**
     * Adds a reachable database for the {@code postgres} profile (the shared Testcontainers PostgreSQL); the other
     * profiles use H2 in-memory and need nothing extra.
     */
    static String[] withDatabase(String profile, String... args) {
        if (!"postgres".equals(profile)) {
            return args;
        }
        return concat(args, PostgresTestcontainer.datasourceArguments());
    }

    static String[] concat(String[] first, String... second) {
        return Stream.concat(Arrays.stream(first), Arrays.stream(second)).toArray(String[]::new);
    }
}
