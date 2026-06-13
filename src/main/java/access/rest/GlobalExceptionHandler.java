package access.rest;

import core.LibraryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Translates {@link LibraryException} subtypes into RFC-9457 {@link ProblemDetail}
 * responses for every {@code @RestController} in the access layer.
 *
 * <p>Mapping table:
 * <ul>
 *   <li>{@link LibraryException.NotFoundException}          → 404 Not Found</li>
 *   <li>{@link LibraryException.InvalidIdentifierException} → 400 Bad Request</li>
 *   <li>{@link LibraryException.BusinessRuleException}      → 422 Unprocessable Entity</li>
 *   <li>{@link LibraryException.DuplicateException}         → 409 Conflict</li>
 *   <li>{@link LibraryException.PersistenceException}       → 500 Internal Server Error</li>
 *   <li>{@link LibraryException} (base)                     → 500 Internal Server Error</li>
 * </ul>
 *
 * <p>Stack traces and DB credentials are NEVER included in the response body
 * (strategy D-3 / security note). Only the exception message (locale-free, machine-readable)
 * is forwarded.
 *
 * @author JuanGS (workstream c — access layer)
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(LibraryException.NotFoundException.class)
    public ProblemDetail handleNotFound(LibraryException.NotFoundException ex) {
        log.debug("NotFoundException: {}", ex.getMessage());
        return problem(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    @ExceptionHandler(LibraryException.InvalidIdentifierException.class)
    public ProblemDetail handleInvalidIdentifier(LibraryException.InvalidIdentifierException ex) {
        log.debug("InvalidIdentifierException: {}", ex.getMessage());
        return problem(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(LibraryException.BusinessRuleException.class)
    public ProblemDetail handleBusinessRule(LibraryException.BusinessRuleException ex) {
        log.debug("BusinessRuleException: {}", ex.getMessage());
        return problem(HttpStatus.UNPROCESSABLE_ENTITY, ex.getMessage());
    }

    @ExceptionHandler(LibraryException.DuplicateException.class)
    public ProblemDetail handleDuplicate(LibraryException.DuplicateException ex) {
        log.debug("DuplicateException: {}", ex.getMessage());
        return problem(HttpStatus.CONFLICT, ex.getMessage());
    }

    @ExceptionHandler(LibraryException.PersistenceException.class)
    public ProblemDetail handlePersistence(LibraryException.PersistenceException ex) {
        // Log at WARN — infrastructure failure but not a code bug.
        // Deliberately do NOT include the cause in the response to avoid leaking DB details.
        log.warn("PersistenceException: {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "A database error occurred.");
    }

    @ExceptionHandler(LibraryException.class)
    public ProblemDetail handleLibraryException(LibraryException ex) {
        log.warn("LibraryException (unclassified): {}", ex.getMessage());
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected library error occurred.");
    }

    // -------------------------------------------------------------------------

    private static ProblemDetail problem(HttpStatus status, String detail) {
        ProblemDetail pd = ProblemDetail.forStatus(status);
        pd.setDetail(detail);
        return pd;
    }
}
