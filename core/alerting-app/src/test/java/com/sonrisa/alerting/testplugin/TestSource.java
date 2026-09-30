package com.sonrisa.alerting.testplugin;

import com.sonrisa.alerting.spi.source.EventDraft;
import com.sonrisa.alerting.spi.source.EventSource;
import com.sonrisa.alerting.spi.source.FetchContext;
import java.util.List;

/** Minimal test source; returns nothing. */
public class TestSource implements EventSource {

    private final String key;

    public TestSource(String key) {
        this.key = key;
    }

    @Override
    public String key() {
        return key;
    }

    @Override
    public List<EventDraft> fetchNewItems(FetchContext context) {
        return List.of();
    }
}
