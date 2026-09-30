package com.sonrisa.alerting.app.config;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Finds secret-like configuration keys that have a literal value in committed configuration files.
 * Allowed values for a secret key: empty, or a placeholder such as {@code ${ENV_NAME}} with no default
 * (or an empty default).
 */
final class SecretScanner {

    /** Key names that hold secrets, in kebab, camel, snake or upper-case environment style. */
    private static final Pattern SECRET_KEY = Pattern.compile(
            "(?i).*(password|passwd|secret|api[-_.]?key|token|private[-_.]?key|encryption[-_.]?key"
                    + "|fingerprint[-_.]?key|credentials?|access[-_.]?key)$");

    /** {@code key: value} (YAML) or {@code key=value} (properties, .env). */
    private static final Pattern ENTRY = Pattern.compile("^\\s*-?\\s*([A-Za-z0-9_.\\-\\[\\]]+)\\s*[:=]\\s*(.*?)\\s*$");

    private static final Pattern ALLOWED_VALUE = Pattern.compile("^(|\"\"|''|\\$\\{[A-Za-z0-9_.\\-]+:?})$");

    private SecretScanner() {
    }

    record Finding(Path file, int line, String key) {

        @Override
        public String toString() {
            // Never print the value itself.
            return file + ":" + line + " key '" + key + "' has a literal value";
        }
    }

    /** Committed configuration files: main resources of every module and the environment example. */
    static List<Path> configurationFiles(Path coreDir) {
        try (Stream<Path> paths = Files.walk(coreDir)) {
            return paths
                    .filter(Files::isRegularFile)
                    .filter(path -> !path.toString().contains("/build/") && !path.toString().contains("/.gradle/"))
                    .filter(path -> {
                        String name = path.getFileName().toString();
                        boolean mainResource = path.toString().contains("/src/main/resources/")
                                && (name.endsWith(".yaml") || name.endsWith(".yml") || name.endsWith(".properties"));
                        return mainResource || name.equals(".env.example");
                    })
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Finding> scan(Path file) {
        try {
            return scan(file, Files.readAllLines(file));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    static List<Finding> scan(Path file, List<String> lines) {
        List<Finding> findings = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String line = stripComment(lines.get(i));
            Matcher entry = ENTRY.matcher(line);
            if (entry.matches() && SECRET_KEY.matcher(entry.group(1)).matches()
                    && !ALLOWED_VALUE.matcher(entry.group(2)).matches()) {
                findings.add(new Finding(file, i + 1, entry.group(1)));
            }
        }
        return findings;
    }

    private static String stripComment(String line) {
        String trimmed = line.stripLeading();
        if (trimmed.startsWith("#") || trimmed.startsWith("!")) {
            return "";
        }
        int comment = line.indexOf(" #");
        return comment >= 0 ? line.substring(0, comment) : line;
    }
}
