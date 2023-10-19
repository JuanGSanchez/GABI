/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de usuarios
 */
package sql.users;

import tables.Usuario;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
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
     * URL de la base de datos utilizada por este código
     */
    private final String url;
    /**
     * Lista de propiedades del programa
     */
    private final Properties configProps;
    /**
     * Ruta completa de la tabla de datos manejada en esta clase
     */
    private final String tableName;

    /**
     * Constructor privado de la clase
     */
    private UserDerby() {
        configProps = new Properties();
        try (FileInputStream fis = new FileInputStream("src/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }
        url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");
        tableName = configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-4");
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
            System.out.println("  Identidad verificada, acceso concedido: bienvenido, " + user);
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para contar el número de usuarios registrados
     * en la base de datos
     *
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    @Override
    public int[] countUser(String user, char[] password) {
        String query1 = "SELECT COUNT(*) FROM " + tableName;
        String query2 = String.format("SELECT iduser FROM %s WHERE iduser = (SELECT max(iduser) FROM %s)", tableName, tableName);

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
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
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    @Override
    public void addUser(String user, char[] password, Usuario newUser) {
        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";
        String query1 = "SELECT COUNT(*) FROM " + tableName;
        String query2 = "SELECT nombre FROM " + tableName;
        String query3 = "SELECT * FROM " + tableName + " WHERE LOWER(nombre) = LOWER(?)";
        String query4 = "INSERT INTO " + tableName + " VALUES (?,?)";

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
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

            pStmt1.setString(1, newUser.getNombre());
            ResultSet pRs1 = pStmt1.executeQuery();
            if (pRs1.next()) {
                pRs1.close();
                throw new SQLException("Usuario ya registrado");
            } else {
                pRs1.close();
            }

            s2.executeUpdate(setProperty + "'derby.user." + newUser.getNombre() + "', '" + newUser.getPassword() + "')");
            StringBuilder listUsers = new StringBuilder("admin,");
            while (rs3.next()) {
                listUsers.append(rs3.getString(1)).append(",");
            }
            s2.executeUpdate(setProperty + fullAccessUsers + ", '" + listUsers + newUser.getNombre() + "')");

            pStmt2.setInt(1, newUser.getIdUser());
            pStmt2.setString(2, newUser.getNombre());
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
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    @Override
    public List<Usuario> searchUser(String user, char[] password) {
        String query = "SELECT * FROM " + tableName;
        List<Usuario> listUsuario = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listUsuario.add(new Usuario(rs.getInt(1),
                        rs.getString(2)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listUsuario;
    }

    public List<Usuario> searchUser(String user, char[] password, String seed) {
        String query = "SELECT * FROM " + tableName + " WHERE LOWER(nombre) LIKE LOWER(?)";

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<Usuario> listUsuarios = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listUsuarios.add(new Usuario(rs.getInt(1),
                        rs.getString(2)));
            }
            rs.close();
            if (listUsuarios.isEmpty()) {
                throw new SQLException("No se encuentran usuarios con los parámetros de búsqueda indicados");
            }
            return listUsuarios;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    public Usuario searchUser(String user, char[] password, int ID) {
        String query = "SELECT * FROM " + tableName + " WHERE iduser = ?";

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                Usuario newUsuario = new Usuario(ID,
                        rs.getString(2));
                rs.close();
                return newUsuario;
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
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    @Override
    public int deleteUser(String user, char[] password, int ID) {
        String setProperty = "CALL SYSCS_UTIL.SYSCS_SET_DATABASE_PROPERTY(";
        String fullAccessUsers = "'derby.database.fullAccessUsers'";
        String query1 = "SELECT * FROM " + tableName + " WHERE iduser = ?";
        String query2 = "SELECT nombre FROM " + tableName;
        String query3 = "DELETE FROM " + tableName + " WHERE iduser = ?";
        String query4 = String.format("SELECT iduser FROM %s WHERE iduser = (SELECT max(iduser) FROM %s)", tableName, tableName);

        try (Connection con = DriverManager.getConnection(url, user, String.valueOf(password));
             Statement s1 = con.createStatement();
             Statement s2 = con.createStatement();
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             Statement stmt4 = con.createStatement()) {
            pStmt1.setInt(1, ID);
            ResultSet rs1 = pStmt1.executeQuery();
            if (rs1.next()) {
                s1.executeUpdate(setProperty + "'derby.user." + rs1.getString(2) + "', null)");
                rs1.close();
            } else {
                rs1.close();
                throw new SQLException("No se encuentra usuario con la ID indicada");
            }

            pStmt3.setInt(1, ID);
            if (pStmt3.executeUpdate() == 1) {
                StringBuilder listUsers = new StringBuilder("admin,");
                ResultSet rs2 = s2.executeQuery(query2);
                while (rs2.next()) {
                    listUsers.append(rs2.getString(1)).append(",");
                }
                rs2.close();
                s1.executeUpdate(setProperty + fullAccessUsers + ", '" + listUsers.deleteCharAt(listUsers.length() - 1) + "')");
                System.out.println("Usuario eliminado con éxito.");
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
