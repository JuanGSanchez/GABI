package sql;

import core.IdentifierValidator;

import java.sql.*;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Código base para definir permisos y usuarios de la base de datos,
 * basado en código extraído de Stack Overflow.
 *
 * <p>Workstream (b) — D-4 fix: all schema identifiers (schema name, table names)
 * that are concatenated into DDL statements are now validated by
 * {@link IdentifierValidator} before use.  This prevents SQL identifier injection
 * via a malicious {@code database-name} or {@code database-table-*} property value.
 */
public class DatabaseBuilder {

    /**
     * Constructor privado de la clase
     * para recopilar las propiedades del programa
     */
    private DatabaseBuilder() {
    }

    /**
     * Método de ejecuciones para la creación
     * de la base de datos
     */
    public static void sqlExecuter(Properties configProps, ResourceBundle rb) {

        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String getProperty = "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(";

        String requireAuth = "'derby.connection.requireAuthentication'";

        String sqlAuthorization = "'derby.database.sqlAuthorization'";
        String defaultConnMode = "'derby.database.defaultConnectionMode'";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";
//        String readOnlyAccessUsers = "'derby.database.readOnlyAccessUsers'";

        String provider = "'derby.authentication.provider'";
        String propertiesOnly = "'derby.database.propertiesOnly'";

        String url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");

        // D-4 fix: validate all identifiers that will be concatenated into DDL.
        // IdentifierValidator.validate() throws InvalidIdentifierException on bad input.
        String dbName  = IdentifierValidator.validate(configProps.getProperty("database-name"), "database-name");
        String table1  = IdentifierValidator.validate(configProps.getProperty("database-table-1"), "database-table-1");
        String table2  = IdentifierValidator.validate(configProps.getProperty("database-table-2"), "database-table-2");
        String table3  = IdentifierValidator.validate(configProps.getProperty("database-table-3"), "database-table-3");
        String table4  = IdentifierValidator.validate(configProps.getProperty("database-table-4"), "database-table-4");
        String f1_1    = IdentifierValidator.validate(configProps.getProperty("database-table-1-field-1"), "t1-f1");
        String f1_2    = IdentifierValidator.validate(configProps.getProperty("database-table-1-field-2"), "t1-f2");
        String f1_3    = IdentifierValidator.validate(configProps.getProperty("database-table-1-field-3"), "t1-f3");
        String f1_4    = IdentifierValidator.validate(configProps.getProperty("database-table-1-field-4"), "t1-f4");
        String f2_1    = IdentifierValidator.validate(configProps.getProperty("database-table-2-field-1"), "t2-f1");
        String f2_2    = IdentifierValidator.validate(configProps.getProperty("database-table-2-field-2"), "t2-f2");
        String f2_3    = IdentifierValidator.validate(configProps.getProperty("database-table-2-field-3"), "t2-f3");
        String f3_1    = IdentifierValidator.validate(configProps.getProperty("database-table-3-field-1"), "t3-f1");
        String f3_4    = IdentifierValidator.validate(configProps.getProperty("database-table-3-field-4"), "t3-f4");
        String f4_1    = IdentifierValidator.validate(configProps.getProperty("database-table-4-field-1"), "t4-f1");
        String f4_2    = IdentifierValidator.validate(configProps.getProperty("database-table-4-field-2"), "t4-f2");

        try (Connection conn = DriverManager.getConnection(url + ";user=" + dbName +
                                                           ";password=" + configProps.getProperty("database-password") +
                                                           ";create=true");
             Statement s = conn.createStatement()) {
            System.out.printf("%s '%s'...\n", rb.getString("builder-setup"), configProps.getProperty("database"));

// Stablish admin user
            s.executeUpdate(setProperty + "'derby.user." + dbName +
                            "', '" + configProps.getProperty("database-password") + "')");

// Define read-write user
            s.executeUpdate(setProperty + fullAccessUsers + ", '" + dbName + "')");

// Set requireAuthentication
            s.executeUpdate(setProperty + requireAuth + ", 'true')");

// Set sqlAuthorization
            s.executeUpdate(setProperty + sqlAuthorization + ", 'true')");

// Set authentication scheme to Derby builtin
            s.executeUpdate(setProperty + provider + ", 'BUILTIN')");

// Define noAccess as default connection mode
            s.executeUpdate(setProperty + defaultConnMode + ", 'noAccess')");

// Confirm full-access users
            ResultSet rs = s.executeQuery(getProperty + fullAccessUsers + ")");
            rs.next();
            System.out.printf("  %s %s\n", rb.getString("builder-admin"), rs.getString(1));
            rs.close();

            System.out.printf("  %s...\n", rb.getString("builder-building"));
//            Delete tables and schema if they already exists
            // All identifiers below are the pre-validated variables (D-4 fix)
            try {
                s.executeUpdate(String.format("DROP TABLE %s.%s", dbName, table3));
                s.executeUpdate(String.format("DROP TABLE %s.%s", dbName, table1));
                s.executeUpdate(String.format("DROP TABLE %s.%s", dbName, table2));
                s.executeUpdate(String.format("DROP TABLE %s.%s", dbName, table4));
                s.executeUpdate(String.format("DROP SCHEMA %s RESTRICT", dbName));
                System.out.printf("  %s\n", rb.getString("builder-cleanup"));
            } catch (SQLException sqle) {
                System.out.printf("  %s\n", rb.getString("builder-empty"));
            }
//            Create schema and tables from scratch
            s.executeUpdate(String.format("CREATE SCHEMA %s", dbName));

            s.executeUpdate(String.format("CREATE TABLE %s.%s(%s INTEGER NOT NULL,%s VARCHAR(%s),PRIMARY KEY (%s))",
                    dbName, table4, f4_1, f4_2,
                    configProps.getProperty("database-table-4-field-2-maxchar"), f4_1));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s VARCHAR(%s),%s VARCHAR(%s),%s BOOLEAN,PRIMARY KEY (%s))",
                    dbName, table1, f1_1,
                    f1_2, configProps.getProperty("database-table-1-field-2-maxchar"),
                    f1_3, configProps.getProperty("database-table-1-field-3-maxchar"),
                    f1_4, f1_1));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s VARCHAR(%s),%s VARCHAR(%s),PRIMARY KEY (%s))",
                    dbName, table2, f2_1,
                    f2_2, configProps.getProperty("database-table-2-field-2-maxchar"),
                    f2_3, configProps.getProperty("database-table-2-field-3-maxchar"),
                    f2_1));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s INTEGER NOT NULL,%s INTEGER NOT NULL,%s DATE," +
                                          "PRIMARY KEY (%s),FOREIGN KEY (%s) REFERENCES %s(%s),FOREIGN KEY (%s) REFERENCES %s(%s))",
                    dbName, table3,
                    f3_1, f2_1, f1_1, f3_4,
                    f3_1,
                    f2_1, table2, f2_1,
                    f1_1, table1, f1_1));

// We would set the following property to TRUE only when we were ready to deploy.
// Setting it to FALSE means that we can always override using system properties
// if we accidentally paint ourselves into a corner.
            s.executeUpdate(setProperty + propertiesOnly + ", 'true')");

            System.out.printf("  %s.\n", rb.getString("builder-finish"));
        } catch (SQLException sqle) {
            System.err.printf("  %s: %s", rb.getString("builder-error"), sqle.getMessage());
        }
    }

}
