package sql.users;

import core.LibraryException;
import org.junit.jupiter.api.Test;
import tables.User;

import java.util.ResourceBundle;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * SPEC-R03: the legacy CLI privileged-admin sink {@code UserDerby} must route every
 * identifier reaching a DDL/admin statement through {@code core.IdentifierValidator},
 * fail-fast, before any SQL is issued.
 *
 * <p>These negative tests assert that a malicious Derby username is rejected with a typed
 * {@link LibraryException.InvalidIdentifierException} <em>before</em> a connection is opened
 * — so a crafted name can never reach {@code SET PROPERTY 'derby.user.<name>'} or a
 * {@code GRANT}/{@code REVOKE} statement. (No live Derby is required: validation precedes
 * {@code DriverManager.getConnection}.)
 */
class UserDerbyIdentifierTest {

    private final ResourceBundle rb = ResourceBundle.getBundle("statements");

    @Test
    void addDb_rejectsSqlInjectionInUsername_beforeAnySql() {
        User admin = new User(0, "admin", "pw");
        User malicious = new User(99, "x'; DROP TABLE users--", "pw");

        assertThatThrownBy(() -> UserDerby.getInstance().addDb(admin, malicious, rb))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);
    }

    @Test
    void addDb_rejectsUsernameWithQuoteOrSpace() {
        User admin = new User(0, "admin", "pw");

        assertThatThrownBy(() -> UserDerby.getInstance()
                .addDb(admin, new User(1, "bad name", "pw"), rb))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);

        assertThatThrownBy(() -> UserDerby.getInstance()
                .addDb(admin, new User(2, "rob'ert", "pw"), rb))
                .isInstanceOf(LibraryException.InvalidIdentifierException.class);
    }
}
