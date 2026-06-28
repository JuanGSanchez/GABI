package ui.desktop;

import core.AnswerWithSources;
import core.LibraryException;
import core.LibraryService;
import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * In-memory {@link LibraryService} test double for the desktop adapter tests.
 *
 * <p>It stands in for the real DataSource-backed core so the Swing panels can be exercised
 * without a Derby instance. It carries the same external contract (typed exceptions,
 * {@code [count,maxId]} pairs, {@code Optional} lookups) so a panel test against this fake
 * is behaviourally equivalent to one against {@code LibraryServiceImpl}.
 */
public class FakeLibraryService implements LibraryService {

    final List<Book> books = new ArrayList<>();
    final List<Member> members = new ArrayList<>();
    final List<Loan> loans = new ArrayList<>();
    final List<User> users = new ArrayList<>();

    private static <E> int[] countOf(List<E> list, java.util.function.ToIntFunction<E> id) {
        int max = list.stream().mapToInt(id).max().orElse(0);
        return new int[]{list.size(), max};
    }

    // ---- Books --------------------------------------------------------------
    @Override public List<Book> listBooks() { return new ArrayList<>(books); }

    @Override public List<Book> searchBooks(String field, String text) {
        List<Book> hits = books.stream().filter(b ->
                ("author".equalsIgnoreCase(field) ? b.getAuthor() : b.getTitle())
                        .toLowerCase().contains(text.toLowerCase())).toList();
        if (hits.isEmpty()) {
            throw new LibraryException.NotFoundException("no books");
        }
        return hits;
    }

    @Override public Optional<Book> getBook(int id) {
        return books.stream().filter(b -> b.getID() == id).findFirst();
    }

    @Override public Book addBook(int id, String title, String author) {
        Book b = new Book(id, title, author);
        if (books.contains(b)) {
            throw new LibraryException.DuplicateException("dup");
        }
        books.add(b);
        return b;
    }

    @Override public int deleteBook(int id) {
        books.removeIf(b -> b.getID() == id);
        return countOf(books, Book::getID)[1];
    }

    @Override public int[] countBooks() { return countOf(books, Book::getID); }

    // ---- Members ------------------------------------------------------------
    @Override public List<Member> listMembers() { return new ArrayList<>(members); }
    @Override public List<Member> listMembersWithLoans() { return new ArrayList<>(members); }

    @Override public List<Member> searchMembers(String field, String text) {
        return members.stream().filter(m ->
                ("surname".equalsIgnoreCase(field) ? m.getSurname() : m.getName())
                        .toLowerCase().contains(text.toLowerCase())).toList();
    }

    @Override public Optional<Member> getMember(int id) {
        return members.stream().filter(m -> m.getID() == id).findFirst();
    }

    @Override public Member addMember(int id, String name, String surname) {
        Member m = new Member(id, name, surname);
        members.add(m);
        return m;
    }

    @Override public int deleteMember(int id) {
        members.removeIf(m -> m.getID() == id);
        return countOf(members, Member::getID)[1];
    }

    @Override public int[] countMembers() { return countOf(members, Member::getID); }

    // ---- Loans --------------------------------------------------------------
    @Override public List<Loan> listLoans() { return new ArrayList<>(loans); }
    @Override public List<Loan> listLoansWithDetails() { return new ArrayList<>(loans); }

    @Override public List<Loan> listLoansByMember(int memberId) {
        return loans.stream().filter(l -> l.getIdMember() == memberId).toList();
    }

    @Override public Loan createLoan(int loanId, int memberId, int bookId) {
        Loan l = new Loan(loanId, memberId, bookId);
        loans.add(l);
        return l;
    }

    @Override public int returnLoan(int loanId) {
        loans.removeIf(l -> l.getID() == loanId);
        return countOf(loans, Loan::getID)[1];
    }

    @Override public int[] countLoans() { return countOf(loans, Loan::getID); }

    int loanPeriodDays = core.LoanPolicy.DEFAULT_PERIOD_DAYS;

    @Override public int loanPeriodDays() { return loanPeriodDays; }

    @Override public java.time.LocalDate dueDate(Loan loan) {
        return new core.LoanPolicy(loanPeriodDays).dueDate(loan);
    }

    @Override public List<Loan> listOverdueLoans() {
        core.LoanPolicy policy = new core.LoanPolicy(loanPeriodDays);
        java.time.LocalDate today = java.time.LocalDate.now();
        return loans.stream().filter(l -> policy.isOverdue(l, today)).toList();
    }

    // ---- Users --------------------------------------------------------------
    @Override public List<User> listUsers() { return new ArrayList<>(users); }

    @Override public User addUser(int id, String loginName, char[] pw) {
        User u = new User(id, loginName);
        users.add(u);
        return u;
    }

    @Override public int deleteUser(int id) {
        users.removeIf(u -> u.getID() == id);
        return countOf(users, u -> u.getID())[1];
    }

    @Override public int[] countUsers() { return countOf(users, u -> u.getID()); }

    // ---- RAG (unused by the desktop panels) ---------------------------------
    @Override public void ingest() { /* no-op */ }

    @Override public AnswerWithSources ask(String question) {
        return new AnswerWithSources("", List.of());
    }
}
