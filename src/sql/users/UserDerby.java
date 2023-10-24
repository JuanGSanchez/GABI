/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de usuarios
 */
package sql.users;

import tables.User;
import utils.Utils;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Usuarios; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
public final class UserDerby implements UserDAO {
    /**
     * Instancia única de la clase
     */
    private static final UserDerby instance = new UserDerby();
    /**
     * Lista de propiedades del programa
     */
    private final Properties configProps = Utils.readProperties();
    /**
     * URL de la base de datos utilizada por este código
     */
    private final String url = configProps.getProperty("database-url") + "/" +
                               configProps.getProperty("database");
    /**
     * Ruta completa de la tabla de datos manejada en esta clase
     */
    private final String tableName = configProps.getProperty("database-name") + "." +
                                     configProps.getProperty("database-table-4");
    /**
     * Campo 1 de la tabla de datos;
     */
    private final String field1 = configProps.getProperty("database-table-4-field-1");
    /**
     * Campo 2 de la tabla de datos;
     */
    private final String field2 = configProps.getProperty("database-table-4-field-2");

    /**
     * Constructor privado de la clase
     */
    private UserDerby() {
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static UserDerby getInstance() {
        return instance;
    }

    /**
     * Método para probar la conexión de un usuario y contraseña
     * suministrados, comprobando su validez de conexión
     *
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    @Override
    public void tryUser(String user, char[] password) {
        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password))) {
            con.isReadOnly();
            System.out.println("  Identidad verificada, acceso concedido: bienvenido, " + user);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para contar el número de usuarios registrados
     * en la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    @Override
    public int[] countUser(User currentUser) {
        String query1 = String.format("SELECT COUNT(*) FROM %s", tableName);
        String query2 = String.format("SELECT %s FROM %s WHERE %s = (SELECT max(%s) FROM %s)",
                field1, tableName, field1, field1, tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt1 = con.createStatement();
             Statement stmt2 = con.createStatement();
             ResultSet rs1 = stmt1.executeQuery(query1);
             ResultSet rs2 = stmt2.executeQuery(query2)) {
            rs1.next();
            if (rs2.next()) {
                return new int[]{rs1.getInt(1), rs2.getInt(1)};
            } else {
                return new int[]{rs1.getInt(1), 0};
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
            return null;
        }
    }

    /**
     * Método para añadir un nuevo usuario a la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    @Override
    public void addUser(User currentUser, User newUser) {
        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";
        String query1 = String.format("SELECT COUNT(*) FROM %s", tableName);
        String query2 = String.format("SELECT %s FROM %s", field2, tableName);
        String query3 = String.format("SELECT * FROM %s WHERE LOWER(%s) = LOWER(?)", tableName, field2);
        String query4 = String.format("INSERT INTO %s VALUES (?,?)", tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement s1 = con.createStatement();
             Statement s2 = con.createStatement();
             Statement s3 = con.createStatement();
             PreparedStatement pStmt1 = con.prepareStatement(query3);
             PreparedStatement pStmt2 = con.prepareStatement(query4);
             ResultSet rs3 = s3.executeQuery(query2)) {
            ResultSet rs1 = s1.executeQuery(query1);
            rs1.next();
            if (rs1.getInt(1) >= Integer.parseInt(configProps.getProperty("database-user-maxusers"))) {
                rs1.close();
                throw new SQLException("Límite de usuarios registrados ya alcanzado");
            } else {
                rs1.close();
            }

            if (newUser.getName().equals(configProps.getProperty("database-name")) || newUser.getName().equals("user")) {
                throw new SQLException("Nombre de usuario no válido");
            }

            pStmt1.setString(1, newUser.getName());
            ResultSet pRs1 = pStmt1.executeQuery();
            if (pRs1.next()) {
                pRs1.close();
                throw new SQLException("Usuario ya registrado");
            } else {
                pRs1.close();
            }

            s2.executeUpdate(setProperty + "'derby.user." + newUser.getName() + "', '" + newUser.getPassword() + "')");
            StringBuilder listUsers = new StringBuilder(configProps.getProperty("database-name") + ",");
            while (rs3.next()) {
                listUsers.append(rs3.getString(1)).append(",");
            }
            s2.executeUpdate(setProperty + fullAccessUsers + ", '" + listUsers + newUser.getName() + "')");
            for (int i = 1; i < 4; i++) {
                s2.executeUpdate("GRANT ALL PRIVILEGES ON TABLE " + configProps.getProperty("database-name") +
                                 "." + configProps.getProperty("database-table-" + i) + " TO " + newUser.getName());
            }
            pStmt2.setInt(1, newUser.getIdUser());
            pStmt2.setString(2, newUser.getName());
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("  nuevo usuario agregado con éxito.");
            } else throw new SQLException("Ha habido un problema inesperado\nal intentar agregar el libro");
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para buscar usuarios de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    @Override
    public List<User> searchUser(User currentUser) {
        String query = String.format("SELECT * FROM %s", tableName);
        List<User> listUser = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listUser.add(new User(rs.getInt(1),
                        rs.getString(2)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listUser;
    }

    /**
     * Método para buscar usuarios de la base de datos
     * según su nombre o un fragmento de éste
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param seed        Fragmento de texto a buscar entre los nombres
     */
    public List<User> searchUser(User currentUser, String seed) {
        String query = String.format("SELECT * FROM %s WHERE LOWER(%s) LIKE LOWER(?)", tableName, field2);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<User> listUsers = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listUsers.add(new User(rs.getInt(1),
                        rs.getString(2)));
            }
            rs.close();
            if (listUsers.isEmpty()) {
                throw new SQLException("No se encuentran usuarios con los parámetros de búsqueda indicados");
            }
            return listUsers;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para buscar usuarios de la base de datos
     * según su identificación única o ID
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica del usuario
     */
    public User searchUser(User currentUser, int ID) {
        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, field1);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                User newUser = new User(ID,
                        rs.getString(2));
                rs.close();
                return newUser;
            } else {
                rs.close();
                throw new SQLException("No se encuentra usuario con la ID indicada");
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para eliminar un usuario de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    @Override
    public int deleteUser(User currentUser, int ID) {
        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";
        String query1 = String.format("SELECT %s FROM %s WHERE %s = ?", field2, tableName, field1);
        String query2 = String.format("SELECT %s FROM %s", field2, tableName);
        String query3 = String.format("DELETE FROM %s WHERE %s = ?", tableName, field1);
        String query4 = String.format("SELECT %s FROM %s WHERE %s = (SELECT max(%s) FROM %s)",
                field1, tableName, field1, field1, tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement s1 = con.createStatement();
             Statement s2 = con.createStatement();
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             Statement stmt4 = con.createStatement()) {
            pStmt1.setInt(1, ID);
            ResultSet rs1 = pStmt1.executeQuery();
            if (rs1.next()) {
                s1.executeUpdate(setProperty + "'derby.user." + rs1.getString(1) + "', null)");
                for (int i = 1; i < 4; i++) {
                    s2.executeUpdate("REVOKE ALL PRIVILEGES ON TABLE " + configProps.getProperty("database-name") +
                                     "." + configProps.getProperty("database-table-" + i) + " FROM " + rs1.getString(1));
                }
                rs1.close();
            } else {
                rs1.close();
                throw new SQLException("No se encuentra usuario con la ID indicada");
            }

            pStmt3.setInt(1, ID);
            if (pStmt3.executeUpdate() == 1) {
                StringBuilder listUsers = new StringBuilder(configProps.getProperty("database-name") + ",");
                ResultSet rs2 = s2.executeQuery(query2);
                while (rs2.next()) {
                    listUsers.append(rs2.getString(1)).append(",");
                }
                rs2.close();
                s1.executeUpdate(setProperty + fullAccessUsers + ", '" + listUsers.deleteCharAt(listUsers.length() - 1) + "')");
                System.out.println("  usuario eliminado con éxito.");
                ResultSet rs4 = stmt4.executeQuery(query4);
                if (rs4.next()) {
                    int maxIDUser = rs4.getInt(1);
                    rs4.close();
                    return maxIDUser;
                } else {
                    rs4.close();
                    return 0;
                }
            } else {
                throw new SQLException();
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

    }
}
