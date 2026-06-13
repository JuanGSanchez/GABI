package core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the {@link LibraryException} hierarchy and {@link AnswerWithSources} record.
 *
 * <p>Verifies the exception type hierarchy, message preservation, and the
 * AnswerWithSources canonical constructor guarantees.
 */
class LibraryExceptionTest {

    // =========================================================================
    // Exception hierarchy
    // =========================================================================

    @Test
    @DisplayName("NotFoundException is a LibraryException")
    void notFoundException_isLibraryException() {
        LibraryException.NotFoundException ex = new LibraryException.NotFoundException("not found");
        assertThat(ex).isInstanceOf(LibraryException.class);
        assertThat(ex.getMessage()).isEqualTo("not found");
    }

    @Test
    @DisplayName("BusinessRuleException is a LibraryException")
    void businessRuleException_isLibraryException() {
        LibraryException.BusinessRuleException ex = new LibraryException.BusinessRuleException("rule violated");
        assertThat(ex).isInstanceOf(LibraryException.class);
        assertThat(ex.getMessage()).isEqualTo("rule violated");
    }

    @Test
    @DisplayName("DuplicateException is a LibraryException")
    void duplicateException_isLibraryException() {
        LibraryException.DuplicateException ex = new LibraryException.DuplicateException("duplicate");
        assertThat(ex).isInstanceOf(LibraryException.class);
        assertThat(ex.getMessage()).isEqualTo("duplicate");
    }

    @Test
    @DisplayName("InvalidIdentifierException is a LibraryException")
    void invalidIdentifierException_isLibraryException() {
        LibraryException.InvalidIdentifierException ex =
                new LibraryException.InvalidIdentifierException("bad identifier");
        assertThat(ex).isInstanceOf(LibraryException.class);
        assertThat(ex.getMessage()).isEqualTo("bad identifier");
    }

    @Test
    @DisplayName("PersistenceException wraps cause correctly")
    void persistenceException_wrapsCause() {
        Throwable cause = new RuntimeException("sql error");
        LibraryException.PersistenceException ex =
                new LibraryException.PersistenceException("db failed", cause);
        assertThat(ex).isInstanceOf(LibraryException.class);
        assertThat(ex.getMessage()).isEqualTo("db failed");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("PersistenceException accepts null cause")
    void persistenceException_nullCause() {
        LibraryException.PersistenceException ex =
                new LibraryException.PersistenceException("db failed", null);
        assertThat(ex.getCause()).isNull();
    }

    @Test
    @DisplayName("LibraryException base constructor with message and cause")
    void libraryException_messageAndCause() {
        Throwable cause = new RuntimeException("root");
        LibraryException ex = new LibraryException("wrapped", cause);
        assertThat(ex.getMessage()).isEqualTo("wrapped");
        assertThat(ex.getCause()).isSameAs(cause);
    }

    @Test
    @DisplayName("Each subtype is distinct (not interchangeable)")
    void subtypes_areDistinct() {
        assertThat(new LibraryException.NotFoundException("x"))
                .isNotInstanceOf(LibraryException.DuplicateException.class);
        assertThat(new LibraryException.BusinessRuleException("x"))
                .isNotInstanceOf(LibraryException.PersistenceException.class);
    }

    // =========================================================================
    // AnswerWithSources record
    // =========================================================================

    @Test
    @DisplayName("AnswerWithSources preserves answer and sources")
    void answerWithSources_normal() {
        var aws = new AnswerWithSources("hello", java.util.List.of("s1", "s2"));
        assertThat(aws.answer()).isEqualTo("hello");
        assertThat(aws.sources()).containsExactly("s1", "s2");
    }

    @Test
    @DisplayName("AnswerWithSources null answer defaults to empty string")
    void answerWithSources_nullAnswer() {
        var aws = new AnswerWithSources(null, java.util.List.of());
        assertThat(aws.answer()).isEmpty();
    }

    @Test
    @DisplayName("AnswerWithSources null sources defaults to empty list")
    void answerWithSources_nullSources() {
        var aws = new AnswerWithSources("answer", null);
        assertThat(aws.sources()).isEmpty();
    }

    @Test
    @DisplayName("AnswerWithSources sources list is immutable")
    void answerWithSources_sourcesImmutable() {
        var sources = new java.util.ArrayList<>(java.util.List.of("s1"));
        var aws = new AnswerWithSources("answer", sources);
        // modifying the original list should not affect the record
        sources.add("s2");
        assertThat(aws.sources()).hasSize(1);
        // the record's list itself should not be modifiable
        assertThatThrownBy(() -> aws.sources().add("s3"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("AnswerWithSources with empty answer and empty sources")
    void answerWithSources_empty() {
        var aws = new AnswerWithSources("", java.util.List.of());
        assertThat(aws.answer()).isEmpty();
        assertThat(aws.sources()).isEmpty();
    }
}
