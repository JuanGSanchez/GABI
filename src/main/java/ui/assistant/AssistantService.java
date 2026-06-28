package ui.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The in-app AI assistant engine (SPEC-02): provider-agnostic, streaming, multi-turn,
 * context-aware, and fail-soft. It is Swing-free so it is fully unit-testable with a
 * stub {@link ChatBackend}.
 *
 * <p>Responsibilities:
 * <ul>
 *   <li><b>Conversation memory</b> — keeps the multi-turn history for the session;
 *       {@link #clear()} resets it.</li>
 *   <li><b>Context injection</b> — when enabled, prepends a secret-free app-state snapshot
 *       (from {@link AppStateProvider}) so the model can reason about current data.</li>
 *   <li><b>Streaming</b> — forwards backend chunks to the view as they arrive, accumulating
 *       the assistant turn and committing it to memory on completion.</li>
 *   <li><b>Graceful failure</b> — a provider error / timeout / rate-limit is logged and
 *       surfaced to the view as a readable message; it never throws to the caller and
 *       never commits a partial assistant turn.</li>
 *   <li><b>Runtime provider swap</b> — {@link #setBackend(ChatBackend)} re-routes the next
 *       message through a new provider with no recompile.</li>
 * </ul>
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public class AssistantService {

    private static final Logger log = LoggerFactory.getLogger(AssistantService.class);

    private final List<ChatMessage> conversation = new ArrayList<>();
    private ChatBackend backend;
    private final transient AppStateProvider contextProvider;
    private boolean injectContext;

    public AssistantService(ChatBackend backend, AppStateProvider contextProvider, boolean injectContext) {
        this.backend = backend;
        this.contextProvider = contextProvider == null ? AppStateProvider.none() : contextProvider;
        this.injectContext = injectContext;
    }

    /** Swaps the provider backend at runtime (next message uses it). */
    public synchronized void setBackend(ChatBackend backend) {
        this.backend = backend;
    }

    /** The active provider label for the UI. */
    public synchronized String providerName() {
        return backend.providerName();
    }

    /** Whether the active backend can reach a provider. */
    public synchronized boolean isReady() {
        return backend.isReady();
    }

    public synchronized void setInjectContext(boolean injectContext) {
        this.injectContext = injectContext;
    }

    public synchronized boolean isInjectContext() {
        return injectContext;
    }

    /** An unmodifiable snapshot of the conversation so far (for the view and tests). */
    public synchronized List<ChatMessage> conversation() {
        return Collections.unmodifiableList(new ArrayList<>(conversation));
    }

    /** Clears the multi-turn memory, starting a fresh session. */
    public synchronized void clear() {
        conversation.clear();
    }

    /**
     * Sends a user message and streams the assistant's reply to {@code view}.
     *
     * @param userText the user's message (non-blank)
     * @param view     receives streamed chunks then a terminal signal; never receives a
     *                 raw stack trace
     */
    public void send(String userText, ChatStreamHandler view) {
        if (userText == null || userText.isBlank()) {
            return;
        }
        final ChatBackend active;
        final List<ChatMessage> turnHistory;
        final String context;
        synchronized (this) {
            conversation.add(ChatMessage.user(userText));
            active = backend;
            turnHistory = new ArrayList<>(conversation);
            context = injectContext ? safeContext() : "";
        }

        StringBuilder assembled = new StringBuilder();
        ChatStreamHandler wrapped = new ChatStreamHandler() {
            @Override
            public void onChunk(String chunk) {
                if (chunk != null) {
                    assembled.append(chunk);
                }
                view.onChunk(chunk);
            }

            @Override
            public void onComplete() {
                synchronized (AssistantService.this) {
                    conversation.add(ChatMessage.assistant(assembled.toString()));
                }
                view.onComplete();
            }

            @Override
            public void onError(Throwable error) {
                log.warn("Assistant provider error: {}", error.toString());
                // The partial assistant turn is discarded (not committed to memory).
                view.onError(error);
            }
        };

        try {
            active.stream(turnHistory, context, wrapped);
        } catch (RuntimeException e) {
            // Synchronous failure from the backend — degrade gracefully, do not propagate.
            log.warn("Assistant call failed synchronously: {}", e.toString());
            view.onError(e);
        }
    }

    private String safeContext() {
        try {
            String snap = contextProvider.snapshot();
            return snap == null ? "" : snap;
        } catch (RuntimeException e) {
            log.debug("Context snapshot failed: {}", e.getMessage());
            return "";
        }
    }
}
