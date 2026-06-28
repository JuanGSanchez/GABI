package core;

import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Headless library-core service interface.
 *
 * <p>This is the single contract that ALL front-ends call:
 * <ul>
 *   <li>The CLI (manager package) calls it as a thin adapter.</li>
 *   <li>The MCP/REST access layer (workstream c) exposes it as tools/endpoints.</li>
 *   <li>Tests (workstream e) mock or exercise it against in-memory Derby.</li>
 * </ul>
 *
 * <p>Design invariants:
 * <ul>
 *   <li>All methods are <em>locale-free</em> — no ResourceBundle, no localized strings.</li>
 *   <li>Credentials are injected via a Spring-managed {@code DataSource} — never per-call.</li>
 *   <li>Business rules (duplicate checks, loan limits, lent-book guard) are enforced here,
 *       not in the UI layer.</li>
 *   <li>All exception types are subtypes of {@link LibraryException} — callers never receive
 *       a raw {@code RuntimeException(localizedMessage)}.</li>
 * </ul>
 *
 * @author JuanGS (workstream b — headless core extraction)
 */
public interface LibraryService {

    // =========================================================================
    // BOOKS
    // =========================================================================

    /** Returns all books in the catalogue. Never null; may be empty. */
    List<Book> listBooks();

    /**
     * Returns books matching {@code text} in the given {@code field}.
     *
     * @param field "title" or "author"
     * @param text  substring to search (case-insensitive LIKE match)
     * @return matching books; never null
     * @throws LibraryException.NotFoundException if no books match
     */
    List<Book> searchBooks(String field, String text);

    /**
     * Returns the book with the given ID.
     *
     * @throws LibraryException.NotFoundException if the ID does not exist
     */
    Optional<Book> getBook(int id);

    /**
     * Adds a new book to the catalogue.
     *
     * @param title  book title (non-blank)
     * @param author book author (non-blank)
     * @param id     the next sequential ID (managed by the caller / LibMenu counter)
     * @return the persisted Book
     * @throws LibraryException.DuplicateException if title+author already exists
     */
    Book addBook(int id, String title, String author);

    /**
     * Deletes the book with the given ID.
     *
     * @return the new max ID after deletion (0 if catalogue is now empty)
     * @throws LibraryException.NotFoundException     if the ID does not exist
     * @throws LibraryException.BusinessRuleException if the book is currently lent out
     */
    int deleteBook(int id);

    /**
     * Returns {@code [count, maxId]}: the total number of books and the current
     * maximum ID. Returns {@code [0, 0]} when the table is empty.
     */
    int[] countBooks();

    // =========================================================================
    // MEMBERS
    // =========================================================================

    /** Returns all members. Never null; may be empty. */
    List<Member> listMembers();

    /**
     * Returns members with details (list of currently borrowed books).
     * Never null; may be empty.
     */
    List<Member> listMembersWithLoans();

    /**
     * Searches members by name or surname fragment.
     *
     * @param field "name" or "surname"
     * @param text  substring (case-insensitive)
     * @return matching members; never null
     * @throws LibraryException.NotFoundException if no members match
     */
    List<Member> searchMembers(String field, String text);

    /**
     * Returns the member with the given ID.
     *
     * @throws LibraryException.NotFoundException if the ID does not exist
     */
    Optional<Member> getMember(int id);

    /**
     * Adds a new member.
     *
     * @param id      the next sequential ID
     * @param name    first name (non-blank)
     * @param surname surname (non-blank)
     * @return the persisted Member
     * @throws LibraryException.DuplicateException     if name+surname already registered
     * @throws LibraryException.BusinessRuleException  if the member limit is reached
     */
    Member addMember(int id, String name, String surname);

    /**
     * Deletes the member with the given ID.
     *
     * @return the new max ID after deletion (0 if table is now empty)
     * @throws LibraryException.NotFoundException     if the ID does not exist
     * @throws LibraryException.BusinessRuleException if the member still has active loans
     */
    int deleteMember(int id);

