package core;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Utility that creates (or recreates) the GABI library schema in the
 * in-memory Derby test database.
 *
 * <p>Schema mirrors configuration.properties defaults:
 * schema = admin, tables = books / members / loans / users.
 *
 * <p>Call {@link #createSchema(DataSource)} in a {@code @BeforeEach} or
 * {@code @BeforeAll} to start each test with a clean, empty database.
 *
 * <p><b>SQL Authorization note:</b> Derby's GRANT/REVOKE requires the
 * {@code derby.database.sqlAuthorization} property set to TRUE, which only takes
 * effect after a database reboot.  {@link #enableSqlAuthorizationAndReboot(String)}
 * must be called once (per unique in-memory DB name) to activate GRANT support.
 */
public final class TestSchemaHelper {

    public static final String SCHEMA = "admin";

    private TestSchemaHelper() {}

    /**
     * Enables Derby SQL authorization on the in-memory database and reboots it
     * so that the change takes effect for subsequent connections.
     *
     * <p>This is a one-time setup per unique DB name. Must be called before
     * {@link #createSchema(DataSource)}.
     *
     * @param dbName the in-memory DB name (e.g. "gabiTest")
     */
    public static void enableSqlAuthorizationAndReboot(String dbName) {
        String createUrl  = "jdbc:derby:memory:" + dbName + ";create=true";
        String shutdownUrl = "jdbc:derby:memory:" + dbName + ";shutdown=true";

        // Step 1: create the DB if needed and set sqlAuthorization
        try (Connection c = DriverManager.getConnection(createUrl, "", "");
             Statement s = c.createStatement()) {
            s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" +
                            "'derby.database.sqlAuthorization', 'TRUE')");
        } catch (SQLException e) {
            if (!e.getSQLState().equals("XJ015")) { // XJ015 = expected shutdown success
                // ignore "already exists" — property may already be set
            }
        }

        // Step 2: shutdown the in-memory DB so the property takes effect on next connect
        try {
            DriverManager.getConnection(shutdownUrl, "", "");
        } catch (SQLException e) {
            // Derby always throws on shutdown — expected (SQLState "08006" for db shutdown,
            // "XJ015" for system shutdown). Anything else might be a real error but we
            // continue because the DB may not have been booted yet.
        }
        // After shutdown, next getConnection with "create=true" recreates it with auth enabled.
    }

    /**
     * Drops all existing tables + schema and recreates them from scratch.
     * Safe to call multiple times — DROP errors are swallowed.
     *
     * <p>Assumes the DataSource points at {@code jdbc:derby:memory:&lt;name&gt;;create=true}.
     * If GRANT support is needed (for addUser tests), call
     * {@link #enableSqlAuthorizationAndReboot(String)} once before creating the DataSource.
     */
    public static void createSchema(DataSource ds) throws SQLException {
        try (Connection c = ds.getConnection();
             Statement s = c.createStatement()) {

            // Drop in FK-safe order (loans -> books/members -> users -> schema)
            dropSilently(s, "DROP TABLE admin.loans");
            dropSilently(s, "DROP TABLE admin.books");
            dropSilently(s, "DROP TABLE admin.members");
            dropSilently(s, "DROP TABLE admin.users");
            dropSilently(s, "DROP SCHEMA admin RESTRICT");

            s.executeUpdate("CREATE SCHEMA admin");

            // users table
            s.executeUpdate(
                "CREATE TABLE admin.users (" +
                "  idUser   INTEGER NOT NULL, " +
                "  name     VARCHAR(20), " +
                "  PRIMARY KEY (idUser)" +
                ")"
            );

            // books table
            s.executeUpdate(
                "CREATE TABLE admin.books (" +
                "  idBook  INTEGER NOT NULL, " +
                "  title   VARCHAR(120), " +
                "  author  VARCHAR(60), " +
                "  lent    BOOLEAN, " +
                "  PRIMARY KEY (idBook)" +
                ")"
            );

            // members table
            s.executeUpdate(
                "CREATE TABLE admin.members (" +
                "  idMember INTEGER NOT NULL, " +
                "  name     VARCHAR(20), " +
                "  surname  VARCHAR(40), " +
                "  PRIMARY KEY (idMember)" +
                ")"
            );

            // loans table — FK to members and books
            s.executeUpdate(
                "CREATE TABLE admin.loans (" +
                "  idLoan   INTEGER NOT NULL, " +
                "  idMember INTEGER NOT NULL, " +
                "  idBook   INTEGER NOT NULL, " +
                "  dateLoan DATE, " +
                "  PRIMARY KEY (idLoan), " +
                "  FOREIGN KEY (idMember) REFERENCES admin.members(idMember), " +
                "  FOREIGN KEY (idBook)   REFERENCES admin.books(idBook)" +
                ")"
            );
        }
    }

    private static void dropSilently(Statement s, String sql) {
        try {
            s.executeUpdate(sql);
        } catch (SQLException ignored) {
            // table/schema may not exist on the first call — that is fine
        }
    }
}
