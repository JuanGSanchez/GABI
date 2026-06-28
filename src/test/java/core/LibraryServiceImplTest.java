package core;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.DataSourceBuilder;
import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit-integration tests for {@link LibraryServiceImpl} against in-memory Derby.
 *
 * <p>No Spring context is started — the DataSource and schema are created directly
 * via {@link InMemoryDerbyConfig} and {@link TestSchemaHelper}.
 * A {@link StubRagService} replaces the real RAG implementation so no Ollama is needed.
 *
 * <p>User tests that call GRANT/REVOKE use a separate in-memory Derby database
 * ({@code gabiTestUsers}) with SQL authorization pre-enabled.
 */
class LibraryServiceImplTest {

    private DataSource ds;
    private LibraryServiceImpl service;
    private StubRagService ragService;

    @BeforeEach
    void setUp() throws SQLException {
        ds = new InMemoryDerbyConfig().testDataSource();
        TestSchemaHelper.createSchema(ds);
        ragService = new StubRagService();
        service = new LibraryServiceImpl(ds, ragService);
    }

    // =========================================================================
    // BOOKS
    // =========================================================================

    @Nested
    @DisplayName("Books")
    class Books {

        @Test
        @DisplayName("listBooks returns empty list when no books exist")
        void listBooks_empty() {
            assertThat(service.listBooks()).isEmpty();
        }

        @Test
        @DisplayName("addBook persists and listBooks returns it")
        void addBook_and_list() {
            Book b = service.addBook(1, "Dune", "Herbert");
            assertThat(b.getID()).isEqualTo(1);
            assertThat(b.getTitle()).isEqualTo("Dune");
            assertThat(b.getAuthor()).isEqualTo("Herbert");
            assertThat(b.isLent()).isFalse();

            List<Book> all = service.listBooks();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getTitle()).isEqualTo("Dune");
        }

        @Test
        @DisplayName("addBook throws DuplicateException for same title+author")
        void addBook_duplicate() {
            service.addBook(1, "Dune", "Herbert");
            assertThatThrownBy(() -> service.addBook(2, "Dune", "Herbert"))
                    .isInstanceOf(LibraryException.DuplicateException.class);
        }

        @Test
        @DisplayName("addBook duplicate check is case-insensitive")
        void addBook_duplicateCaseInsensitive() {
            service.addBook(1, "Dune", "Herbert");
            assertThatThrownBy(() -> service.addBook(2, "dune", "HERBERT"))
                    .isInstanceOf(LibraryException.DuplicateException.class);
        }

        @Test
        @DisplayName("getBook returns present when book exists")
        void getBook_found() {
            service.addBook(1, "Foundation", "Asimov");
            Optional<Book> result = service.getBook(1);
            assertThat(result).isPresent();
            assertThat(result.get().getTitle()).isEqualTo("Foundation");
        }

