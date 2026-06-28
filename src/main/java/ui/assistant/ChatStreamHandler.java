package ui.assistant;

/**
 * Sink for a streaming assistant response (SPEC-02).
 *
 * <p>A {@link ChatBackend} delivers the answer incrementally: zero or more
 * {@link #onChunk(String)} calls as tokens arrive, then exactly one terminal call —
 * either {@link #onComplete()} or {@link #onError(Throwable)}. The handler is how the
 * Swing view renders partial output before the full answer is ready.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public interface ChatStreamHandler {

    /** A partial chunk of the assistant's response. */
    void onChunk(String chunk);

    /** The response completed normally. */
    void onComplete();

    /** The response failed (provider error, timeout, rate limit). Non-fatal to the app. */
    void onError(Throwable error);
}
