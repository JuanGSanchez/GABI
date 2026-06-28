package core;

/**
 * Base typed exception for the GABI headless library core.
 * Replaces the locale-bearing RuntimeException(localizedMessage) pattern
 * used in the legacy DAOs (D-5 fix). The message is always an English
 * machine-readable key — localisation stays at the UI edge.
 *
 * @author JuanGS (enhanced — workstream b)
 */
public class LibraryException extends RuntimeException {

    public LibraryException(String message) {
        super(message);
    }

    public LibraryException(String message, Throwable cause) {
        super(message, cause);
    }

    // ---- subtypes -------------------------------------------------------

    /** Entity not found in the catalogue. */
    public static class NotFoundException extends LibraryException {
        public NotFoundException(String message) { super(message); }
    }

    /** A business rule was violated (e.g. "cannot delete a lent book"). */
    public static class BusinessRuleException extends LibraryException {
        public BusinessRuleException(String message) { super(message); }
    }

    /** A duplicate entry was detected (unique-constraint level). */
    public static class DuplicateException extends LibraryException {
        public DuplicateException(String message) { super(message); }
    }

    /** The supplied identifier failed the whitelist validation (D-4 fix). */
    public static class InvalidIdentifierException extends LibraryException {
        public InvalidIdentifierException(String message) { super(message); }
    }

    /**
     * Wraps an SQL / persistence error that the caller should treat as a
     * transient infrastructure failure (not a business-rule violation).
     */
    public static class PersistenceException extends LibraryException {
        public PersistenceException(String message, Throwable cause) { super(message, cause); }
    }
}
