package core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Loan;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-19 acceptance over in-memory Derby: {@code listOverdueLoans()} returns loans past
 * their due date, and {@code dueDate}/{@code loanPeriodDays} reflect the configured policy.
 */
class LibraryServiceImplOverdueTest {

    private DataSource ds;
    private LibraryServiceImpl service;

    @BeforeEach
    void setUp() throws SQLException {
        ds = new InMemoryDerbyConfig().testDataSource();
        TestSchemaHelper.createSchema(ds);
        service = new LibraryServiceImpl(ds, new StubRagService());
    }

    private void seedLoan(int loanId, int memberId, int bookId, LocalDate date) throws SQLException {
        try (Connection c = ds.getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO admin.members(idMember,name,surname) VALUES (?,?,?)")) {
                ps.setInt(1, memberId); ps.setString(2, "M" + memberId); ps.setString(3, "S");
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO admin.books(idBook,title,author,lent) VALUES (?,?,?,?)")) {
                ps.setInt(1, bookId); ps.setString(2, "T" + bookId); ps.setString(3, "A");
                ps.setBoolean(4, true);
                ps.executeUpdate();
            }
            try (PreparedStatement ps = c.prepareStatement(
                    "INSERT INTO admin.loans(idLoan,idMember,idBook,dateLoan) VALUES (?,?,?,?)")) {
                ps.setInt(1, loanId); ps.setInt(2, memberId); ps.setInt(3, bookId);
                ps.setDate(4, Date.valueOf(date));
                ps.executeUpdate();
            }
        }
    }

    @Test
    @DisplayName("default loan period is three weeks and due date derives from it")
    void periodAndDueDate() throws SQLException {
        LocalDate loanDate = LocalDate.now().minusDays(5);
        seedLoan(1, 1, 1, loanDate);
        assertThat(service.loanPeriodDays()).isEqualTo(LoanPolicy.DEFAULT_PERIOD_DAYS);
        Loan loan = service.listLoans().get(0);
        assertThat(service.dueDate(loan)).isEqualTo(loanDate.plusDays(LoanPolicy.DEFAULT_PERIOD_DAYS));
    }

    @Test
    @DisplayName("listOverdueLoans returns only loans past their due date")
    void overdueFiltering() throws SQLException {
        seedLoan(1, 1, 1, LocalDate.now().minusDays(60)); // overdue (60 > 21)
        seedLoan(2, 2, 2, LocalDate.now());               // fresh — not overdue

        List<Loan> overdue = service.listOverdueLoans();
        assertThat(overdue).hasSize(1);
        assertThat(overdue.get(0).getID()).isEqualTo(1);
        // resolved details are present (member + book joined)
        assertThat(overdue.get(0).getMember()).isNotNull();
        assertThat(overdue.get(0).getBook()).isNotNull();
    }

    @Test
    @DisplayName("no overdue loans yields an empty list, never null")
    void noOverdue() throws SQLException {
        seedLoan(1, 1, 1, LocalDate.now());
        assertThat(service.listOverdueLoans()).isEmpty();
    }
}
