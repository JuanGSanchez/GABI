package core;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the {@link LibraryServiceImpl} exception/error paths.
 *
 * <p>Uses a Mockito-mocked {@link DataSource} to simulate {@link SQLException}s
 * and verify that each service method wraps them in the correct
 * {@link LibraryException} subtype. This covers the {@code catch(SQLException)}
 * branches in every method that would otherwise be unreachable with in-memory Derby.
 */
@ExtendWith(MockitoExtension.class)
class LibraryServiceImplExceptionTest {

    @Mock
    private DataSource ds;

    @Mock
    private Connection conn;

    @Mock
    private Statement stmt;

    @Mock
    private PreparedStatement ps;

    @Mock
    private ResultSet rs;

    private LibraryServiceImpl service;
    private StubRagService ragService;

    @BeforeEach
    void setUp() throws SQLException {
        when(ds.getConnection()).thenReturn(conn);
        ragService = new StubRagService();
        service = new LibraryServiceImpl(ds, ragService);
    }

    // =========================================================================
    // listBooks SQLException path
    // =========================================================================

    @Test
    @DisplayName("listBooks wraps SQLException in PersistenceException")
    void listBooks_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("list books fail"));
        assertThatThrownBy(() -> service.listBooks())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list books");
    }

    // =========================================================================
    // searchBooks SQLException path
    // =========================================================================

    @Test
    @DisplayName("searchBooks wraps SQLException in PersistenceException")
    void searchBooks_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("search books fail"));
        assertThatThrownBy(() -> service.searchBooks("title", "Dune"))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to search books");
    }

    // =========================================================================
    // getBook SQLException path
    // =========================================================================

    @Test
    @DisplayName("getBook wraps SQLException in PersistenceException")
    void getBook_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("getBook fail"));
        assertThatThrownBy(() -> service.getBook(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to get book");
    }

    // =========================================================================
    // addBook SQLException path
    // =========================================================================

    @Test
    @DisplayName("addBook wraps unexpected SQLException in PersistenceException")
    void addBook_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("addBook fail"));
        assertThatThrownBy(() -> service.addBook(1, "Dune", "Herbert"))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to add book");
    }

    // =========================================================================
    // deleteBook SQLException path
    // =========================================================================

    @Test
    @DisplayName("deleteBook wraps unexpected SQLException in PersistenceException")
    void deleteBook_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("deleteBook fail"));
        assertThatThrownBy(() -> service.deleteBook(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to delete book");
    }

    // =========================================================================
    // listMembers SQLException path
    // =========================================================================

    @Test
    @DisplayName("listMembers wraps SQLException in PersistenceException")
    void listMembers_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("list members fail"));
        assertThatThrownBy(() -> service.listMembers())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list members");
    }

    // =========================================================================
    // listMembersWithLoans SQLException path
    // =========================================================================

    @Test
    @DisplayName("listMembersWithLoans wraps SQLException in PersistenceException")
    void listMembersWithLoans_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("listMembersWithLoans fail"));
        assertThatThrownBy(() -> service.listMembersWithLoans())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list members with loans");
    }

    // =========================================================================
    // searchMembers SQLException path
    // =========================================================================

    @Test
    @DisplayName("searchMembers wraps SQLException in PersistenceException")
    void searchMembers_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("searchMembers fail"));
        assertThatThrownBy(() -> service.searchMembers("name", "Alice"))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to search members");
    }

    // =========================================================================
    // getMember SQLException path
    // =========================================================================

    @Test
    @DisplayName("getMember wraps SQLException in PersistenceException")
    void getMember_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("getMember fail"));
        assertThatThrownBy(() -> service.getMember(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to get member");
    }

    // =========================================================================
    // addMember SQLException path
    // =========================================================================

    @Test
    @DisplayName("addMember wraps unexpected SQLException in PersistenceException")
    void addMember_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("addMember fail"));
        assertThatThrownBy(() -> service.addMember(1, "Alice", "Smith"))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to add member");
    }

    // =========================================================================
    // deleteMember SQLException path
    // =========================================================================

    @Test
    @DisplayName("deleteMember wraps unexpected SQLException in PersistenceException")
    void deleteMember_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("deleteMember fail"));
        assertThatThrownBy(() -> service.deleteMember(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to delete member");
    }

    // =========================================================================
    // listLoans SQLException path
    // =========================================================================

    @Test
    @DisplayName("listLoans wraps SQLException in PersistenceException")
    void listLoans_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("listLoans fail"));
        assertThatThrownBy(() -> service.listLoans())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list loans");
    }

    // =========================================================================
    // listLoansWithDetails SQLException path
    // =========================================================================

    @Test
    @DisplayName("listLoansWithDetails wraps SQLException in PersistenceException")
    void listLoansWithDetails_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("listLoansWithDetails fail"));
        assertThatThrownBy(() -> service.listLoansWithDetails())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list loans with details");
    }

    // =========================================================================
    // listLoansByMember SQLException path
    // =========================================================================

    @Test
    @DisplayName("listLoansByMember wraps SQLException in PersistenceException")
    void listLoansByMember_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("listLoansByMember fail"));
        assertThatThrownBy(() -> service.listLoansByMember(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list loans by member");
    }

    // =========================================================================
    // createLoan SQLException path
    // =========================================================================

    @Test
    @DisplayName("createLoan wraps unexpected SQLException in PersistenceException")
    void createLoan_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("createLoan fail"));
        assertThatThrownBy(() -> service.createLoan(1, 1, 1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to create loan");
    }

    // =========================================================================
    // returnLoan SQLException path
    // =========================================================================

    @Test
    @DisplayName("returnLoan wraps unexpected SQLException in PersistenceException")
    void returnLoan_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("returnLoan fail"));
        assertThatThrownBy(() -> service.returnLoan(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to return loan");
    }

    // =========================================================================
    // listUsers SQLException path
    // =========================================================================

    @Test
    @DisplayName("listUsers wraps SQLException in PersistenceException")
    void listUsers_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("listUsers fail"));
        assertThatThrownBy(() -> service.listUsers())
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to list users");
    }

    // =========================================================================
    // addUser SQLException path
    // =========================================================================

    @Test
    @DisplayName("addUser wraps unexpected SQLException in PersistenceException")
    void addUser_sqlException() throws SQLException {
        when(conn.createStatement()).thenThrow(new SQLException("addUser fail"));
        assertThatThrownBy(() -> service.addUser(1, "testuser1", "pass".toCharArray()))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to add user");
    }

    // =========================================================================
    // deleteUser SQLException path
    // =========================================================================

    @Test
    @DisplayName("deleteUser wraps unexpected SQLException in PersistenceException")
    void deleteUser_sqlException() throws SQLException {
        when(conn.prepareStatement(anyString())).thenThrow(new SQLException("deleteUser fail"));
        assertThatThrownBy(() -> service.deleteUser(1))
                .isInstanceOf(LibraryException.PersistenceException.class)
                .hasMessageContaining("Failed to delete user");
    }
}
