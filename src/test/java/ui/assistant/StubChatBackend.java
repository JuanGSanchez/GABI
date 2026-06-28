package ui.assistant;

import java.util.ArrayList;
import java.util.List;

/**
 * Configurable in-memory {@link ChatBackend} test double (SPEC-02). It makes NO network
 * call: it replays preset chunks (or simulates an error) synchronously, and records the
 * conversation and context it was handed so tests can assert injection and memory.
 */
class StubChatBackend implements ChatBackend {

    private final String name;
    private final List<String> chunks;
    private final RuntimeException failure;
    private final boolean throwSynchronously;

    String lastContext;
    List<ChatMessage> lastConversation;

    private StubChatBackend(String name, List<String> chunks, RuntimeException failure, boolean throwSync) {
        this.name = name;
        this.chunks = chunks;
        this.failure = failure;
        this.throwSynchronously = throwSync;
    }

    static StubChatBackend emitting(String name, String... chunks) {
        return new StubChatBackend(name, List.of(chunks), null, false);
    }

    static StubChatBackend failingViaHandler(String name, RuntimeException ex) {
        return new StubChatBackend(name, List.of(), ex, false);
    }

    static StubChatBackend throwingSynchronously(String name, RuntimeException ex) {
        return new StubChatBackend(name, List.of(), ex, true);
    }

    @Override
    public String providerName() {
        return name;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void stream(List<ChatMessage> conversation, String context, ChatStreamHandler handler) {
        this.lastContext = context;
        this.lastConversation = new ArrayList<>(conversation);
        if (throwSynchronously) {
            throw failure;
        }
        if (failure != null) {
            handler.onError(failure);
            return;
        }
        for (String c : chunks) {
            handler.onChunk(c);
        }
        handler.onComplete();
    }
}
