package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import rag.RagService;
import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

/**
 * DataSource-backed implementation of {@link LibraryService}.
 *
 * <p>Key changes from the legacy singleton DAOs:
 * <ul>
 *   <li>Injects a Spring-managed {@link DataSource} — no per-call credentials (D-3/D-7 fix).</li>
 *   <li>Returns typed values and throws {@link LibraryException} subtypes — no localized strings,
 *       no {@code System.out/err} calls (D-5/D-11 fix).  SLF4J is used for diagnostics.</li>
 *   <li>All user-supplied SQL identifiers pass through {@link IdentifierValidator} (D-4 fix).</li>
 *   <li>All data values are bound via {@code PreparedStatement} parameters.</li>
 *   <li>{@code null} sentinels replaced with {@code Optional} / typed returns (D-8 fix).</li>
 * </ul>
 *
 * <p>The {@link Properties} {@code configProps} are read from the classpath resource
 * {@code /utils/configuration.properties} (loaded via {@code utils.Utils}) to keep
 * the table/field names configurable without hard-wiring them here.
 *
 * @author JuanGS (workstream b — headless core)
 */
@Service
public class LibraryServiceImpl implements LibraryService {

    private static final Logger log = LoggerFactory.getLogger(LibraryServiceImpl.class);

    private final DataSource dataSource;
    private final RagService ragService;

    // ---- schema constants (loaded from configuration.properties) ----------
    private final String schemaName;
    private final String bookTable;
    private final String memberTable;
    private final String loanTable;
    private final String userTable;
    // book fields
    private final String bId;
    private final String bTitle;
    private final String bAuthor;
    private final String bLent;
    // member fields
    private final String mId;
    private final String mName;
    private final String mSurname;
    // loan fields
    private final String lId;
    private final String lDate;
    // user fields
    private final String uId;
    private final String uName;
    // limits
    private final int maxMembers;
    private final int maxLoan;
    private final int maxUsers;

    public LibraryServiceImpl(DataSource dataSource, RagService ragService) {
        this.dataSource  = dataSource;
        this.ragService  = ragService;

        Properties p = utils.Utils.readProperties();
        schemaName   = p.getProperty("database-name", "admin");
        bookTable    = schemaName + "." + p.getProperty("database-table-1", "books");
        memberTable  = schemaName + "." + p.getProperty("database-table-2", "members");
        loanTable    = schemaName + "." + p.getProperty("database-table-3", "loans");
        userTable    = schemaName + "." + p.getProperty("database-table-4", "users");
        bId      = p.getProperty("database-table-1-field-1", "idBook");
        bTitle   = p.getProperty("database-table-1-field-2", "title");
        bAuthor  = p.getProperty("database-table-1-field-3", "author");
        bLent    = p.getProperty("database-table-1-field-4", "lent");
        mId      = p.getProperty("database-table-2-field-1", "idMember");
        mName    = p.getProperty("database-table-2-field-2", "name");
        mSurname = p.getProperty("database-table-2-field-3", "surname");
        lId      = p.getProperty("database-table-3-field-1", "idLoan");
        lDate    = p.getProperty("database-table-3-field-4", "dateLoan");
        uId      = p.getProperty("database-table-4-field-1", "idUser");
        uName    = p.getProperty("database-table-4-field-2", "name");
        maxMembers = Integer.parseInt(p.getProperty("database-table-2-maxsocs", "50"));
        maxLoan    = Integer.parseInt(p.getProperty("database-table-2-maxloan", "10"));
        maxUsers   = Integer.parseInt(p.getProperty("database-user-maxusers", "10"));
        this.loanPolicy = new LoanPolicy(Integer.parseInt(
                p.getProperty("loan-period-days", String.valueOf(LoanPolicy.DEFAULT_PERIOD_DAYS))));
    }

    /** Loan-period policy for derived due dates / overdue tracking (SPEC-19). */
    private final LoanPolicy loanPolicy;

    // =========================================================================
    // BOOKS
    // =========================================================================