    /**
     * Returns {@code [count, maxId]} for the members table.
     */
    int[] countMembers();

    // =========================================================================
    // LOANS
    // =========================================================================

    /** Returns all loans. Never null; may be empty. */
    List<Loan> listLoans();

    /**
     * Returns all loans with resolved Member and Book details.
     * Never null; may be empty.
     */
    List<Loan> listLoansWithDetails();

    /**
     * Returns loans for the given member ID.
     *
     * @throws LibraryException.NotFoundException if no loans exist for that member
     */
    List<Loan> listLoansByMember(int memberId);

    /**
     * Creates a new loan linking a member to a book.
     *
     * @param loanId   the next sequential loan ID
     * @param memberId the borrowing member's ID
     * @param bookId   the book being borrowed
     * @return the persisted Loan
     * @throws LibraryException.BusinessRuleException if the member has reached the loan limit
     *         or the book is already lent out or does not exist
     */
    Loan createLoan(int loanId, int memberId, int bookId);

    /**
     * Returns a loan (sets the book back to available).
     *
     * @return the new max loan ID after the return (0 if no loans remain)
     * @throws LibraryException.NotFoundException if the loan ID does not exist
     */
    int returnLoan(int loanId);

    /**
     * Returns {@code [count, maxId]} for the loans table.
     */
    int[] countLoans();

    // ---- Due dates / overdue tracking (SPEC-19) -----------------------------

    /** The configured loan period in days (the single overdue knob). */
    int loanPeriodDays();

    /** The derived due date for a loan ({@code loanDate + loanPeriodDays}). */
    LocalDate dueDate(Loan loan);

    /**
     * Returns all loans that are past their due date as of today, with resolved Member and
     * Book details. Never null; may be empty.
     */
    List<Loan> listOverdueLoans();

    // =========================================================================
    // USERS (admin)
    // =========================================================================

    /** Returns all DB users. Never null; may be empty. */
    List<User> listUsers();

    /**
     * Provisions a new Derby database user.
     *
     * @param id       the next sequential user ID
     * @param username the Derby login name — validated by {@link IdentifierValidator}
     * @param password the user's password (char[] — never String, to allow zeroing)
     * @return the persisted User record (password field intentionally not populated)
     * @throws LibraryException.DuplicateException          if username already exists
     * @throws LibraryException.BusinessRuleException       if the user limit is reached
     * @throws LibraryException.InvalidIdentifierException  if username fails whitelist
     */
    User addUser(int id, String username, char[] password);

    /**
     * Removes a Derby database user and revokes its privileges.
     *
     * @return the new max user ID (0 if table is now empty)
     * @throws LibraryException.NotFoundException           if the user ID does not exist
     * @throws LibraryException.InvalidIdentifierException  if the stored username fails validation
     */
    int deleteUser(int id);

    /**
     * Returns {@code [count, maxId]} for the users table.
     */
    int[] countUsers();

    // =========================================================================
    // RAG (greenfield R2.a)
    // =========================================================================

    /**
     * Indexes the entire live book catalogue (and optionally additional documents)
     * into the RAG vector store.
     *
     * <p>Projects each book row into a {@code Document} with content
     * {@code "Title: <title>; Author: <author>; Lent: <true|false>"}, chunks
     * with {@code TokenTextSplitter}, embeds, and stores in {@code SimpleVectorStore}.
     * Optionally persists the store to the configured file path.
     *
     * <p>Call {@code ingest()} after adding/removing books, or at startup
     * to warm the vector store from the current catalogue state.
     */
    void ingest();

    /**
     * Answers a natural-language question about the library catalogue using
     * retrieval-augmented generation.
     *
     * <p>Performs a similarity search over the vector store, injects the top-k
     * chunks into the chat prompt as context, and returns the model's answer
     * together with the source chunks used.
     *
     * @param question the natural-language question (non-blank)
     * @return the grounded answer and the source chunks used
     */
    AnswerWithSources ask(String question);
}
