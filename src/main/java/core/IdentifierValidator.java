package core;

import java.util.regex.Pattern;

/**
 * Strict whitelist validator for SQL identifiers (table names, column names,
 * Derby user names) that cannot be bound as PreparedStatement parameters.
 *
 * <p>Defect D-4 fix: user-supplied names flowed unescaped into:
 * <ul>
 *   <li>Derby SET PROPERTY {@code derby.user.<name>}</li>
 *   <li>GRANT / REVOKE DDL statements in {@code UserDerby}</li>
 *   <li>{@code DROP/CREATE TABLE} in {@code DatabaseBuilder}</li>
 * </ul>
 *
 * <p>This validator must be called on every user-supplied identifier BEFORE
 * it is interpolated into a SQL string. Values that are DATA (not identifiers)
 * must still use PreparedStatement / parameter binding — this class is NOT a
 * substitute for parameterised queries on data values.
 *
 * @author JuanGS (workstream b — D-4 fix)
 */
public final class IdentifierValidator {

    /**
     * Allowed pattern: letters, digits, underscore only.
     * Matches SQL identifiers safe for Derby DDL without quoting.
     */
    private static final Pattern SAFE_IDENTIFIER = Pattern.compile("^[A-Za-z0-9_]+$");

    /** Maximum reasonable identifier length (Derby identifier limit is 128 chars). */
    private static final int MAX_LENGTH = 128;

    private IdentifierValidator() {}

    /**
     * Validates that {@code identifier} is a safe SQL identifier.
     *
     * @param identifier the name to validate
     * @param context    a short description used in the exception message
     *                   (e.g. "username", "table name")
     * @return the trimmed identifier if valid
     * @throws LibraryException.InvalidIdentifierException if the identifier
     *         is null, blank, too long, or contains characters outside
     *         {@code [A-Za-z0-9_]}
     */
    public static String validate(String identifier, String context) {
        if (identifier == null || identifier.isBlank()) {
            throw new LibraryException.InvalidIdentifierException(
                    "Identifier (" + context + ") must not be null or blank");
        }
        String trimmed = identifier.trim();
        if (trimmed.length() > MAX_LENGTH) {
            throw new LibraryException.InvalidIdentifierException(
                    "Identifier (" + context + ") exceeds maximum length " + MAX_LENGTH);
        }
        if (!SAFE_IDENTIFIER.matcher(trimmed).matches()) {
            throw new LibraryException.InvalidIdentifierException(
                    "Identifier (" + context + ") contains disallowed characters. " +
                    "Only [A-Za-z0-9_] are permitted.");
        }
        return trimmed;
    }
}