    @Override
    public List<Book> listBooks() {
        String sql = "SELECT * FROM " + bookTable;
        List<Book> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapBook(rs));
            }
        } catch (SQLException e) {
            log.error("listBooks failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list books", e);
        }
        return list;
    }

    @Override
    public List<Book> searchBooks(String field, String text) {
        String col = resolveBookSearchColumn(field);
        String sql = "SELECT * FROM " + bookTable + " WHERE LOWER(" + col + ") LIKE LOWER(?)";
        List<Book> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapBook(rs));
            }
        } catch (SQLException e) {
            log.error("searchBooks failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to search books", e);
        }
        if (list.isEmpty()) {
            throw new LibraryException.NotFoundException("No books found matching " + field + "='" + text + "'");
        }
        return list;
    }

    @Override
    public core.search.Page<Book> searchBooksPaged(String query, int page, int size,
                                                   String sortField, boolean ascending) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        // Sort identifier comes from a fixed whitelist and is re-validated against the
        // [A-Za-z0-9_] allowlist before interpolation (SPEC-07): never raw user input.
        String sortColumn = IdentifierValidator.validate(resolveBookSortColumn(sortField), "sort column");
        String direction = ascending ? "ASC" : "DESC";
        String pattern = "%" + (query == null ? "" : query.toLowerCase()) + "%";

        String where = " WHERE LOWER(" + bTitle + ") LIKE ? OR LOWER(" + bAuthor + ") LIKE ?";
        String countSql = "SELECT COUNT(*) FROM " + bookTable + where;
        String pageSql  = "SELECT * FROM " + bookTable + where
                + " ORDER BY " + sortColumn + " " + direction
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Book> content = new ArrayList<>();
        long total;
        try (Connection c = dataSource.getConnection();
             PreparedStatement countPs = c.prepareStatement(countSql);
             PreparedStatement pagePs  = c.prepareStatement(pageSql)) {
            countPs.setString(1, pattern);
            countPs.setString(2, pattern);
            try (ResultSet rs = countPs.executeQuery()) {
                total = rs.next() ? rs.getLong(1) : 0L;
            }
            pagePs.setString(1, pattern);
            pagePs.setString(2, pattern);
            pagePs.setInt(3, safePage * safeSize);
            pagePs.setInt(4, safeSize);
            try (ResultSet rs = pagePs.executeQuery()) {
                while (rs.next()) content.add(mapBook(rs));
            }
        } catch (SQLException e) {
            log.error("searchBooksPaged failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to search books (paged)", e);
        }
        return new core.search.Page<>(content, safePage, safeSize, total);
    }

    private String resolveBookSortColumn(String field) {
        if (field == null) return bId;
        return switch (field.toLowerCase()) {
            case "title"  -> bTitle;
            case "author" -> bAuthor;
            default        -> bId;
        };
    }

    @Override
    public Optional<Book> getBook(int id) {
        String sql = "SELECT * FROM " + bookTable + " WHERE " + bId + " = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapBook(rs));
            }
        } catch (SQLException e) {
            log.error("getBook({}) failed: {}", id, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to get book " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Book addBook(int id, String title, String author) {
        // duplicate check
        String checkSql = "SELECT * FROM " + bookTable +
                          " WHERE LOWER(" + bTitle + ") = LOWER(?) AND LOWER(" + bAuthor + ") = LOWER(?)";
        String insertSql = "INSERT INTO " + bookTable + " VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement check  = c.prepareStatement(checkSql);
             PreparedStatement insert = c.prepareStatement(insertSql)) {
            check.setString(1, title);
            check.setString(2, author);
            try (ResultSet rs = check.executeQuery()) {
                if (rs.next()) throw new LibraryException.DuplicateException(
                        "Book already exists: title='" + title + "', author='" + author + "'");
            }
            insert.setInt(1, id);
            insert.setString(2, title);
            insert.setString(3, author);
            insert.setBoolean(4, false);
            if (insert.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("INSERT book returned 0 rows", null);
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("addBook failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to add book", e);
        }
        log.info("Book added: id={}, title='{}', author='{}'", id, title, author);
        return new Book(id, title, author, false);
    }

    @Override
    public int deleteBook(int id) {
        String checkLent = "SELECT " + bLent + " FROM " + bookTable + " WHERE " + bId + " = ?";
        String delete    = "DELETE FROM " + bookTable + " WHERE " + bId + " = ?";
        String maxId     = "SELECT " + bId + " FROM " + bookTable +
                           " WHERE " + bId + " = (SELECT MAX(" + bId + ") FROM " + bookTable + ")";
        try (Connection c = dataSource.getConnection();
             PreparedStatement psCheck  = c.prepareStatement(checkLent);
             PreparedStatement psDelete = c.prepareStatement(delete);
             Statement stmtMax = c.createStatement()) {
            psCheck.setInt(1, id);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (!rs.next()) throw new LibraryException.NotFoundException("Book not found: id=" + id);
                if (rs.getBoolean(1)) throw new LibraryException.BusinessRuleException(
                        "Cannot delete book id=" + id + ": it is currently lent out");
            }
            psDelete.setInt(1, id);
            if (psDelete.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("DELETE book returned 0 rows", null);
            }
            try (ResultSet rs = stmtMax.executeQuery(maxId)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("deleteBook({}) failed: {}", id, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to delete book " + id, e);
        }
    }

    @Override
    public int[] countBooks() {
        return countTable(bookTable, bId);
    }

    // =========================================================================
    // MEMBERS
    // =========================================================================

    @Override
    public List<Member> listMembers() {
        String sql = "SELECT * FROM " + memberTable;
        List<Member> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapMember(rs));
        } catch (SQLException e) {
            log.error("listMembers failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list members", e);
        }
        return list;
    }

    @Override
    public List<Member> listMembersWithLoans() {
        String memberSql = "SELECT * FROM " + memberTable;
        String bookIdSql  = "SELECT " + bId + " FROM " + loanTable + " WHERE " + mId + " = ?";
        String bookSql    = "SELECT * FROM " + bookTable + " WHERE " + bId + " = ?";
        List<Member> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement stmt = c.createStatement();
             PreparedStatement psBid = c.prepareStatement(bookIdSql);
             PreparedStatement psBk  = c.prepareStatement(bookSql);
             ResultSet rs = stmt.executeQuery(memberSql)) {
            while (rs.next()) {
                int memberId = rs.getInt(1);
                psBid.setInt(1, memberId);
                List<Book> books = new ArrayList<>();
                try (ResultSet rsBid = psBid.executeQuery()) {
                    while (rsBid.next()) {
                        psBk.setInt(1, rsBid.getInt(1));
                        try (ResultSet rsBk = psBk.executeQuery()) {
                            if (rsBk.next()) books.add(mapBook(rsBk));
                        }
                    }
                }
                list.add(new Member(memberId, rs.getString(2), rs.getString(3), books));
            }
        } catch (SQLException e) {
            log.error("listMembersWithLoans failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list members with loans", e);
        }
        return list;
    }

    @Override
    public List<Member> searchMembers(String field, String text) {
        String col = field.equalsIgnoreCase("surname") ? mSurname : mName;
        String sql = "SELECT * FROM " + memberTable + " WHERE LOWER(" + col + ") LIKE LOWER(?)";
        List<Member> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, "%" + text + "%");
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapMember(rs));
            }
        } catch (SQLException e) {
            log.error("searchMembers failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to search members", e);
        }
        if (list.isEmpty()) throw new LibraryException.NotFoundException(
                "No members found matching " + field + "='" + text + "'");
        return list;
    }

    @Override
    public core.search.Page<Member> searchMembersPaged(String query, int page, int size,
                                                       String sortField, boolean ascending) {
        int safePage = Math.max(page, 0);
        int safeSize = size <= 0 ? 20 : size;
        // Sort identifier comes from a fixed whitelist and is re-validated against the
        // [A-Za-z0-9_] allowlist before interpolation (SPEC-07): never raw user input.
        String sortColumn = IdentifierValidator.validate(resolveMemberSortColumn(sortField), "sort column");
        String direction = ascending ? "ASC" : "DESC";
        String pattern = "%" + (query == null ? "" : query.toLowerCase()) + "%";

        String where = " WHERE LOWER(" + mName + ") LIKE ? OR LOWER(" + mSurname + ") LIKE ?";
        String countSql = "SELECT COUNT(*) FROM " + memberTable + where;
        String pageSql  = "SELECT * FROM " + memberTable + where
                + " ORDER BY " + sortColumn + " " + direction
                + " OFFSET ? ROWS FETCH NEXT ? ROWS ONLY";

        List<Member> content = new ArrayList<>();
        long total;
        try (Connection c = dataSource.getConnection();
             PreparedStatement countPs = c.prepareStatement(countSql);
             PreparedStatement pagePs  = c.prepareStatement(pageSql)) {
            countPs.setString(1, pattern);
            countPs.setString(2, pattern);
            try (ResultSet rs = countPs.executeQuery()) {
                total = rs.next() ? rs.getLong(1) : 0L;
            }
            pagePs.setString(1, pattern);
            pagePs.setString(2, pattern);
            pagePs.setInt(3, safePage * safeSize);
            pagePs.setInt(4, safeSize);
            try (ResultSet rs = pagePs.executeQuery()) {
                while (rs.next()) content.add(mapMember(rs));
            }
        } catch (SQLException e) {
            log.error("searchMembersPaged failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to search members (paged)", e);
        }
        return new core.search.Page<>(content, safePage, safeSize, total);
    }

    private String resolveMemberSortColumn(String field) {
        if (field == null) return mId;
        return switch (field.toLowerCase()) {
            case "name"    -> mName;
            case "surname" -> mSurname;
            default        -> mId;
        };
    }

    @Override
    public Optional<Member> getMember(int id) {
        String sql = "SELECT * FROM " + memberTable + " WHERE " + mId + " = ?";
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(mapMember(rs));
            }
        } catch (SQLException e) {
            log.error("getMember({}) failed: {}", id, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to get member " + id, e);
        }
        return Optional.empty();
    }

    @Override
    public Member addMember(int id, String name, String surname) {
        String countSql = "SELECT COUNT(*) FROM " + memberTable;
        String checkSql = "SELECT * FROM " + memberTable +
                          " WHERE LOWER(" + mName + ") = LOWER(?) AND LOWER(" + mSurname + ") = LOWER(?)";
        String insertSql = "INSERT INTO " + memberTable + " VALUES (?,?,?)";
        try (Connection c = dataSource.getConnection();
             Statement stmtCount = c.createStatement();
             PreparedStatement psCheck  = c.prepareStatement(checkSql);
             PreparedStatement psInsert = c.prepareStatement(insertSql)) {
            try (ResultSet rs = stmtCount.executeQuery(countSql)) {
                rs.next();
                if (rs.getInt(1) >= maxMembers) throw new LibraryException.BusinessRuleException(
                        "Member limit reached: max=" + maxMembers);
            }
            psCheck.setString(1, name);
            psCheck.setString(2, surname);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) throw new LibraryException.DuplicateException(
                        "Member already registered: name='" + name + "' surname='" + surname + "'");
            }
            psInsert.setInt(1, id);
            psInsert.setString(2, name);
            psInsert.setString(3, surname);
            if (psInsert.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("INSERT member returned 0 rows", null);
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("addMember failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to add member", e);
        }
        log.info("Member added: id={}, name='{}', surname='{}'", id, name, surname);
        return new Member(id, name, surname);
    }

    @Override
    public int deleteMember(int id) {
        // check no active loans
        String loanCheck = "SELECT " + mId + " FROM " + loanTable + " WHERE " + mId + " = ?";
        String delete     = "DELETE FROM " + memberTable + " WHERE " + mId + " = ?";
        String maxId      = "SELECT " + mId + " FROM " + memberTable +
                            " WHERE " + mId + " = (SELECT MAX(" + mId + ") FROM " + memberTable + ")";
        try (Connection c = dataSource.getConnection();
             PreparedStatement psLoan = c.prepareStatement(loanCheck);
             PreparedStatement psDel  = c.prepareStatement(delete);
             Statement stmtMax = c.createStatement()) {
            psLoan.setInt(1, id);
            try (ResultSet rs = psLoan.executeQuery()) {
                if (rs.next()) throw new LibraryException.BusinessRuleException(
                        "Cannot delete member id=" + id + ": member has active loans");
            }
            psDel.setInt(1, id);
            if (psDel.executeUpdate() != 1) throw new LibraryException.NotFoundException(
                    "Member not found: id=" + id);
            try (ResultSet rs = stmtMax.executeQuery(maxId)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("deleteMember({}) failed: {}", id, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to delete member " + id, e);
        }
    }

    @Override
    public int[] countMembers() {
        return countTable(memberTable, mId);
    }

    // =========================================================================
    // LOANS
    // =========================================================================

    @Override
    public List<Loan> listLoans() {
        String sql = "SELECT * FROM " + loanTable;
        List<Loan> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) list.add(mapLoan(rs));
        } catch (SQLException e) {
            log.error("listLoans failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list loans", e);
        }
        return list;
    }

    @Override
    public List<Loan> listLoansWithDetails() {
        String loanSql   = "SELECT * FROM " + loanTable;
        String memberSql = "SELECT * FROM " + memberTable + " WHERE " + mId + " = ?";
        String bookSql   = "SELECT * FROM " + bookTable   + " WHERE " + bId + " = ?";
        List<Loan> list  = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement stmt   = c.createStatement();
             PreparedStatement psMember = c.prepareStatement(memberSql);
             PreparedStatement psBook   = c.prepareStatement(bookSql);
             ResultSet rs = stmt.executeQuery(loanSql)) {
            while (rs.next()) {
                int loanId   = rs.getInt(1);
                int memberId = rs.getInt(2);
                int bookId   = rs.getInt(3);
                LocalDate date = rs.getDate(4).toLocalDate();
                psMember.setInt(1, memberId);
                psMember.clearParameters();
                psMember.setInt(1, memberId);
                psBook.setInt(1, bookId);
                Member member = null;
                Book book = null;
                try (ResultSet rm = psMember.executeQuery()) {
                    if (rm.next()) member = mapMember(rm);
                }
                psBook.setInt(1, bookId);
                try (ResultSet rb = psBook.executeQuery()) {
                    if (rb.next()) book = mapBook(rb);
                }
                list.add(new Loan(loanId, memberId, bookId, date, member, book));
            }
        } catch (SQLException e) {
            log.error("listLoansWithDetails failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list loans with details", e);
        }
        return list;
    }

    @Override
    public int loanPeriodDays() {
        return loanPolicy.loanPeriodDays();
    }

    @Override
    public LocalDate dueDate(Loan loan) {
        return loanPolicy.dueDate(loan);
    }

    @Override
    public List<Loan> listOverdueLoans() {
        LocalDate today = LocalDate.now();
        List<Loan> overdue = new ArrayList<>();
        for (Loan loan : listLoansWithDetails()) {
            if (loanPolicy.isOverdue(loan, today)) {
                overdue.add(loan);
            }
        }
        return overdue;
    }

    @Override
    public List<Loan> listLoansByMember(int memberId) {
        String sql = "SELECT * FROM " + loanTable + " WHERE " + mId + " = ?";
        List<Loan> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, memberId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapLoan(rs));
            }
        } catch (SQLException e) {
            log.error("listLoansByMember({}) failed: {}", memberId, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list loans by member", e);
        }
        if (list.isEmpty()) throw new LibraryException.NotFoundException(
                "No loans found for member id=" + memberId);
        return list;
    }

    @Override
    public Loan createLoan(int loanId, int memberId, int bookId) {
        String loanCountSql = "SELECT COUNT(*) FROM " + loanTable + " WHERE " + mId + " = ?";
        String bookLentSql  = "SELECT " + bLent + " FROM " + bookTable + " WHERE " + bId + " = ?";
        String markLentSql  = "UPDATE " + bookTable + " SET " + bLent + " = ? WHERE " + bId + " = ?";
        String insertSql    = "INSERT INTO " + loanTable + " VALUES (?,?,?,?)";
        try (Connection c = dataSource.getConnection();
             PreparedStatement psCount   = c.prepareStatement(loanCountSql);
             PreparedStatement psBookLent = c.prepareStatement(bookLentSql);
             PreparedStatement psMarkLent = c.prepareStatement(markLentSql);
             PreparedStatement psInsert  = c.prepareStatement(insertSql)) {
            psCount.setInt(1, memberId);
            try (ResultSet rs = psCount.executeQuery()) {
                rs.next();
                if (rs.getInt(1) >= maxLoan) throw new LibraryException.BusinessRuleException(
                        "Loan limit reached for member id=" + memberId + " (max=" + maxLoan + ")");
            }
            psBookLent.setInt(1, bookId);
            try (ResultSet rs = psBookLent.executeQuery()) {
                if (!rs.next()) throw new LibraryException.BusinessRuleException(
                        "Book not found: id=" + bookId);
                if (rs.getBoolean(1)) throw new LibraryException.BusinessRuleException(
                        "Book id=" + bookId + " is already lent out");
            }
            psMarkLent.setBoolean(1, true);
            psMarkLent.setInt(2, bookId);
            int updated = psMarkLent.executeUpdate();
            if (updated > 1) {
                psMarkLent.setBoolean(1, false);
                psMarkLent.setInt(2, bookId);
                psMarkLent.executeUpdate();
                throw new LibraryException.PersistenceException("Unexpected: multiple rows updated for book id=" + bookId, null);
            }
            LocalDate today = LocalDate.now();
            psInsert.setInt(1, loanId);
            psInsert.setInt(2, memberId);
            psInsert.setInt(3, bookId);
            psInsert.setDate(4, Date.valueOf(today));
            if (psInsert.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("INSERT loan returned 0 rows", null);
            }
            log.info("Loan created: id={}, memberId={}, bookId={}", loanId, memberId, bookId);
            return new Loan(loanId, memberId, bookId, today);
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("createLoan failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to create loan", e);
        }
    }

    @Override
    public int returnLoan(int loanId) {
        String findBookSql  = "SELECT " + bId + " FROM " + loanTable + " WHERE " + lId + " = ?";
        String markAvailSql = "UPDATE " + bookTable + " SET " + bLent + " = false WHERE " + bId + " = ?";
        String deleteSql    = "DELETE FROM " + loanTable + " WHERE " + lId + " = ?";
        String maxIdSql     = "SELECT " + lId + " FROM " + loanTable +
                              " WHERE " + lId + " = (SELECT MAX(" + lId + ") FROM " + loanTable + ")";
        try (Connection c = dataSource.getConnection();
             PreparedStatement psFind   = c.prepareStatement(findBookSql);
             PreparedStatement psAvail  = c.prepareStatement(markAvailSql);
             PreparedStatement psDelete = c.prepareStatement(deleteSql);
             Statement stmtMax = c.createStatement()) {
            int bookId;
            psFind.setInt(1, loanId);
            try (ResultSet rs = psFind.executeQuery()) {
                if (!rs.next()) throw new LibraryException.NotFoundException("Loan not found: id=" + loanId);
                bookId = rs.getInt(1);
            }
            psAvail.setInt(1, bookId);
            int upd = psAvail.executeUpdate();
            if (upd == 0) throw new LibraryException.PersistenceException(
                    "Could not mark book id=" + bookId + " as available", null);
            psDelete.setInt(1, loanId);
            if (psDelete.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("DELETE loan returned 0 rows", null);
            }
            log.info("Loan returned: id={}", loanId);
            try (ResultSet rs = stmtMax.executeQuery(maxIdSql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("returnLoan({}) failed: {}", loanId, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to return loan " + loanId, e);
        }
    }

    @Override
    public int[] countLoans() {
        return countTable(loanTable, lId);
    }

    // =========================================================================
    // USERS (admin — identifier-validated DDL paths)
    // =========================================================================

    @Override
    public List<User> listUsers() {
        String sql = "SELECT * FROM " + userTable;
        List<User> list = new ArrayList<>();
        try (Connection c = dataSource.getConnection();
             Statement s = c.createStatement();
             ResultSet rs = s.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new User(rs.getInt(1), rs.getString(2)));
            }
        } catch (SQLException e) {
            log.error("listUsers failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to list users", e);
        }
        return list;
    }

    @Override
    public User addUser(int id, String username, char[] password) {
        // D-4 fix: validate before any string concatenation into DDL
        String validName = IdentifierValidator.validate(username, "username");
        String setProperty  = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccess   = "'derby.database.fullAccessUsers'";
        String countSql     = "SELECT COUNT(*) FROM " + userTable;
        String allUsersSql  = "SELECT " + uName + " FROM " + userTable;
        String checkSql     = "SELECT * FROM " + userTable + " WHERE LOWER(" + uName + ") = LOWER(?)";
        String insertSql    = "INSERT INTO " + userTable + " VALUES (?,?)";

        try (Connection c = dataSource.getConnection();
             Statement stmtCount   = c.createStatement();
             Statement stmtDdl     = c.createStatement();
             Statement stmtUsers   = c.createStatement();
             PreparedStatement psCheck  = c.prepareStatement(checkSql);
             PreparedStatement psInsert = c.prepareStatement(insertSql)) {

            try (ResultSet rs = stmtCount.executeQuery(countSql)) {
                rs.next();
                if (rs.getInt(1) >= maxUsers) throw new LibraryException.BusinessRuleException(
                        "User limit reached: max=" + maxUsers);
            }

            // Block reserved names
            if (validName.equalsIgnoreCase(schemaName) || validName.equalsIgnoreCase("user")) {
                throw new LibraryException.BusinessRuleException("Reserved username: " + validName);
            }

            psCheck.setString(1, validName);
            try (ResultSet rs = psCheck.executeQuery()) {
                if (rs.next()) throw new LibraryException.DuplicateException(
                        "User already exists: " + validName);
            }

            // Derby SET PROPERTY with validated identifier (D-4 fix)
            String pwdValue = new String(password);
            stmtDdl.executeUpdate(setProperty + "'derby.user." + validName + "', '" + pwdValue + "')");

            // Build the fullAccessUsers list from the current users table
            StringBuilder listUsers = new StringBuilder(schemaName + ",");
            try (ResultSet rs = stmtUsers.executeQuery(allUsersSql)) {
                while (rs.next()) listUsers.append(rs.getString(1)).append(",");
            }
            stmtDdl.executeUpdate(setProperty + fullAccess + ", '" + listUsers + validName + "')");

            for (int i = 1; i <= 3; i++) {
                String tbl = schemaName + "." + getTableName(i);
                stmtDdl.executeUpdate("GRANT ALL PRIVILEGES ON TABLE " + tbl + " TO " + validName);
            }

            psInsert.setInt(1, id);
            psInsert.setString(2, validName);
            if (psInsert.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("INSERT user returned 0 rows", null);
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("addUser failed: {}", e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to add user", e);
        }
        log.info("User provisioned: id={}, username='{}'", id, validName);
        return new User(id, validName);
    }

    @Override
    public int deleteUser(int id) {
        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccess  = "'derby.database.fullAccessUsers'";
        String findSql     = "SELECT " + uName + " FROM " + userTable + " WHERE " + uId + " = ?";
        String allSql      = "SELECT " + uName + " FROM " + userTable;
        String deleteSql   = "DELETE FROM " + userTable + " WHERE " + uId + " = ?";
        String maxIdSql    = "SELECT " + uId + " FROM " + userTable +
                             " WHERE " + uId + " = (SELECT MAX(" + uId + ") FROM " + userTable + ")";

        try (Connection c = dataSource.getConnection();
             PreparedStatement psFind   = c.prepareStatement(findSql);
             Statement stmtDdl  = c.createStatement();
             Statement stmtAll  = c.createStatement();
             PreparedStatement psDelete = c.prepareStatement(deleteSql);
             Statement stmtMax  = c.createStatement()) {

            String storedName;
            psFind.setInt(1, id);
            try (ResultSet rs = psFind.executeQuery()) {
                if (!rs.next()) throw new LibraryException.NotFoundException("User not found: id=" + id);
                storedName = rs.getString(1);
            }
            // D-4 fix: validate the stored identifier before using it in DDL
            String validName = IdentifierValidator.validate(storedName, "stored username");

            stmtDdl.executeUpdate(setProperty + "'derby.user." + validName + "', null)");
            for (int i = 1; i <= 3; i++) {
                String tbl = schemaName + "." + getTableName(i);
                stmtDdl.executeUpdate("REVOKE ALL PRIVILEGES ON TABLE " + tbl + " FROM " + validName);
            }

            psDelete.setInt(1, id);
            if (psDelete.executeUpdate() != 1) {
                throw new LibraryException.PersistenceException("DELETE user returned 0 rows", null);
            }

            StringBuilder listUsers = new StringBuilder(schemaName + ",");
            try (ResultSet rs = stmtAll.executeQuery(allSql)) {
                while (rs.next()) listUsers.append(rs.getString(1)).append(",");
            }
            if (listUsers.charAt(listUsers.length() - 1) == ',') {
                listUsers.deleteCharAt(listUsers.length() - 1);
            }
            stmtDdl.executeUpdate(setProperty + fullAccess + ", '" + listUsers + "')");

            log.info("User deleted: id={}, username='{}'", id, validName);
            try (ResultSet rs = stmtMax.executeQuery(maxIdSql)) {
                return rs.next() ? rs.getInt(1) : 0;
            }
        } catch (LibraryException le) {
            throw le;
        } catch (SQLException e) {
            log.error("deleteUser({}) failed: {}", id, e.getMessage(), e);
            throw new LibraryException.PersistenceException("Failed to delete user " + id, e);
        }
    }

    @Override
    public int[] countUsers() {
        return countTable(userTable, uId);
    }

    // =========================================================================
    // RAG — delegate to RagService
    // =========================================================================

    @Override
    public void ingest() {
        List<Book> books = listBooks();
        ragService.ingest(books);
    }

    @Override
    public AnswerWithSources ask(String question) {
        if (question == null || question.isBlank()) {
            throw new LibraryException("Question must not be blank");
        }
        return ragService.ask(question);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    private Book mapBook(ResultSet rs) throws SQLException {
        return new Book(rs.getInt(1), rs.getString(2), rs.getString(3), rs.getBoolean(4));
    }

    private Member mapMember(ResultSet rs) throws SQLException {
        return new Member(rs.getInt(1), rs.getString(2), rs.getString(3));
    }

    private Loan mapLoan(ResultSet rs) throws SQLException {
        return new Loan(rs.getInt(1), rs.getInt(2), rs.getInt(3), rs.getDate(4).toLocalDate());
    }

    private int[] countTable(String table, String idField) {
        String q1 = "SELECT COUNT(*) FROM " + table;
        String q2 = "SELECT " + idField + " FROM " + table +
                    " WHERE " + idField + " = (SELECT MAX(" + idField + ") FROM " + table + ")";
        try (Connection c = dataSource.getConnection();
             Statement s1 = c.createStatement();
             Statement s2 = c.createStatement();
             ResultSet rs1 = s1.executeQuery(q1);
             ResultSet rs2 = s2.executeQuery(q2)) {
            rs1.next();
            int count = rs1.getInt(1);
            return rs2.next() ? new int[]{count, rs2.getInt(1)} : new int[]{count, 0};
        } catch (SQLException e) {
            log.error("countTable({}) failed: {}", table, e.getMessage(), e);
            return new int[]{0, 0};
        }
    }

    private String resolveBookSearchColumn(String field) {
        if (field == null) return bTitle;
        return switch (field.toLowerCase()) {
            case "author" -> bAuthor;
            default       -> bTitle;
        };
    }

    /** Returns the table name (without schema) for table index 1=books, 2=members, 3=loans. */
    private String getTableName(int index) {
        Properties p = utils.Utils.readProperties();
        return p.getProperty("database-table-" + index, switch (index) {
            case 1 -> "books";
            case 2 -> "members";
            case 3 -> "loans";
            default -> throw new IllegalArgumentException("Unknown table index: " + index);
        });
    }
}
