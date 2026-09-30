package com.sonrisa.alerting.app.plugin;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * The enabled plugins of one extension point, by key. Built once at start-up from all beans of the SPI
 * type; fails fast on an invalid or duplicate key (ADR-10).
 *
 * @param <T> the SPI type
 */
public abstract class PluginRegistry<T> {

    /** Lower-case letters, digits and hyphens, starting with a letter (see the SPI package docs). */
    static final Pattern KEY_PATTERN = Pattern.compile("[a-z][a-z0-9-]*");

    private final String kind;
    private final Map<String, T> byKey;

    /**
     * @param kind what the plugins are, for messages, for example "notification channel"
     * @param plugins all beans of the SPI type
     * @param keyOf reads the key of a plugin
     * @throws PluginConfigurationException on an invalid or duplicate key
     */
    protected PluginRegistry(String kind, List<? extends T> plugins, Function<? super T, String> keyOf) {
        this.kind = kind;
        Map<String, T> map = new TreeMap<>();
        for (T plugin : plugins) {
            String key = keyOf.apply(plugin);
            if (key == null || !KEY_PATTERN.matcher(key).matches()) {
                throw new PluginConfigurationException(String.format(
                        "The %s %s has the invalid key '%s'. A key consists of lower-case letters, digits and"
                                + " hyphens and starts with a letter.", kind, plugin.getClass().getName(), key));
            }
            T previous = map.putIfAbsent(key, plugin);
            if (previous != null) {
                throw new PluginConfigurationException(String.format(
                        "Two %ss have the key '%s': %s and %s. Keys must be unique; disable one of them.",
                        kind, key, previous.getClass().getName(), plugin.getClass().getName()));
            }
        }
        this.byKey = Collections.unmodifiableMap(map);
    }

    /** The plugin with this key, if it is enabled. */
    public Optional<T> find(String key) {
        return Optional.ofNullable(byKey.get(key));
    }

    /**
     * The plugin with this key.
     *
     * @throws IllegalArgumentException if no enabled plugin has this key
     */
    public T get(String key) {
        T plugin = byKey.get(key);
        if (plugin == null) {
            throw new IllegalArgumentException("No enabled " + kind + " with key '" + key + "'");
        }
        return plugin;
    }

    /** Whether an enabled plugin has this key. */
    public boolean contains(String key) {
        return byKey.containsKey(key);
    }

    /** The keys of all enabled plugins, sorted. */
    public Set<String> keys() {
        return byKey.keySet();
    }

    /** All enabled plugins, sorted by key. */
    public Collection<T> all() {
        return byKey.values();
    }
}
