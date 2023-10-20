package sql;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.Properties;

/**
 * Código base para definir permisos y usuarios de la base de datos,
 * basado en código extraído de Stack Overflow
 */
public class UserAuthentication {
    public static void main(String[] args) {
        String url;

        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String getProperty = "VALUES SYSCS_UTIL.SYSCS_GET_DATABASE_PROPERTY(";
        String requireAuth = "'derby.connection.requireAuthentication'";

        String sqlAuthorization = "'derby.database.sqlAuthorization'";
        String defaultConnMode = "'derby.database.defaultConnectionMode'";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";

        String readOnlyAccessUsers = "'derby.database.readOnlyAccessUsers'";
        String provider = "'derby.authentication.provider'";
        String propertiesOnly = "'derby.database.propertiesOnly'";

        Properties configProps = new Properties();
        try (FileInputStream fis = new FileInputStream("src/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }
        url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");

        try (Connection conn = DriverManager.getConnection(url + ";user=" + configProps.getProperty("database-name") +
                                                           ";password=" + configProps.getProperty("database-password") +
                                                           ";create=true");
             Statement s = conn.createStatement()) {
            System.out.println("Turning on authentication and SQL authorization.");

// Set requireAuthentication
            s.executeUpdate(setProperty + requireAuth + ", 'true')");

//CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.connection.requireAuthentication','true'
// Set sqlAuthorization
            s.executeUpdate(setProperty + sqlAuthorization + ", 'true')");

//CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY('derby.database.sqlAuthentication','true'
// Retrieve and display property values
            ResultSet rs = s.executeQuery(getProperty + requireAuth + ")");
            rs.next();
            System.out.println("Value of requireAuthentication is " + rs.getString(1));

            rs = s.executeQuery(getProperty + sqlAuthorization + ")");
            rs.next();
            System.out.println("Value of sqlAuthorization is " + rs.getString(1));

// Set authentication scheme to Derby builtin
            s.executeUpdate(setProperty + provider + ", 'BUILTIN')");

// Create some sample users
            s.executeUpdate(setProperty + "'derby.user." + configProps.getProperty("database-name") +
                            "', '" + configProps.getProperty("database-password") + "')");

// Define noAccess as default connection mode
            s.executeUpdate(setProperty + defaultConnMode + ", 'noAccess')");

// Confirm default connection mode
            rs = s.executeQuery(getProperty + defaultConnMode + ")");
            rs.next();
            System.out.println("Value of defaultConnectionMode is " + rs.getString(1));

// Define read-write users
            s.executeUpdate(setProperty + fullAccessUsers + ", '" + configProps.getProperty("database-name") + "')");

// Define read-only user
//            s.executeUpdate(setProperty + readOnlyAccessUsers + ", 'guest')");

// Confirm full-access users
            rs = s.executeQuery(getProperty + fullAccessUsers + ")");
            rs.next();
            System.out.println("Value of fullAccessUsers is " + rs.getString(1));

// Confirm read-only users
            rs = s.executeQuery(getProperty + readOnlyAccessUsers + ")");
            rs.next();
            System.out.println("Value of readOnlyAccessUsers is " + rs.getString(1));

// We would set the following property to TRUE only when we were
// ready to deploy. Setting it to FALSE means that we can always
// override using system properties if we accidentally paint
// ourselves into a corner.
            s.executeUpdate("CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(" + propertiesOnly + ", 'false')");
        } catch (SQLException sqle) {
            System.out.println(sqle.getMessage());
        }
    }
}
