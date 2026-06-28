package ui.assistant;

import core.LibraryService;

/**
 * Default {@link AppStateProvider} that summarizes the live catalogue (SPEC-02).
 *
 * <p>Builds a short, secret-free snapshot from {@code core.LibraryService} counts:
 * the number of books, members and loans currently registered. It reads only aggregate
 * catalogue figures — never DB credentials, API keys, or user passwords — so the snapshot
 * is safe to append to an outbound prompt.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public class CatalogueContextProvider implements AppStateProvider {

    private final transient LibraryService service;

    public CatalogueContextProvider(LibraryService service) {
        this.service = service;
    }

    @Override
    public String snapshot() {
        try {
            int books = service.countBooks()[0];
            int members = service.countMembers()[0];
            int loans = service.countLoans()[0];
            return String.format(
                    "GABI library state: %d books, %d members, %d active loans currently registered.",
                    books, members, loans);
        } catch (RuntimeException e) {
            // Context is best-effort; never block a chat on a snapshot failure.
            return "";
        }
    }
}
