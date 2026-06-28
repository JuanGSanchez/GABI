package core;

import core.search.Page;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Book;

import javax.sql.DataSource;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-20 acceptance over in-memory Derby: paginated, multi-field, case-insensitive,
 * sorted book search using parameterized queries (the sort identifier is whitelisted and
 * validated — no interpolation of user input).
 */
class LibraryServiceImplPagedSearchTest {

    private LibraryServiceImpl service;

    @BeforeEach
    void setUp() throws SQLException {
        DataSource ds = new InMemoryDerbyConfig().testDataSource();
        TestSchemaHelper.createSchema(ds);
        service = new LibraryServiceImpl(ds, new StubRagService());
        service.addBook(1, "Dune", "Herbert");
        service.addBook(2, "Dune Messiah", "Herbert");
        service.addBook(3, "It", "King");
        service.addBook(4, "The Stand", "King");
    }

    @Test
    @DisplayName("matches across both title and author, case-insensitively")
    void multiFieldCaseInsensitive() {
        // 'herbert' matches author of two books
        Page<Book> byAuthor = service.searchBooksPaged("herbert", 0, 10, "id", true);
        assertThat(byAuthor.totalElements()).isEqualTo(2);
        // 'dune' matches title of two books
        Page<Book> byTitle = service.searchBooksPaged("DUNE", 0, 10, "id", true);
        assertThat(byTitle.totalElements()).isEqualTo(2);
    }

    @Test
    @DisplayName("blank query returns all books with pagination metadata")
    void blankReturnsAll() {
        Page<Book> all = service.searchBooksPaged("", 0, 3, "id", true);
        assertThat(all.totalElements()).isEqualTo(4);
        assertThat(all.content()).hasSize(3);
        assertThat(all.totalPages()).isEqualTo(2);
        assertThat(all.hasNext()).isTrue();
    }

    @Test
    @DisplayName("pages slice the result set")
    void pages() {
        Page<Book> page0 = service.searchBooksPaged("", 0, 2, "id", true);
        Page<Book> page1 = service.searchBooksPaged("", 1, 2, "id", true);
        assertThat(page0.content()).extracting(Book::getID).containsExactly(1, 2);
        assertThat(page1.content()).extracting(Book::getID).containsExactly(3, 4);
    }

    @Test
    @DisplayName("sorting by title respects direction")
    void sorting() {
        Page<Book> asc = service.searchBooksPaged("", 0, 10, "title", true);
        Page<Book> desc = service.searchBooksPaged("", 0, 10, "title", false);
        assertThat(asc.content().get(0).getTitle()).isEqualTo("Dune");
        assertThat(desc.content().get(0).getTitle()).isEqualTo("The Stand");
    }

    @Test
    @DisplayName("an unknown sort field falls back to a safe default (no injection)")
    void unknownSortDefaults() {
        // a crafted sort field must not break the query — it falls back to the id column
        Page<Book> page = service.searchBooksPaged("", 0, 10, "title; DROP TABLE admin.books", true);
        assertThat(page.totalElements()).isEqualTo(4);
    }
}
