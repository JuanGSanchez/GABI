package ui.assistant;

import java.util.List;

/**
 * Provider-agnostic chat backend SPI (SPEC-02).
 *
 * <p>This is the seam that makes the in-app assistant <b>model-agnostic</b>: the Swing
 * panel and the {@link AssistantService} talk only to this interface, never to a concrete
 * provider. The production implementation ({@code SpringAiChatBackend}) wraps a Spring AI
 * {@code ChatClient}, which is itself model-agnostic across OpenAI / Anthropic / Azure /
 * Ollama; a no-op implementation covers the "no provider configured" case; tests inject a
 * stub. Swapping the backend at runtime re-routes the next message through a new provider
 * with no recompile.
 *
 * <p><b>Separation from MCP (SPEC-06):</b> this path is strictly outbound to an AI provider
 * via {@code ChatClient}. It shares no transport or endpoint with the MCP server, which
 * exposes GABI <em>to</em> external models. An implementation here must not call the MCP
 * server.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public interface ChatBackend {

    /** Human-readable provider/model label shown in the UI (e.g. "openai:gpt-4o-mini"). */
    String providerName();

    /** Whether this backend can actually reach a provider (false ⇒ "configure provider"). */
    boolean isReady();

    /**
     * Streams a response for the given conversation.
     *
     * @param conversation the full multi-turn history (oldest first), the last entry being
     *                     the new user message
     * @param context      an optional app-state context snapshot to inject (may be null/blank);
     *                     guaranteed by the caller to carry no secret
     * @param handler      receives streamed chunks then exactly one terminal signal
     */
    void stream(List<ChatMessage> conversation, String context, ChatStreamHandler handler);
}
