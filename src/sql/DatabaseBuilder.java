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

// We would set the following property to TRUE only when we were ready to deploy.
// Setting it to FALSE means that we can always override using system properties
// if we accidentally paint ourselves into a corner.
            s.executeUpdate(setProperty + propertiesOnly + ", 'true')");

            System.out.println("  Montaje de la base de datos finalizado con éxito");
        } catch (SQLException sqle) {
            System.out.println("  Error montando la base de datos: " + sqle.getMessage());
        }
    }

}
