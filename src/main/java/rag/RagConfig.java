package rag;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.File;

/**
 * Spring AI bean configuration for the GABI RAG pipeline.
 *
 * <p><b>SimpleVectorStore</b> — in-process, in-memory, backed by Spring AI's built-in
 * implementation. Constructed from the active {@link EmbeddingModel} bean.
 * Optional file persistence: if {@code gabi.vectorstore.file} is set, the JSON
 * snapshot is loaded at startup and saved after ingest.
 *
 * <p><b>Provider selection</b> — achieved through Spring profiles declared in
 * application.yml / profile-specific yml files.  The starters added to the pom
 * (spring-ai-starter-model-ollama, -openai, -anthropic) each auto-configure a
 * {@code ChatModel} and/or {@code EmbeddingModel} bean when their profile is active.
 * Spring AI's auto-configuration already honours {@code spring.profiles.active};
 * no extra {@code @ConditionalOnProfile} is needed here because the starters are
 * classpath-present but profile-config-gated via the YAML files.
 *
 * <p><b>Build-time safety</b> — the {@code SimpleVectorStore} bean construction
 * does NOT require a live Ollama/OpenAI endpoint; it is purely in-memory. The
 * {@code EmbeddingModel} dependency may fail to start if no model server is running,
 * so we guard startup with lazy initialisation where appropriate and let the
 * {@link RagServiceImpl} catch and log errors gracefully.
 *
 * @author JuanGS (workstream b — RAG core)
 */
@Configuration
public class RagConfig {

    private static final Logger log = LoggerFactory.getLogger(RagConfig.class);

    /**
     * Builds the in-memory {@code SimpleVectorStore}.
     *
     * <p>If {@code gabi.vectorstore.file} is a non-blank path and the file exists,
     * the prior index is loaded from JSON. This gives durable, infra-free persistence
     * across restarts without requiring an external vector database.
     *
     * @param embeddingModel the active embedding model (provided by whichever Spring AI
     *                       model starter is on the classpath and active via profile)
     * @param filePath       optional path from {@code gabi.vectorstore.file}; empty string
     *                       means in-memory only (default)
     * @return a ready-to-use {@code SimpleVectorStore}
     */
    @Bean
    public SimpleVectorStore simpleVectorStore(
            EmbeddingModel embeddingModel,
            @Value("${gabi.vectorstore.file:}") String filePath) {

        SimpleVectorStore store = SimpleVectorStore.builder(embeddingModel).build();

        if (filePath != null && !filePath.isBlank()) {
            File f = new File(filePath);
            if (f.exists()) {
                try {
                    store.load(f);
                    log.info("Vector store loaded from: {}", filePath);
                } catch (Exception e) {
                    log.warn("Could not load vector store from '{}': {} — starting empty.", filePath, e.getMessage());
                }
            } else {
                log.info("Vector store file not found at '{}' — starting with empty index.", filePath);
            }
        }

        return store;
    }
}
