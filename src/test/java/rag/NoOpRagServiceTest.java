package rag;

import core.AnswerWithSources;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Book;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link NoOpRagService} — the fallback RAG implementation.
 *
 * <p>No Spring context; no model server required.
 */
class NoOpRagServiceTest {

    private final NoOpRagService service = new NoOpRagService();

    @Test
    @DisplayName("ingest() completes without throwing when given books")
    void ingest_withBooks_doesNotThrow() {
        List<Book> books = List.of(
                new Book(1, "Dune", "Herbert"),
                new Book(2, "Foundation", "Asimov")
        );
        assertThatCode(() -> service.ingest(books)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ingest() completes without throwing when given empty list")
    void ingest_emptyList_doesNotThrow() {
        assertThatCode(() -> service.ingest(List.of())).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ingest() completes without throwing when given null")
    void ingest_null_doesNotThrow() {
        assertThatCode(() -> service.ingest(null)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("ask() returns a non-null AnswerWithSources with a degradation message")
    void ask_returnsDegradationMessage() {
        AnswerWithSources result = service.ask("What books are available?");
        assertThat(result).isNotNull();
        assertThat(result.answer()).isNotBlank();
        // degradation message should mention RAG configuration
        assertThat(result.answer()).containsIgnoringCase("RAG");
    }

    @Test
    @DisplayName("ask() returns empty sources list")
    void ask_returnsEmptySources() {
        AnswerWithSources result = service.ask("any question");
        assertThat(result.sources()).isEmpty();
    }

    @Test
    @DisplayName("ask() completes without throwing for any non-null question")
    void ask_doesNotThrow() {
        assertThatCode(() -> service.ask("hello")).doesNotThrowAnyException();
    }
}
