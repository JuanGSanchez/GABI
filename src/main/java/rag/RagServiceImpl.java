package rag;

import core.AnswerWithSources;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.SimpleVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tables.Book;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Spring AI implementation of the RAG pipeline for the GABI library catalogue.
 *
 * <p><b>Pipeline:</b>
 * <ol>
 *   <li><b>Ingest:</b> project Derby book rows → {@code Document}s → chunk with
 *       {@code TokenTextSplitter} → embed via the active {@code EmbeddingModel}
 *       (Ollama {@code nomic-embed-text} by default) → store in
 *       {@code SimpleVectorStore} → optionally persist to file.</li>
 *   <li><b>Query:</b> {@code ChatClient} with {@code QuestionAnswerAdvisor} over the
 *       vector store → similarity search → context injection → grounded answer.</li>
 * </ol>
 *
 * <p><b>Provider selection:</b> achieved through Spring profiles. Active profile
 * determines which {@code ChatModel} and {@code EmbeddingModel} beans are created
 * (see {@link RagConfig}):
 * <ul>
 *   <li>(default, no profile) — Ollama; no API key needed.</li>
 *   <li>{@code openai} — OpenAI ({@code OPENAI_API_KEY} env var).</li>
 *   <li>{@code anthropic} — Anthropic chat + Ollama embeddings ({@code ANTHROPIC_API_KEY}).</li>
 * </ul>
 *
 * <p>The {@code SimpleVectorStore} is in-process / in-memory. If
 * {@code gabi.vectorstore.file} is configured, the store is loaded at startup
 * and saved after each ingest call.
 *
 * @author JuanGS (workstream b — RAG core)
 */
@Service
public class RagServiceImpl implements RagService {

    private static final Logger log = LoggerFactory.getLogger(RagServiceImpl.class);

    private static final int TOP_K               = 5;
    private static final double SIMILARITY_THRESHOLD = 0.5;

    private final SimpleVectorStore vectorStore;
    private final ChatClient chatClient;

    @Value("${gabi.vectorstore.file:}")
    private String vectorStoreFilePath;

    public RagServiceImpl(SimpleVectorStore vectorStore, ChatClient.Builder chatClientBuilder) {
        this.vectorStore = vectorStore;
        this.chatClient  = chatClientBuilder.build();
    }

    // =========================================================================
    // INGEST
    // =========================================================================

    @Override
    public void ingest(List<Book> books) {
        if (books == null || books.isEmpty()) {
            log.info("RAG ingest: no books to index — vector store unchanged");
            return;
        }

        // Project each book row into a Document
        List<Document> docs = books.stream()
                .map(this::bookToDocument)
                .collect(Collectors.toList());

        // Chunk with TokenTextSplitter (token-aware; preserves sentence boundaries)
        List<Document> chunks = new TokenTextSplitter().apply(docs);

        // Embed and store
        vectorStore.add(chunks);
        log.info("RAG ingest: added {} chunks from {} books to vector store", chunks.size(), books.size());

        // Persist to file if configured
        persistStore();
    }

    // =========================================================================
    // ASK
    // =========================================================================

    @Override
    public AnswerWithSources ask(String question) {
        // QuestionAnswerAdvisor performs similarity search and injects context into the prompt
        QuestionAnswerAdvisor advisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(TOP_K)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .build();

        String answer;
        try {
            answer = chatClient.prompt()
                    .advisors(advisor)
                    .user(question)
                    .call()
                    .content();
        } catch (Exception e) {
            log.warn("RAG ask failed — no model available or error: {}", e.getMessage());
            // Graceful degradation: return an informative message rather than throwing
            return new AnswerWithSources(
                    "RAG service is not available (no model running). " +
                    "Start Ollama with 'ollama run llama3.2' or set an API key via OPENAI_API_KEY.",
                    List.of());
        }

        // Retrieve the same top-k documents for source citation
        List<String> sources = retrieveSources(question);

        return new AnswerWithSources(
                answer != null ? answer : "",
                sources);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Document bookToDocument(Book book) {
        String content = String.format(
                "Book ID: %d | Title: %s | Author: %s | Status: %s",
                book.getID(),
                book.getTitle(),
                book.getAuthor(),
                book.isLent() ? "Currently lent out" : "Available");
        return Document.builder()
                .text(content)
                .metadata(Map.of(
                        "source", "library-catalogue",
                        "bookId", String.valueOf(book.getID()),
                        "title",  book.getTitle(),
                        "author", book.getAuthor()))
                .build();
    }

    private List<String> retrieveSources(String question) {
        try {
            return vectorStore.similaritySearch(
                            SearchRequest.builder()
                                    .query(question)
                                    .topK(TOP_K)
                                    .similarityThreshold(SIMILARITY_THRESHOLD)
                                    .build())
                    .stream()
                    .map(Document::getText)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.debug("Source retrieval failed: {}", e.getMessage());
            return List.of();
        }
    }

    private void persistStore() {
        if (vectorStoreFilePath != null && !vectorStoreFilePath.isBlank()) {
            try {
                File f = new File(vectorStoreFilePath);
                f.getParentFile().mkdirs();
                vectorStore.save(f);
                log.info("Vector store persisted to: {}", vectorStoreFilePath);
            } catch (Exception e) {
                log.warn("Failed to persist vector store to '{}': {}", vectorStoreFilePath, e.getMessage());
            }
        }
    }
}
