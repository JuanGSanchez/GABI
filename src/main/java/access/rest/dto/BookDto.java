package access.rest.dto;

import tables.Book;

/**
 * Data-transfer object for {@link Book} — exposes catalogue data over the REST and MCP
 * surfaces without leaking the domain object's internal structure.
 *
 * <p>Produced by {@link access.rest.LibraryRestController} and
 * {@link access.mcp.LibraryMcpTools}; both surfaces use the same DTO so the shape is
 * consistent (F-single-core).
 *
 * @param id     unique book identifier
 * @param title  book title
 * @param author primary author
 * @param lent   {@code true} if the book is currently on loan
 *
 * @author JuanGS (workstream c — access layer)
 */
public record BookDto(int id, String title, String author, boolean lent) {

    /** Maps a domain {@link Book} to its DTO representation. */
    public static BookDto from(Book b) {
        return new BookDto(b.getID(), b.getTitle(), b.getAuthor(), b.isLent());
    }
}
