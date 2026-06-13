package access.rest.dto;

/**
 * Wraps the {@code int[2]} returned by the core's {@code count*()} methods.
 *
 * <p>{@link core.LibraryService} returns {@code int[] {count, maxId}} from all count
 * operations. This record gives the REST/MCP surface a named, serialisable shape.
 *
 * @param count the number of rows in the table
 * @param maxId the current maximum primary-key value (0 when the table is empty)
 *
 * @author JuanGS (workstream c — access layer)
 */
public record CountResult(int count, int maxId) {

    /** Converts the core's {@code int[]} pair to this record. */
    public static CountResult from(int[] pair) {
        return new CountResult(pair[0], pair[1]);
    }
}
