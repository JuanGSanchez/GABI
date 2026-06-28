package ui.assistant;

import java.util.List;

/**
 * Fallback {@link ChatBackend} used when no AI provider is configured/enabled (SPEC-02).
 *
 * <p>Mirrors the RAG {@code NoOpRagService} graceful-degradation pattern: instead of
 * crashing when there is no model or API key, it streams a single clear "configure
 * provider" message so the panel stays usable and the app responsive.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public class NoOpChatBackend implements ChatBackend {

    private static final String MESSAGE =
            "No AI provider is configured. Choose a provider/model and supply its API key "
            + "via environment configuration (e.g. OPENAI_API_KEY or ANTHROPIC_API_KEY), "
            + "or run a local Ollama model, then reopen the assistant.";

    @Override
    public String providerName() {
        return "none";
    }

    @Override
    public boolean isReady() {
        return false;
    }

    @Override
    public void stream(List<ChatMessage> conversation, String context, ChatStreamHandler handler) {
        handler.onChunk(MESSAGE);
        handler.onComplete();
    }
}
