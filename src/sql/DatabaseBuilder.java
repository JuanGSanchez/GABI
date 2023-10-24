package sql;

import java.sql.*;
import java.util.Properties;

/**
 * Código base para definir permisos y usuarios de la base de datos,
 * basado en código extraído de Stack Overflow
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
    public static void sqlExecuter(Properties configProps) {

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

        try (Connection conn = DriverManager.getConnection(url + ";user=" + configProps.getProperty("database-name") +
                                                           ";password=" + configProps.getProperty("database-password") +
                                                           ";create=true");
             Statement s = conn.createStatement()) {
            System.out.println("Montando la base de datos '" + configProps.getProperty("database") + "'...");

// Stablish admin user
            s.executeUpdate(setProperty + "'derby.user." + configProps.getProperty("database-name") +
                            "', '" + configProps.getProperty("database-password") + "')");

// Define read-write user
            s.executeUpdate(setProperty + fullAccessUsers + ", '" + configProps.getProperty("database-name") + "')");

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
            System.out.println("  El administrador de la base de datos es " + rs.getString(1));
            rs.close();

            System.out.println("  (re)construyendo tablas...");
//            Delete tables and schema if they already exists
            try {
                s.executeUpdate(String.format("DROP TABLE %s.%s", configProps.getProperty("database-name"), configProps.getProperty("database-table-3")));
                s.executeUpdate(String.format("DROP TABLE %s.%s", configProps.getProperty("database-name"), configProps.getProperty("database-table-1")));
                s.executeUpdate(String.format("DROP TABLE %s.%s", configProps.getProperty("database-name"), configProps.getProperty("database-table-2")));
                s.executeUpdate(String.format("DROP TABLE %s.%s", configProps.getProperty("database-name"), configProps.getProperty("database-table-4")));
                s.executeUpdate(String.format("DROP SCHEMA %s RESTRICT", configProps.getProperty("database-name")));
                System.out.println("  limpieza de la base de datos completada");
            } catch (SQLException sqle) {
                System.out.println("  base de datos ya vacía");
            }
//            Create schema and tables from scratch
            s.executeUpdate(String.format("CREATE SCHEMA %s", configProps.getProperty("database-name")));

            s.executeUpdate(String.format("CREATE TABLE %s.%s(%s INTEGER NOT NULL,%s VARCHAR(%s),PRIMARY KEY (%s))",
                    configProps.getProperty("database-name"), configProps.getProperty("database-table-4"),
                    configProps.getProperty("database-table-4-field-1"), configProps.getProperty("database-table-4-field-2"),
                    configProps.getProperty("database-table-4-field-2-maxchar"), configProps.getProperty("database-table-4-field-1")));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s VARCHAR(%s),%s VARCHAR(%s),%s BOOLEAN,PRIMARY KEY (%s))",
                    configProps.getProperty("database-name"), configProps.getProperty("database-table-1"),
                    configProps.getProperty("database-table-1-field-1"),
                    configProps.getProperty("database-table-1-field-2"), configProps.getProperty("database-table-1-field-2-maxchar"),
                    configProps.getProperty("database-table-1-field-3"), configProps.getProperty("database-table-1-field-3-maxchar"),
                    configProps.getProperty("database-table-1-field-4"), configProps.getProperty("database-table-1-field-1")));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s VARCHAR(%s),%s VARCHAR(%s),PRIMARY KEY (%s))",
                    configProps.getProperty("database-name"), configProps.getProperty("database-table-2"),
                    configProps.getProperty("database-table-2-field-1"),
                    configProps.getProperty("database-table-2-field-2"), configProps.getProperty("database-table-2-field-2-maxchar"),
                    configProps.getProperty("database-table-2-field-3"), configProps.getProperty("database-table-2-field-3-maxchar"),
                    configProps.getProperty("database-table-2-field-1")));

            s.executeUpdate(String.format("CREATE TABLE %s.%s (%s INTEGER NOT NULL,%s INTEGER NOT NULL,%s INTEGER NOT NULL,%s DATE," +
                                          "PRIMARY KEY (%s),FOREIGN KEY (%s) REFERENCES %s(%s),FOREIGN KEY (%s) REFERENCES %s(%s))",
                    configProps.getProperty("database-name"), configProps.getProperty("database-table-3"),
                    configProps.getProperty("database-table-3-field-1"), configProps.getProperty("database-table-2-field-1"),
                    configProps.getProperty("database-table-1-field-1"), configProps.getProperty("database-table-3-field-4"),
                    configProps.getProperty("database-table-3-field-1"),
                    configProps.getProperty("database-table-2-field-1"), configProps.getProperty("database-table-2"), configProps.getProperty("database-table-2-field-1"),
                    configProps.getProperty("database-table-1-field-1"), configProps.getProperty("database-table-1"), configProps.getProperty("database-table-1-field-1")));

// We would set the following property to TRUE only when we were ready to deploy.
// Setting it to FALSE means that we can always override using system properties
// if we accidentally paint ourselves into a corner.
            s.executeUpdate(setProperty + propertiesOnly + ", 'true')");

            System.out.println("  Montaje de la base de datos finalizado con éxito");
        } catch (SQLException sqle) {
            System.err.println("  Error montando la base de datos: " + sqle.getMessage());
        }
    }

}