        @Test
        @DisplayName("getBook returns empty when book does not exist")
        void getBook_notFound() {
            Optional<Book> result = service.getBook(999);
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("searchBooks by title returns matching books")
        void searchBooks_byTitle() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");
            List<Book> result = service.searchBooks("title", "dun");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getTitle()).isEqualTo("Dune");
        }

        @Test
        @DisplayName("searchBooks by author returns matching books")
        void searchBooks_byAuthor() {
            service.addBook(1, "Foundation", "Asimov");
            service.addBook(2, "I Robot", "Asimov");
            List<Book> result = service.searchBooks("author", "asimov");
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("searchBooks throws NotFoundException when no matches")
        void searchBooks_noMatch() {
            service.addBook(1, "Dune", "Herbert");
            assertThatThrownBy(() -> service.searchBooks("title", "xyz_no_match"))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("deleteBook removes the book and returns new max id")
        void deleteBook_success() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");
            int newMax = service.deleteBook(1);
            // max remaining is book 2
            assertThat(newMax).isEqualTo(2);
            assertThat(service.listBooks()).hasSize(1);
        }

        @Test
        @DisplayName("deleteBook returns 0 when catalogue becomes empty")
        void deleteBook_lastBook() {
            service.addBook(1, "Dune", "Herbert");
            int newMax = service.deleteBook(1);
            assertThat(newMax).isEqualTo(0);
            assertThat(service.listBooks()).isEmpty();
        }

        @Test
        @DisplayName("deleteBook throws NotFoundException for nonexistent id")
        void deleteBook_notFound() {
            assertThatThrownBy(() -> service.deleteBook(999))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("deleteBook throws BusinessRuleException when book is lent out")
        void deleteBook_lentOut() throws SQLException {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);

            assertThatThrownBy(() -> service.deleteBook(1))
                    .isInstanceOf(LibraryException.BusinessRuleException.class)
                    .hasMessageContaining("lent out");
        }

        @Test
        @DisplayName("countBooks returns [count, maxId] correctly")
        void countBooks() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");
            int[] result = service.countBooks();
            assertThat(result[0]).isEqualTo(2);
            assertThat(result[1]).isEqualTo(2);
        }

        @Test
        @DisplayName("countBooks returns [0, 0] when table is empty")
        void countBooks_empty() {
            int[] result = service.countBooks();
            assertThat(result[0]).isEqualTo(0);
            assertThat(result[1]).isEqualTo(0);
        }
    }

    // =========================================================================
    // MEMBERS
    // =========================================================================

    @Nested
    @DisplayName("Members")
    class Members {

        @Test
        @DisplayName("listMembers returns empty list when no members exist")
        void listMembers_empty() {
            assertThat(service.listMembers()).isEmpty();
        }

        @Test
        @DisplayName("addMember persists and listMembers returns it")
        void addMember_and_list() {
            Member m = service.addMember(1, "Alice", "Smith");
            assertThat(m.getID()).isEqualTo(1);
            assertThat(m.getName()).isEqualTo("Alice");
            assertThat(m.getSurname()).isEqualTo("Smith");

            List<Member> all = service.listMembers();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("addMember throws DuplicateException for same name+surname")
        void addMember_duplicate() {
            service.addMember(1, "Alice", "Smith");
            assertThatThrownBy(() -> service.addMember(2, "Alice", "Smith"))
                    .isInstanceOf(LibraryException.DuplicateException.class);
        }

        @Test
        @DisplayName("addMember duplicate check is case-insensitive")
        void addMember_duplicateCaseInsensitive() {
            service.addMember(1, "Alice", "Smith");
            assertThatThrownBy(() -> service.addMember(2, "ALICE", "smith"))
                    .isInstanceOf(LibraryException.DuplicateException.class);
        }

        @Test
        @DisplayName("getMember returns present when member exists")
        void getMember_found() {
            service.addMember(1, "Bob", "Jones");
            Optional<Member> result = service.getMember(1);
            assertThat(result).isPresent();
            assertThat(result.get().getName()).isEqualTo("Bob");
        }

        @Test
        @DisplayName("getMember returns empty when member does not exist")
        void getMember_notFound() {
            assertThat(service.getMember(999)).isEmpty();
        }

        @Test
        @DisplayName("searchMembers by name returns matching members")
        void searchMembers_byName() {
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Jones");
            List<Member> result = service.searchMembers("name", "ali");
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getName()).isEqualTo("Alice");
        }

        @Test
        @DisplayName("searchMembers by surname returns matching members")
        void searchMembers_bySurname() {
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Smith");
            List<Member> result = service.searchMembers("surname", "smith");
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("searchMembers throws NotFoundException when no matches")
        void searchMembers_noMatch() {
            service.addMember(1, "Alice", "Smith");
            assertThatThrownBy(() -> service.searchMembers("name", "xyz_no_match"))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("deleteMember removes the member and returns new max id")
        void deleteMember_success() {
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Jones");
            int newMax = service.deleteMember(1);
            assertThat(newMax).isEqualTo(2);
            assertThat(service.listMembers()).hasSize(1);
        }

        @Test
        @DisplayName("deleteMember returns 0 when table becomes empty")
        void deleteMember_lastMember() {
            service.addMember(1, "Alice", "Smith");
            int newMax = service.deleteMember(1);
            assertThat(newMax).isEqualTo(0);
        }

        @Test
        @DisplayName("deleteMember throws NotFoundException for nonexistent id")
        void deleteMember_notFound() {
            assertThatThrownBy(() -> service.deleteMember(999))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("deleteMember throws BusinessRuleException when member has active loans")
        void deleteMember_hasActiveLoans() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);

            assertThatThrownBy(() -> service.deleteMember(1))
                    .isInstanceOf(LibraryException.BusinessRuleException.class)
                    .hasMessageContaining("active loans");
        }

        @Test
        @DisplayName("countMembers returns [count, maxId] correctly")
        void countMembers() {
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Jones");
            int[] result = service.countMembers();
            assertThat(result[0]).isEqualTo(2);
            assertThat(result[1]).isEqualTo(2);
        }

        @Test
        @DisplayName("countMembers returns [0, 0] when table is empty")
        void countMembers_empty() {
            int[] result = service.countMembers();
            assertThat(result[0]).isEqualTo(0);
            assertThat(result[1]).isEqualTo(0);
        }

        @Test
        @DisplayName("listMembersWithLoans returns members with their borrowed books")
        void listMembersWithLoans_withBorrowedBook() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);

            List<Member> members = service.listMembersWithLoans();
            assertThat(members).hasSize(1);
            // Member has one book in listBook
            assertThat(members.get(0).getListBook()).hasSize(1);
            assertThat(members.get(0).getListBook().get(0).getTitle()).isEqualTo("Dune");
        }

        @Test
        @DisplayName("listMembersWithLoans returns empty list books when no loans")
        void listMembersWithLoans_noLoans() {
            service.addMember(1, "Alice", "Smith");
            List<Member> members = service.listMembersWithLoans();
            assertThat(members).hasSize(1);
            assertThat(members.get(0).getListBook()).isEmpty();
        }
    }

    // =========================================================================
    // LOANS
    // =========================================================================

    @Nested
    @DisplayName("Loans")
    class Loans {

        @Test
        @DisplayName("listLoans returns empty list when no loans exist")
        void listLoans_empty() {
            assertThat(service.listLoans()).isEmpty();
        }

        @Test
        @DisplayName("createLoan persists and listLoans returns it")
        void createLoan_and_list() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");

            Loan loan = service.createLoan(1, 1, 1);
            assertThat(loan.getID()).isEqualTo(1);
            assertThat(loan.getIdMember()).isEqualTo(1);
            assertThat(loan.getIdBook()).isEqualTo(1);
            assertThat(loan.getDateLoan()).isNotNull();

            // book should now be marked as lent
            assertThat(service.getBook(1)).hasValueSatisfying(b -> assertThat(b.isLent()).isTrue());

            List<Loan> all = service.listLoans();
            assertThat(all).hasSize(1);
        }

        @Test
        @DisplayName("createLoan throws BusinessRuleException when book is already lent")
        void createLoan_bookAlreadyLent() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Jones");
            service.createLoan(1, 1, 1);

            assertThatThrownBy(() -> service.createLoan(2, 2, 1))
                    .isInstanceOf(LibraryException.BusinessRuleException.class)
                    .hasMessageContaining("already lent out");
        }

        @Test
        @DisplayName("createLoan throws BusinessRuleException when book does not exist")
        void createLoan_bookNotFound() {
            service.addMember(1, "Alice", "Smith");
            assertThatThrownBy(() -> service.createLoan(1, 1, 999))
                    .isInstanceOf(LibraryException.BusinessRuleException.class)
                    .hasMessageContaining("Book not found");
        }

        @Test
        @DisplayName("returnLoan marks book as available and removes the loan")
        void returnLoan_success() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);

            // confirm book is lent before return
            assertThat(service.getBook(1)).hasValueSatisfying(b -> assertThat(b.isLent()).isTrue());

            int newMax = service.returnLoan(1);
            assertThat(newMax).isEqualTo(0); // no loans remain

            // book should be available again
            assertThat(service.getBook(1)).hasValueSatisfying(b -> assertThat(b.isLent()).isFalse());
            assertThat(service.listLoans()).isEmpty();
        }

        @Test
        @DisplayName("returnLoan throws NotFoundException for nonexistent loan")
        void returnLoan_notFound() {
            assertThatThrownBy(() -> service.returnLoan(999))
                    .isInstanceOf(LibraryException.NotFoundException.class)
                    .hasMessageContaining("Loan not found");
        }

        @Test
        @DisplayName("listLoansByMember returns loans for a specific member")
        void listLoansByMember_found() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");
            service.addMember(1, "Alice", "Smith");
            service.addMember(2, "Bob", "Jones");
            service.createLoan(1, 1, 1);
            service.createLoan(2, 2, 2);

            List<Loan> aliceLoans = service.listLoansByMember(1);
            assertThat(aliceLoans).hasSize(1);
            assertThat(aliceLoans.get(0).getIdBook()).isEqualTo(1);
        }

        @Test
        @DisplayName("listLoansByMember throws NotFoundException when no loans for member")
        void listLoansByMember_noLoans() {
            service.addMember(1, "Alice", "Smith");
            assertThatThrownBy(() -> service.listLoansByMember(1))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("listLoansWithDetails returns loans with resolved member and book")
        void listLoansWithDetails() {
            service.addBook(1, "Dune", "Herbert");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);

            List<Loan> loans = service.listLoansWithDetails();
            assertThat(loans).hasSize(1);
            Loan loan = loans.get(0);
            assertThat(loan.getMember()).isNotNull();
            assertThat(loan.getMember().getName()).isEqualTo("Alice");
            assertThat(loan.getBook()).isNotNull();
            assertThat(loan.getBook().getTitle()).isEqualTo("Dune");
        }

        @Test
        @DisplayName("countLoans returns [count, maxId] correctly")
        void countLoans() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");
            service.addMember(1, "Alice", "Smith");
            service.createLoan(1, 1, 1);
            service.createLoan(2, 1, 2);

            int[] result = service.countLoans();
            assertThat(result[0]).isEqualTo(2);
            assertThat(result[1]).isEqualTo(2);
        }

        @Test
        @DisplayName("countLoans returns [0, 0] when table is empty")
        void countLoans_empty() {
            int[] result = service.countLoans();
            assertThat(result[0]).isEqualTo(0);
            assertThat(result[1]).isEqualTo(0);
        }
    }

    // =========================================================================
    // USERS
    // =========================================================================

    /**
     * User tests use a SEPARATE in-memory Derby database ({@code gabiTestUsers})
     * with SQL authorization enabled, because GRANT/REVOKE in addUser requires
     * {@code derby.database.sqlAuthorization=TRUE} (only active after a DB reboot).
     */
    @Nested
    @DisplayName("Users")
    class Users {

        private static final String USERS_DB_NAME = "gabiTestUsers";
        private static DataSource userDs;
        private static LibraryServiceImpl userService;
        private static StubRagService userRagService;

        @BeforeAll
        static void enableSqlAuth() throws SQLException {
            // Enable SQL authorization once and reboot so GRANT/REVOKE works
            TestSchemaHelper.enableSqlAuthorizationAndReboot(USERS_DB_NAME);
            // Build a DataSource to the now-rebooted DB
            userDs = DataSourceBuilder.create()
                    .driverClassName("org.apache.derby.jdbc.EmbeddedDriver")
                    .url("jdbc:derby:memory:" + USERS_DB_NAME + ";create=true")
                    .username("")
                    .password("")
                    .build();
        }

        @BeforeEach
        void setUpUsers() throws SQLException {
            TestSchemaHelper.createSchema(userDs);
            userRagService = new StubRagService();
            userService = new LibraryServiceImpl(userDs, userRagService);
        }

        @Test
        @DisplayName("listUsers returns empty list when no users exist")
        void listUsers_empty() {
            assertThat(userService.listUsers()).isEmpty();
        }

        @Test
        @DisplayName("addUser persists and listUsers returns it")
        void addUser_and_list() {
            User u = userService.addUser(1, "testuser1", "pass123".toCharArray());
            assertThat(u.getID()).isEqualTo(1);
            assertThat(u.getName()).isEqualTo("testuser1");

            List<User> all = userService.listUsers();
            assertThat(all).hasSize(1);
            assertThat(all.get(0).getName()).isEqualTo("testuser1");
        }

        @Test
        @DisplayName("addUser throws DuplicateException for existing username")
        void addUser_duplicate() {
            userService.addUser(1, "testuser1", "pass123".toCharArray());
            assertThatThrownBy(() -> userService.addUser(2, "testuser1", "pass456".toCharArray()))
                    .isInstanceOf(LibraryException.DuplicateException.class);
        }

        @Test
        @DisplayName("addUser throws InvalidIdentifierException for malicious username with spaces")
        void addUser_invalidUsername_space() {
            assertThatThrownBy(() -> userService.addUser(1, "bad user", "pass".toCharArray()))
                    .isInstanceOf(LibraryException.InvalidIdentifierException.class);
        }

        @Test
        @DisplayName("addUser throws InvalidIdentifierException for username with semicolon (SQL injection)")
        void addUser_invalidUsername_semicolon() {
            assertThatThrownBy(() -> userService.addUser(1, "user;DROP TABLE admin.books--", "pass".toCharArray()))
                    .isInstanceOf(LibraryException.InvalidIdentifierException.class);
        }

        @Test
        @DisplayName("addUser throws InvalidIdentifierException for username with single quote")
        void addUser_invalidUsername_quote() {
            assertThatThrownBy(() -> userService.addUser(1, "o'brien", "pass".toCharArray()))
                    .isInstanceOf(LibraryException.InvalidIdentifierException.class);
        }

        @Test
        @DisplayName("addUser throws InvalidIdentifierException for username with SQL comment")
        void addUser_invalidUsername_sqlComment() {
            assertThatThrownBy(() -> userService.addUser(1, "admin--", "pass".toCharArray()))
                    .isInstanceOf(LibraryException.InvalidIdentifierException.class);
        }

        @Test
        @DisplayName("addUser throws BusinessRuleException for reserved name 'admin'")
        void addUser_reservedName_admin() {
            assertThatThrownBy(() -> userService.addUser(1, "admin", "pass".toCharArray()))
                    .isInstanceOf(LibraryException.BusinessRuleException.class)
                    .hasMessageContaining("Reserved");
        }

        @Test
        @DisplayName("deleteUser removes user and returns new max id")
        void deleteUser_success() {
            userService.addUser(1, "testuser1", "pass1".toCharArray());
            userService.addUser(2, "testuser2", "pass2".toCharArray());

            int newMax = userService.deleteUser(1);
            assertThat(newMax).isEqualTo(2);
            assertThat(userService.listUsers()).hasSize(1);
            assertThat(userService.listUsers().get(0).getName()).isEqualTo("testuser2");
        }

        @Test
        @DisplayName("deleteUser returns 0 when table becomes empty")
        void deleteUser_lastUser() {
            userService.addUser(1, "testuser1", "pass1".toCharArray());
            int newMax = userService.deleteUser(1);
            assertThat(newMax).isEqualTo(0);
            assertThat(userService.listUsers()).isEmpty();
        }

        @Test
        @DisplayName("deleteUser throws NotFoundException for nonexistent id")
        void deleteUser_notFound() {
            assertThatThrownBy(() -> userService.deleteUser(999))
                    .isInstanceOf(LibraryException.NotFoundException.class);
        }

        @Test
        @DisplayName("countUsers returns [count, maxId] correctly")
        void countUsers() {
            userService.addUser(1, "testuser1", "pass1".toCharArray());
            userService.addUser(2, "testuser2", "pass2".toCharArray());
            int[] result = userService.countUsers();
            assertThat(result[0]).isEqualTo(2);
            assertThat(result[1]).isEqualTo(2);
        }
    }

    // =========================================================================
    // RAG delegation
    // =========================================================================

    @Nested
    @DisplayName("RAG delegation")
    class RagDelegation {

        @Test
        @DisplayName("ingest() fetches all books and delegates to RagService")
        void ingest_delegatesToRagService() {
            service.addBook(1, "Dune", "Herbert");
            service.addBook(2, "Foundation", "Asimov");

            service.ingest();

            assertThat(ragService.getLastIngestedBooks()).hasSize(2);
        }

        @Test
        @DisplayName("ingest() with empty catalogue delegates empty list")
        void ingest_emptyBooks() {
            service.ingest();
            assertThat(ragService.getLastIngestedBooks()).isEmpty();
        }

        @Test
        @DisplayName("ask() delegates to RagService and returns result")
        void ask_delegatesToRagService() {
            AnswerWithSources expected = new AnswerWithSources("test answer", List.of("source1"));
            ragService.setAnswer(expected);

            AnswerWithSources result = service.ask("What books do you have?");

            assertThat(ragService.getLastQuestion()).isEqualTo("What books do you have?");
            assertThat(result.answer()).isEqualTo("test answer");
            assertThat(result.sources()).containsExactly("source1");
        }

        @Test
        @DisplayName("ask() with blank question throws LibraryException")
        void ask_blankQuestion() {
            assertThatThrownBy(() -> service.ask("  "))
                    .isInstanceOf(LibraryException.class)
                    .hasMessageContaining("blank");
        }

        @Test
        @DisplayName("ask() with null question throws LibraryException")
        void ask_nullQuestion() {
            assertThatThrownBy(() -> service.ask(null))
                    .isInstanceOf(LibraryException.class);
        }
    }
}
