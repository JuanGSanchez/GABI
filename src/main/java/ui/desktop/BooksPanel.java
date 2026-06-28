package ui.desktop;

import core.LibraryService;
import tables.Book;
import ui.UiText;

import java.util.List;

/**
 * Desktop adapter for the Books catalogue. Mirrors the console {@code BookMenu}
 * (add / list / search / delete / total) over the shared {@code core.LibraryService}.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class BooksPanel extends EntityPanel<Book> {

    public BooksPanel(LibraryService service, UiText text) {
        super(service, text);
        refresh();
    }

    @Override
    protected List<EntityTableModel.Column<Book>> columns() {
        UiText t = textOrDefault();
        return List.of(
                new EntityTableModel.Column<>(t.getOr("program-book-properties-1", "ID"), Book::getID),
                new EntityTableModel.Column<>(t.getOr("program-book-properties-2", "title"), Book::getTitle),
                new EntityTableModel.Column<>(t.getOr("program-book-properties-3", "author"), Book::getAuthor),
                new EntityTableModel.Column<>(
                        t.getOr("dao-book-lent-total", "status"),
                        b -> b.isLent()
                                ? t.getOr("dao-book-lent-true", "lent")
                                : t.getOr("dao-book-lent-false", "available")));
    }

    @Override
    protected List<Book> loadRows() {
        return service.listBooks();
    }

    @Override
    protected String menuPrefix() {
        return "program-book-menu-";
    }

    @Override
    protected String entityWord() {
        return text.getOr("program-properties-field-1-singular", "Book");
    }

    @Override
    protected String entityWordPlural() {
        return text.getOr("program-properties-field-1-plural", "Books");
    }

    @Override
    protected void onAdd() {
        String title = prompt(text.getOr("program-book-properties-2", "title"));
        if (title == null) {
            return;
        }
        String author = prompt(text.getOr("program-book-properties-3", "author"));
        if (author == null) {
            return;
        }
        int nextId = service.countBooks()[1] + 1;
        service.addBook(nextId, title, author); // all rules enforced in the core
    }

    @Override
    protected void onRemove() {
        Book selected = selectedRow();
        if (selected == null) {
            return;
        }
        service.deleteBook(selected.getID());
    }

    @Override
    protected List<Book> searchRows() {
        String field = prompt(text.getOr("program-general-criteria", "field (title/author)"));
        if (field == null) {
            return null;
        }
        String fragment = prompt(text.getOr("program-general-search", "search"));
        if (fragment == null) {
            return null;
        }
        return service.searchBooks(field, fragment);
    }

    // textOrDefault keeps columns() resilient even before the super-constructor field is visible
    private UiText textOrDefault() {
        return this.text != null ? this.text : UiText.english();
    }
}
