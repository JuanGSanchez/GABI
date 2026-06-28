package access.rest;

import access.rest.dto.AskRequest;
import access.rest.dto.BookDto;
import access.rest.dto.CountResult;
import access.rest.dto.LoanDto;
import access.rest.dto.MemberDto;
import core.AnswerWithSources;
import core.LibraryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * REST controller exposing the GABI library catalogue and RAG capabilities over HTTP.
 *
 * <p>All endpoints delegate directly to {@link LibraryService} — there is NO business
 * logic here (F-single-core).  The same core bean also backs the MCP tools in
 * {@link access.mcp.LibraryMcpTools}.
 *
 * <h2>Exposed surface</h2>
 * <ul>
 *   <li>RAG: {@code POST /api/ask}, {@code POST /api/reindex}</li>
 *   <li>Books: {@code GET /api/books}, {@code GET /api/books/search},
 *       {@code GET /api/books/{id}}, {@code GET /api/books/count}</li>
 *   <li>Members: {@code GET /api/members}, {@code GET /api/members/search},
 *       {@code GET /api/members/{id}}, {@code GET /api/members/count}</li>
 *   <li>Loans: {@code GET /api/loans}, {@code GET /api/members/{memberId}/loans},
 *       {@code GET /api/loans/count}</li>
 * </ul>
 *
 * <h2>Security</h2>
 * <ul>
 *   <li>User-admin operations (addUser / deleteUser / derby.user.*) are NOT exposed
 *       here — kept out of the default surface per strategy D4.</li>
 *   <li>Exceptions are translated to safe HTTP responses by
 *       {@link GlobalExceptionHandler} — no stack traces or DB credentials on the wire.</li>
 * </ul>
 *
 * <p>Activated only when the {@code server} Spring profile is active
 * ({@code -Dspring.profiles.active=server}).
 *
 * @author JuanGS (workstream c — access layer)
 */
@RestController
@RequestMapping("/api")
public class LibraryRestController {

    private final LibraryService library;

    public LibraryRestController(LibraryService library) {
        this.library = library;
    }

    // =========================================================================
    // RAG
    // =========================================================================

    /**
     * Answers a natural-language question over the indexed catalogue using RAG.
     *
     * <pre>POST /api/ask
     * Content-Type: application/json
     * {"question": "Which sci-fi books are available?"}</pre>
     */
    @PostMapping("/ask")
    public AnswerWithSources ask(@RequestBody AskRequest req) {
        return library.ask(req.question());
    }

    /**
     * Rebuilds the RAG vector index from the current live catalogue state.
     *
     * <pre>POST /api/reindex</pre>
     */
    @PostMapping("/reindex")
    public ResponseEntity<Void> reindex() {
        library.ingest();
        return ResponseEntity.noContent().build();
    }

    // =========================================================================
    // BOOKS
    // =========================================================================

    /** {@code GET /api/books} — list all books. */
    @GetMapping("/books")
    public List<BookDto> listBooks() {
        return library.listBooks().stream().map(BookDto::from).toList();
    }

    /**
     * {@code GET /api/books/search?field=title&text=Dune} — search by field + text.
     * {@code field} must be {@code title} or {@code author}.
     */
    @GetMapping("/books/search")
    public List<BookDto> searchBooks(
            @RequestParam String field,
            @RequestParam String text) {
        return library.searchBooks(field, text).stream().map(BookDto::from).toList();
    }

    /**
     * {@code GET /api/books/search/paged} — paginated, multi-field, case-insensitive book
     * search (SPEC-20). Query params: {@code q}, {@code page}, {@code size}, {@code sort}
     * ("id"/"title"/"author"), {@code dir} ("asc"/"desc"). Returns a page + metadata.
     */
    @GetMapping("/books/search/paged")
    public core.search.Page<BookDto> searchBooksPaged(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "id") String sort,
            @RequestParam(defaultValue = "asc") String dir) {
        return library.searchBooksPaged(q, page, size, sort, !"desc".equalsIgnoreCase(dir))
                .map(BookDto::from);
    }

    /** {@code GET /api/books/{id}} — get a single book by ID. */
    @GetMapping("/books/{id}")
    public ResponseEntity<BookDto> getBook(@PathVariable int id) {
        return library.getBook(id)
                .map(BookDto::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new core.LibraryException.NotFoundException("Book not found: " + id));
    }

    /** {@code GET /api/books/count} — total count and current max ID. */
    @GetMapping("/books/count")
    public CountResult countBooks() {
        return CountResult.from(library.countBooks());
    }

    // =========================================================================
    // MEMBERS
    // =========================================================================

    /** {@code GET /api/members} — list all members. */
    @GetMapping("/members")
    public List<MemberDto> listMembers() {
        return library.listMembers().stream().map(MemberDto::from).toList();
    }

    /**
     * {@code GET /api/members/search?field=name&text=Ana} — search by field + text.
     * {@code field} must be {@code name} or {@code surname}.
     */
    @GetMapping("/members/search")
    public List<MemberDto> searchMembers(
            @RequestParam String field,
            @RequestParam String text) {
        return library.searchMembers(field, text).stream().map(MemberDto::from).toList();
    }

    /** {@code GET /api/members/{id}} — get a single member by ID. */
    @GetMapping("/members/{id}")
    public ResponseEntity<MemberDto> getMember(@PathVariable int id) {
        return library.getMember(id)
                .map(MemberDto::from)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new core.LibraryException.NotFoundException("Member not found: " + id));
    }

    /** {@code GET /api/members/count} — total count and current max ID. */
    @GetMapping("/members/count")
    public CountResult countMembers() {
        return CountResult.from(library.countMembers());
    }

    // =========================================================================
    // LOANS
    // =========================================================================

    /** {@code GET /api/loans} — list all loans. */
    @GetMapping("/loans")
    public List<LoanDto> listLoans() {
        return library.listLoans().stream().map(LoanDto::from).toList();
    }

    /** {@code GET /api/members/{memberId}/loans} — list loans for a specific member. */
    @GetMapping("/members/{memberId}/loans")
    public List<LoanDto> listLoansByMember(@PathVariable int memberId) {
        return library.listLoansByMember(memberId).stream().map(LoanDto::from).toList();
    }

    /** {@code GET /api/loans/count} — total count and current max ID. */
    @GetMapping("/loans/count")
    public CountResult countLoans() {
        return CountResult.from(library.countLoans());
    }

    /** {@code GET /api/loans/overdue} — read-only list of loans past their due date (SPEC-19). */
    @GetMapping("/loans/overdue")
    public List<access.rest.dto.OverdueLoanDto> listOverdueLoans() {
        return library.listOverdueLoans().stream()
                .map(l -> access.rest.dto.OverdueLoanDto.from(l, library.dueDate(l)))
                .toList();
    }
}
