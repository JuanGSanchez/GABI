package access.mcp;

import access.rest.dto.BookDto;
import access.rest.dto.CountResult;
import access.rest.dto.LoanDto;
import access.rest.dto.MemberDto;
import core.AnswerWithSources;
import core.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * MCP tool facade for the GABI library catalogue and RAG service.
 *
 * <p>All tool methods delegate directly to {@link LibraryService} — there is no
 * business logic here (F-single-core). The same {@link LibraryService} bean also backs
 * the REST endpoints in {@link access.rest.LibraryRestController}.
 *
 * <h2>Exposed MCP tools</h2>
 * <ul>
 *   <li>RAG: {@code ask}, {@code reindex}</li>
 *   <li>Books: {@code list_books}, {@code search_books}, {@code get_book},
 *       {@code count_books}</li>
 *   <li>Members: {@code list_members}, {@code search_members}, {@code get_member},
 *       {@code count_members}</li>
 *   <li>Loans: {@code list_loans}, {@code list_loans_by_member}, {@code count_loans}</li>
 *   <li>Health: {@code health}</li>
 * </ul>
 *
 * <h2>Security</h2>
 * User-admin operations ({@code addUser}, {@code deleteUser}, Derby
 * {@code derby.user.*} / GRANT / REVOKE) are intentionally absent from this tool set
 * per strategy D4. DB credentials are never returned in tool results.
 *
 * <h2>Transport</h2>
 * Registered with the Spring AI MCP server starter via
 * {@link access.mcp.McpToolsConfig}. Active on the {@code server} Spring profile.
 * Transport: Streamable-HTTP ({@code /mcp}) for remote clients; STDIO for CLI/agent
 * launch (configured in {@code application.yml}).
 *
 * @author JuanGS (workstream c — access layer)
 */
@Component
public class LibraryMcpTools {

    private static final Logger log = LoggerFactory.getLogger(LibraryMcpTools.class);

    private final LibraryService library;

    public LibraryMcpTools(LibraryService library) {
        this.library = library;
    }

    // =========================================================================
    // RAG
    // =========================================================================

    /**
     * Answers a natural-language question over the indexed library catalogue using
     * retrieval-augmented generation. Returns the grounded answer and the source
     * catalogue chunks used as context.
     */
    @Tool(name = "ask",
          description = "Answer a natural-language question about the library catalogue using RAG. " +
                        "Returns the grounded answer and the source catalogue chunks used.")
    public AnswerWithSources ask(
            @ToolParam(description = "Natural-language question about the library catalogue, " +
                                     "e.g. 'Which sci-fi books are available?'", required = true)
            String question) {
        log.debug("MCP tool ask: question={}", question);
        return library.ask(question);
    }

    /**
     * Rebuilds the RAG vector index from the current live catalogue state. Call this
     * after adding or removing books, or at startup to warm the vector store.
     */
    @Tool(name = "reindex",
          description = "Rebuild the RAG vector index from the current library catalogue. " +
                        "Call after adding/removing books to keep RAG answers current.")
    public String reindex() {
        log.info("MCP tool reindex: rebuilding vector index");
        library.ingest();
        return "Vector index rebuilt successfully.";
    }

    // =========================================================================
    // BOOKS
    // =========================================================================

    /** Lists all books in the catalogue. */
    @Tool(name = "list_books",
          description = "List all books in the library catalogue.")
    public List<BookDto> listBooks() {
        return library.listBooks().stream().map(BookDto::from).toList();
    }

    /**
     * Searches books by a field (title or author) and text fragment.
     */
    @Tool(name = "search_books",
          description = "Search books by field and text fragment. " +
                        "field must be 'title' or 'author'.")
    public List<BookDto> searchBooks(
            @ToolParam(description = "Search field: 'title' or 'author'", required = true)
            String field,
            @ToolParam(description = "Text fragment to search for (case-insensitive)", required = true)
            String text) {
        return library.searchBooks(field, text).stream().map(BookDto::from).toList();
    }

    /** Paginated, multi-field, case-insensitive book search (read-only, SPEC-20). */
    @Tool(name = "search_books_paged",
          description = "Search books across title and author (case-insensitive), paginated. "
                      + "Returns a page of books with pagination metadata.")
    public core.search.Page<BookDto> searchBooksPaged(
            @ToolParam(description = "Case-insensitive substring; blank matches all", required = false)
            String query,
            @ToolParam(description = "Zero-based page index", required = false)
            Integer page,
            @ToolParam(description = "Page size (default 20)", required = false)
            Integer size,
            @ToolParam(description = "Sort field: 'id', 'title' or 'author'", required = false)
            String sort,
            @ToolParam(description = "Sort direction: 'asc' or 'desc'", required = false)
            String dir) {
        return library.searchBooksPaged(
                query == null ? "" : query,
                page == null ? 0 : page,
                size == null ? 20 : size,
                sort == null ? "id" : sort,
                !"desc".equalsIgnoreCase(dir)).map(BookDto::from);
    }

