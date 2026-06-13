package access;

import access.rest.GlobalExceptionHandler;
import core.LibraryException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for {@link GlobalExceptionHandler} — verifies that each
 * {@link LibraryException} subtype maps to the correct HTTP status.
 *
 * <p>No Spring context required; the handler is tested as a plain object.
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("NotFoundException maps to 404 Not Found")
    void notFound_maps_404() {
        ProblemDetail pd = handler.handleNotFound(
                new LibraryException.NotFoundException("Book not found: 42"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(pd.getDetail()).isEqualTo("Book not found: 42");
    }

    @Test
    @DisplayName("InvalidIdentifierException maps to 400 Bad Request")
    void invalidIdentifier_maps_400() {
        ProblemDetail pd = handler.handleInvalidIdentifier(
                new LibraryException.InvalidIdentifierException("bad identifier"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(pd.getDetail()).isEqualTo("bad identifier");
    }

    @Test
    @DisplayName("BusinessRuleException maps to 422 Unprocessable Entity")
    void businessRule_maps_422() {
        ProblemDetail pd = handler.handleBusinessRule(
                new LibraryException.BusinessRuleException("book is lent out"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY.value());
        assertThat(pd.getDetail()).isEqualTo("book is lent out");
    }

    @Test
    @DisplayName("DuplicateException maps to 409 Conflict")
    void duplicate_maps_409() {
        ProblemDetail pd = handler.handleDuplicate(
                new LibraryException.DuplicateException("already exists"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(pd.getDetail()).isEqualTo("already exists");
    }

    @Test
    @DisplayName("PersistenceException maps to 500 Internal Server Error (detail masked)")
    void persistence_maps_500() {
        ProblemDetail pd = handler.handlePersistence(
                new LibraryException.PersistenceException("DB error details", null));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        // detail must NOT contain the raw DB error — it should be a generic message
        assertThat(pd.getDetail()).doesNotContain("DB error details");
        assertThat(pd.getDetail()).isNotBlank();
    }

    @Test
    @DisplayName("Base LibraryException maps to 500 Internal Server Error")
    void baseLibraryException_maps_500() {
        ProblemDetail pd = handler.handleLibraryException(
                new LibraryException("unexpected"));
        assertThat(pd.getStatus()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(pd.getDetail()).isNotBlank();
    }
}
