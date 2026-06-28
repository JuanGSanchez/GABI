package core.search;

import java.util.List;

/**
 * A single page of search results with pagination metadata (SPEC-20).
 *
 * @param content       the rows on this page
 * @param page          the zero-based page index
 * @param size          the requested page size
 * @param totalElements the total number of matching rows across all pages
 * @param <T>           the row type
 * @author GABI SDD pipeline (SPEC-20 catalogue search and pagination)
 */
public record Page<T>(List<T> content, int page, int size, long totalElements) {

    public Page {
        content = List.copyOf(content);
    }

    /** The total number of pages for the current page size (at least 1, or 0 when empty). */
    public int totalPages() {
        if (size <= 0) {
            return totalElements == 0 ? 0 : 1;
        }
        return (int) ((totalElements + size - 1) / size);
    }

    /** Whether a further page exists after this one. */
    public boolean hasNext() {
        return (long) (page + 1) * size < totalElements;
    }

    /** Maps the page content to another type, preserving the metadata. */
    public <R> Page<R> map(java.util.function.Function<T, R> mapper) {
        return new Page<>(content.stream().map(mapper).toList(), page, size, totalElements);
    }
}
