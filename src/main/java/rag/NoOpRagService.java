package rag;

import core.AnswerWithSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import tables.Book;

import java.util.List;

/**
 * No-op fallback {@link RagService} that activates when no {@code EmbeddingModel}
 * or {@code ChatModel} bean is present on the classpath / in the context.
 *
 * <p>This allows {@code mvn package} and a minimal Spring context to build and start
 * without requiring a live Ollama endpoint or API key.  The active profile starters
 * (spring-ai-starter-model-ollama etc.) will supply a real {@link RagServiceImpl}
 * when an embedding model is available; this bean is then displaced by the higher
 * priority {@code @Primary} on {@code RagServiceImpl}.
 *
 * <p>All methods log a warning and return safe empty/default values so the
 * rest of the application (CLI, access layer) continues to operate.
 *
 * @author JuanGS (workstream b — RAG build safety)
 */
@Service
@ConditionalOnMissingBean(name = "ragServiceImpl")
public class NoOpRagService implements RagService {

    private static final Logger log = LoggerFactory.getLogger(NoOpRagService.class);

    @Override
    public void ingest(List<Book> books) {
        log.warn("RAG: no-op ingest called — no AI model configured. " +
                 "Activate the 'ollama' (default), 'openai', or 'anthropic' profile.");
    }

    @Override
    public AnswerWithSources ask(String question) {
        log.warn("RAG: no-op ask called — no AI model configured.");
        return new AnswerWithSources(
                "RAG is not configured. Start Ollama or set an API key and activate the " +
                "'openai' or 'anthropic' profile via -Dspring.profiles.active=openai.",
                List.of());
    }
}
