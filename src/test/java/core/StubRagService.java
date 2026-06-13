package core;

import rag.RagService;
import tables.Book;

import java.util.ArrayList;
import java.util.List;

/**
 * Minimal stub {@link RagService} for tests that exercise {@link LibraryServiceImpl}.
 *
 * <p>Captures the last {@code ingest} call so tests can verify the delegation
 * without requiring a live model or embedding service.
 */
public class StubRagService implements RagService {

    private List<Book> lastIngestedBooks = null;
    private String lastQuestion = null;
    private AnswerWithSources stubbedAnswer = new AnswerWithSources("stub answer", List.of("stub source"));

    /** Configures the answer returned by {@link #ask(String)}. */
    public void setAnswer(AnswerWithSources answer) {
        this.stubbedAnswer = answer;
    }

    @Override
    public void ingest(List<Book> books) {
        this.lastIngestedBooks = new ArrayList<>(books != null ? books : List.of());
    }

    @Override
    public AnswerWithSources ask(String question) {
        this.lastQuestion = question;
        return stubbedAnswer;
    }

    public List<Book> getLastIngestedBooks() {
        return lastIngestedBooks;
    }

    public String getLastQuestion() {
        return lastQuestion;
    }
}