    /**
     * Returns the book with the given ID, or throws if not found.
     */
    @Tool(name = "get_book",
          description = "Get a single book by its numeric ID.")
    public BookDto getBook(
            @ToolParam(description = "The book's unique integer ID", required = true)
            int id) {
        return library.getBook(id)
                .map(BookDto::from)
                .orElseThrow(() -> new core.LibraryException.NotFoundException("Book not found: " + id));
    }

    /**
     * Returns the total count and current maximum ID for the books table.
     */
    @Tool(name = "count_books",
          description = "Return the total number of books and the current maximum book ID.")
    public CountResult countBooks() {
        return CountResult.from(library.countBooks());
    }

    // =========================================================================
    // MEMBERS
    // =========================================================================

    /** Lists all registered library members. */
    @Tool(name = "list_members",
          description = "List all registered library members.")
    public List<MemberDto> listMembers() {
        return library.listMembers().stream().map(MemberDto::from).toList();
    }

    /**
     * Searches members by a field (name or surname) and text fragment.
     */
    @Tool(name = "search_members",
          description = "Search members by field and text fragment. " +
                        "field must be 'name' or 'surname'.")
    public List<MemberDto> searchMembers(
            @ToolParam(description = "Search field: 'name' or 'surname'", required = true)
            String field,
            @ToolParam(description = "Text fragment to search for (case-insensitive)", required = true)
            String text) {
        return library.searchMembers(field, text).stream().map(MemberDto::from).toList();
    }

    /**
     * Paginated, multi-field, case-insensitive member search (SPEC-R06). Read-only.
     */
    @Tool(name = "search_members_paged",
          description = "Search members across name and surname (case-insensitive), paginated. "
                      + "Returns a page of members with pagination metadata.")
    public core.search.Page<MemberDto> searchMembersPaged(
            @ToolParam(description = "Case-insensitive substring; blank matches all", required = false)
            String query,
            @ToolParam(description = "Zero-based page index", required = false)
            Integer page,
            @ToolParam(description = "Page size (default 20)", required = false)
            Integer size,
            @ToolParam(description = "Sort field: 'id', 'name' or 'surname'", required = false)
            String sort,
            @ToolParam(description = "Sort direction: 'asc' or 'desc'", required = false)
            String dir) {
        return library.searchMembersPaged(
                query == null ? "" : query,
                page == null ? 0 : page,
                size == null ? 20 : size,
                sort == null ? "id" : sort,
                !"desc".equalsIgnoreCase(dir)).map(MemberDto::from);
    }

    /**
     * Returns the member with the given ID, or throws if not found.
     */
    @Tool(name = "get_member",
          description = "Get a single member by their numeric ID.")
    public MemberDto getMember(
            @ToolParam(description = "The member's unique integer ID", required = true)
            int id) {
        return library.getMember(id)
                .map(MemberDto::from)
                .orElseThrow(() -> new core.LibraryException.NotFoundException("Member not found: " + id));
    }

    /**
     * Returns the total count and current maximum ID for the members table.
     */
    @Tool(name = "count_members",
          description = "Return the total number of members and the current maximum member ID.")
    public CountResult countMembers() {
        return CountResult.from(library.countMembers());
    }

    // =========================================================================
    // LOANS
    // =========================================================================

    /** Lists all active loans. */
    @Tool(name = "list_loans",
          description = "List all active book loans.")
    public List<LoanDto> listLoans() {
        return library.listLoans().stream().map(LoanDto::from).toList();
    }

    /**
     * Returns all loans for the specified member.
     */
    @Tool(name = "list_loans_by_member",
          description = "List all loans for a specific member by their ID.")
    public List<LoanDto> listLoansByMember(
            @ToolParam(description = "The member's unique integer ID", required = true)
            int memberId) {
        return library.listLoansByMember(memberId).stream().map(LoanDto::from).toList();
    }

    /**
     * Returns the total count and current maximum ID for the loans table.
     */
    @Tool(name = "count_loans",
          description = "Return the total number of active loans and the current maximum loan ID.")
    public CountResult countLoans() {
        return CountResult.from(library.countLoans());
    }

    /** Lists loans past their due date (read-only, SPEC-19). */
    @Tool(name = "list_overdue_loans",
          description = "List all loans that are past their due date, with each loan's due date.")
    public List<access.rest.dto.OverdueLoanDto> listOverdueLoans() {
        return library.listOverdueLoans().stream()
                .map(l -> access.rest.dto.OverdueLoanDto.from(l, library.dueDate(l)))
                .toList();
    }

    // =========================================================================
    // HEALTH
    // =========================================================================

    /**
     * Returns a simple health check confirming the GABI service is reachable.
     */
    @Tool(name = "health",
          description = "Health check: returns status UP and a server timestamp.")
    public Map<String, String> health() {
        return Map.of(
                "status", "UP",
                "timestamp", Instant.now().toString()
        );
    }
}
