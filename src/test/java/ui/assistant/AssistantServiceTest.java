package ui.assistant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.desktop.FakeLibraryService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-02 acceptance for the assistant engine, exercised entirely with stubbed/abstracted
 * providers — NO live network call.
 */
class AssistantServiceTest {

    /** Collects callbacks and records whether completion happened after the last chunk. */
    private static final class RecordingView implements ChatStreamHandler {
        final List<String> chunks = new ArrayList<>();
        boolean completed;
        Throwable error;
        boolean chunksBeforeComplete = true;

        @Override public void onChunk(String c) {
            if (completed) {
                chunksBeforeComplete = false;
            }
            chunks.add(c);
        }
        @Override public void onComplete() { completed = true; }
        @Override public void onError(Throwable e) { error = e; }
    }

    @Test
    @DisplayName("streaming delivers partial chunks before completion and commits the turn")
    void streaming() {
        StubChatBackend backend = StubChatBackend.emitting("stub", "Hel", "lo", "!");
        AssistantService service = new AssistantService(backend, AppStateProvider.none(), false);
        RecordingView view = new RecordingView();

        service.send("hi", view);

        assertThat(view.chunks).containsExactly("Hel", "lo", "!");
        assertThat(view.chunksBeforeComplete).isTrue();
        assertThat(view.completed).isTrue();
        // conversation now holds the user turn + the assembled assistant turn
        List<ChatMessage> convo = service.conversation();
        assertThat(convo).hasSize(2);
        assertThat(convo.get(0).role()).isEqualTo(ChatMessage.Role.USER);
        assertThat(convo.get(1).content()).isEqualTo("Hello!");
    }

    @Test
    @DisplayName("multi-turn memory is preserved across sends and reset by clear()")
    void multiTurnMemory() {
        StubChatBackend backend = StubChatBackend.emitting("stub", "ok");
        AssistantService service = new AssistantService(backend, AppStateProvider.none(), false);

        service.send("first", new RecordingView());
        service.send("second", new RecordingView());
        assertThat(service.conversation()).hasSize(4); // 2 user + 2 assistant
        // the second backend call saw the full prior history
        assertThat(backend.lastConversation).extracting(ChatMessage::content)
                .contains("first", "second");

        service.clear();
        assertThat(service.conversation()).isEmpty();
    }

    @Test
    @DisplayName("provider is swappable at runtime — the next message routes to the new backend")
    void providerSwap() {
        StubChatBackend a = StubChatBackend.emitting("provider-A", "A");
        StubChatBackend b = StubChatBackend.emitting("provider-B", "B");
        AssistantService service = new AssistantService(a, AppStateProvider.none(), false);

        RecordingView v1 = new RecordingView();
        service.send("x", v1);
        assertThat(v1.chunks).containsExactly("A");
        assertThat(service.providerName()).isEqualTo("provider-A");

        service.setBackend(b);
        RecordingView v2 = new RecordingView();
        service.send("y", v2);
        assertThat(v2.chunks).containsExactly("B");
        assertThat(service.providerName()).isEqualTo("provider-B");
    }

    @Test
    @DisplayName("context injection reaches the backend prompt and carries no secret")
    void contextInjection() {
        FakeLibraryService library = new FakeLibraryService();
        library.addBook(1, "Dune", "Herbert");
        library.addMember(1, "Alice", "Smith");
        StubChatBackend backend = StubChatBackend.emitting("stub", "ok");
        AssistantService service = new AssistantService(
                backend, new CatalogueContextProvider(library), true);

        service.send("how many books?", new RecordingView());

        assertThat(backend.lastContext).contains("1 books", "1 members");
        // the snapshot must never leak a credential into the outbound prompt
        assertThat(backend.lastContext.toLowerCase())
                .doesNotContain("password").doesNotContain("api-key").doesNotContain("secret");
    }

    @Test
    @DisplayName("context is omitted when injection is disabled")
    void contextDisabled() {
        FakeLibraryService library = new FakeLibraryService();
        StubChatBackend backend = StubChatBackend.emitting("stub", "ok");
        AssistantService service = new AssistantService(
                backend, new CatalogueContextProvider(library), false);

        service.send("hi", new RecordingView());
        assertThat(backend.lastContext).isEmpty();
    }

    @Test
    @DisplayName("a provider error is surfaced gracefully and the partial turn is not committed")
    void gracefulHandlerError() {
        StubChatBackend backend = StubChatBackend.failingViaHandler(
                "stub", new RuntimeException("HTTP 429 rate limit"));
        AssistantService service = new AssistantService(backend, AppStateProvider.none(), false);
        RecordingView view = new RecordingView();

        assertThatCode(() -> service.send("hi", view)).doesNotThrowAnyException();
        assertThat(view.error).isNotNull();
        assertThat(view.error.getMessage()).contains("429");
        // only the user turn is retained; no assistant turn committed on failure
        assertThat(service.conversation()).hasSize(1);
        assertThat(service.conversation().get(0).role()).isEqualTo(ChatMessage.Role.USER);
    }

    @Test
    @DisplayName("a synchronous backend failure degrades gracefully (no propagation)")
    void gracefulSynchronousError() {
        StubChatBackend backend = StubChatBackend.throwingSynchronously(
                "stub", new IllegalStateException("connection refused"));
        AssistantService service = new AssistantService(backend, AppStateProvider.none(), false);
        AtomicReference<Throwable> seen = new AtomicReference<>();
        AtomicBoolean threw = new AtomicBoolean(false);

        try {
            service.send("hi", new ChatStreamHandler() {
                @Override public void onChunk(String c) { }
                @Override public void onComplete() { }
                @Override public void onError(Throwable e) { seen.set(e); }
            });
        } catch (RuntimeException e) {
            threw.set(true);
        }

        assertThat(threw).isFalse();
        assertThat(seen.get()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("blank input is ignored")
    void blankIgnored() {
        StubChatBackend backend = StubChatBackend.emitting("stub", "ok");
        AssistantService service = new AssistantService(backend, AppStateProvider.none(), false);
        service.send("  ", new RecordingView());
        assertThat(service.conversation()).isEmpty();
    }
}
