package ui.assistant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;

import java.util.ArrayList;
import java.util.List;

/**
 * Production {@link ChatBackend} backed by a Spring AI {@code ChatClient} (SPEC-02).
 *
 * <p>{@code ChatClient} is model-agnostic: the concrete provider (OpenAI, Anthropic, Azure,
 * Ollama, …) is selected by Spring profile/config and API keys are sourced from
 * {@code ${...}} environment placeholders — never from this class. Responses are streamed
 * token-by-token via the reactive {@code stream().content()} {@code Flux<String>}.
 *
 * <p>This is the outbound, ChatClient-only path of SPEC-02; it shares no transport with the
 * MCP server (SPEC-06) and never calls an MCP endpoint.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public class SpringAiChatBackend implements ChatBackend {

    private static final Logger log = LoggerFactory.getLogger(SpringAiChatBackend.class);

    private final ChatClient chatClient;
    private final String label;

    public SpringAiChatBackend(ChatClient.Builder builder, AssistantProperties props) {
        this.chatClient = builder.build();
        this.label = props.label();
    }

    @Override
    public String providerName() {
        return label;
    }

    @Override
    public boolean isReady() {
        return true;
    }

    @Override
    public void stream(List<ChatMessage> conversation, String context, ChatStreamHandler handler) {
        try {
            List<Message> messages = toSpringMessages(conversation, context);
            chatClient.prompt()
                    .messages(messages)
                    .stream()
                    .content()
                    .subscribe(
                            handler::onChunk,
                            handler::onError,
                            handler::onComplete);
        } catch (RuntimeException e) {
            log.warn("ChatClient stream failed to start: {}", e.toString());
            handler.onError(e);
        }
    }

    private List<Message> toSpringMessages(List<ChatMessage> conversation, String context) {
        List<Message> messages = new ArrayList<>();
        if (context != null && !context.isBlank()) {
            messages.add(new SystemMessage(
                    "Use this current application context where relevant:\n" + context));
        }
        for (ChatMessage m : conversation) {
            switch (m.role()) {
                case USER -> messages.add(new UserMessage(m.content()));
                case ASSISTANT -> messages.add(new AssistantMessage(m.content()));
                case SYSTEM -> messages.add(new SystemMessage(m.content()));
                default -> messages.add(new UserMessage(m.content()));
            }
        }
        return messages;
    }
}
