package rag;

import core.AnswerWithSources;
import tables.Book;

import java.util.List;

/**
 * RAG (Retrieval-Augmented Generation) service interface.
 *
 * <p>Encapsulates the full ingestion and query pipeline over the
 * GABI library catalogue. Implementations are Spring-managed beans.
 *
 * <p>The interface is separated from the implementation so:
 * <ul>
 *   <li>Tests can inject a stub/no-op {@code RagService}.</li>
 *   <li>{@link core.LibraryServiceImpl} can depend on it without importing
 *       Spring AI types into the core service.</li>
 *   <li>The active provider (Ollama / OpenAI / Anthropic) is hidden behind
 *       the interface — only {@code RagServiceImpl} knows the Spring AI wiring.</li>
 * </ul>
 *
 * @author JuanGS (workstream b — RAG core)
 */
public interface RagService {

    /**
     * Ingests the given book list into the vector store.
     *
     * <p>Each book is projected into a {@code Document} string, chunked with
     * {@code TokenTextSplitter}, embedded, and added to the {@code SimpleVectorStore}.
     * The store is optionally persisted to file after ingestion.
     *
     * @param books the live catalogue rows to index; may be empty (clears the index)
     */
    void ingest(List<Book> books);

    /**
     * Answers a natural-language question over the indexed catalogue.
     *
     * @param question non-blank question text
     * @return the grounded answer and the source chunks used as context
     */
    AnswerWithSources ask(String question);
}
