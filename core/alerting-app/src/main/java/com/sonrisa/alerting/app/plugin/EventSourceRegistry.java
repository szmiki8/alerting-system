package com.sonrisa.alerting.app.plugin;

import com.sonrisa.alerting.spi.source.EventSource;
import java.util.List;

/** The enabled {@link EventSource}s by key. */
public final class EventSourceRegistry extends PluginRegistry<EventSource> {

    public EventSourceRegistry(List<? extends EventSource> sources) {
        super("event source", sources, EventSource::key);
    }
}
