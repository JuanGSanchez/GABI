package ui.assistant;

import core.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * Spring wiring for the in-app AI assistant (SPEC-02), active only under the {@code desktop}
 * profile (the UI host). It selects the provider-agnostic backend at startup:
 * <ul>
 *   <li>a {@link SpringAiChatBackend} when a {@code ChatClient.Builder} is available and the
 *       assistant is enabled — the real, model-agnostic outbound path;</li>
 *   <li>a {@link NoOpChatBackend} otherwise — graceful "configure provider" degradation.</li>
 * </ul>
 *
 * <p>No credential is read here: API keys flow into the {@code ChatClient} from
 * {@code ${OPENAI_API_KEY}} / {@code ${ANTHROPIC_API_KEY}} placeholders (SPEC-08). The
 * provider/model/flags come from {@code gabi.assistant.*} runtime config.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
@Configuration
@Profile("desktop")
public class AssistantConfig {

    private static final Logger log = LoggerFactory.getLogger(AssistantConfig.class);

    @Bean
    public AssistantProperties assistantProperties(
            @Value("${gabi.assistant.provider:ollama}") String provider,
            @Value("${gabi.assistant.model:llama3.2}") String model,
            @Value("${gabi.assistant.enabled:true}") boolean enabled,
            @Value("${gabi.assistant.inject-context:true}") boolean injectContext) {
        return new AssistantProperties(provider, model, enabled, injectContext);
    }

    @Bean
    public AssistantService assistantService(
            ObjectProvider<ChatClient.Builder> chatClientBuilders,
            AssistantProperties props,
            LibraryService libraryService) {

        ChatClient.Builder builder = chatClientBuilders.getIfAvailable();
        ChatBackend backend;
        if (props.isEnabled() && builder != null) {
            backend = new SpringAiChatBackend(builder, props);
            log.info("AI assistant backend: {} (ChatClient)", props.label());
        } else {
            backend = new NoOpChatBackend();
            log.warn("AI assistant backend: none (no ChatClient builder or assistant disabled)");
        }
        return new AssistantService(backend,
                new CatalogueContextProvider(libraryService), props.isInjectContext());
    }
}
