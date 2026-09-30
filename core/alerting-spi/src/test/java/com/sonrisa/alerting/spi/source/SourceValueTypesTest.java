package com.sonrisa.alerting.spi.source;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class SourceValueTypesTest {

    private static final Instant AT = Instant.parse("2026-09-30T10:00:00Z");

    @Test
    void draftWithAllFieldsIsAccepted() {
        EventDraft draft = new EventDraft(AT, "Title", "Content", "BBC News",
                URI.create("https://example.org/a"), "https://example.org/a");

        assertThat(draft.title()).isEqualTo("Title");
        assertThat(draft.link()).hasToString("https://example.org/a");
    }

    @Test
    void draftWithoutLinkAndIdentityAndWithEmptyContentIsAccepted() {
        EventDraft draft = new EventDraft(AT, "Title", "", "Source");

        assertThat(draft.link()).isNull();
        assertThat(draft.identity()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\t"})
    void draftWithoutTitleIsRejected(String title) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDraft(AT, title, "Content", "Source"))
                .withMessageContaining("title");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" "})
    void draftWithoutSourceNameIsRejected(String sourceName) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDraft(AT, "Title", "Content", sourceName))
                .withMessageContaining("sourceName");
    }

    @Test
    void draftWithoutTimeOrContentIsRejected() {
        assertThatNullPointerException().isThrownBy(() -> new EventDraft(null, "Title", "Content", "Source"));
        assertThatNullPointerException().isThrownBy(() -> new EventDraft(AT, "Title", null, "Source"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/relative/path", "ftp://example.org/a", "javascript:alert(1)", "mailto:a@b.c"})
    void draftWithNonHttpLinkIsRejected(String link) {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDraft(AT, "Title", "Content", "Source", URI.create(link), null))
                .withMessageContaining("link");
    }

    @Test
    void draftWithBlankIdentityIsRejected() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new EventDraft(AT, "Title", "Content", "Source", null, " "))
                .withMessageContaining("identity");
    }

    @Test
    void fetchContextNeedsAtLeastOneEvent() {
        assertThatIllegalArgumentException().isThrownBy(() -> new FetchContext(null, 0));

        FetchContext first = new FetchContext(null, 10);
        assertThat(first.lastSuccess()).isEmpty();
        assertThat(new FetchContext(AT, 1).lastSuccess()).contains(AT);
    }

    @Test
    void failureCarriesItsClassification() {
        EventSourceException transientFailure = EventSourceException.transientFailure("HTTP 503", null);
        EventSourceException permanentFailure = EventSourceException.permanentFailure("apiKeyInvalid", null);

        assertThat(transientFailure.kind()).isEqualTo(EventSourceException.Kind.TRANSIENT);
        assertThat(transientFailure.isTransient()).isTrue();
        assertThat(permanentFailure.kind()).isEqualTo(EventSourceException.Kind.PERMANENT);
        assertThat(permanentFailure.isTransient()).isFalse();
    }

    @Test
    void aSourceCanBeImplementedWithTheInterfaceAlone() throws EventSourceException {
        EventSource source = new EventSource() {
            @Override
            public String key() {
                return "fixed";
            }

            @Override
            public List<EventDraft> fetchNewItems(FetchContext context) {
                return List.of(new EventDraft(AT, "Title", "Content", "Fixed"));
            }
        };

        assertThat(source.fetchNewItems(new FetchContext(null, 10))).hasSize(1);
    }
}
