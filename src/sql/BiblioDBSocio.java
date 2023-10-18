/**
 * Paquete de comandos de conexión con la base de datos
 */
package sql;

import tables.Libro;
import tables.Socio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Socios; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public final class BiblioDBSocio implements BiblioDAO<Socio> {
    /**
     * Instancia única de la clase
     */
    private static final BiblioDBSocio instance = new BiblioDBSocio();
    /**
     * URL de la base de datos utilizada por este código
     */
    private final String url;
    /**
     * Nombre de usuario para el acceso a la base de datos
     */
    private final String user = "admin";
    /**
     * Contraseña de cuenta para el acceso a la base de datos
     */
    private final String password = "1234";
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
    private BiblioDBSocio() {
        configProps = new Properties();
        try (FileInputStream fis = new FileInputStream("src/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }
        url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");
        tableName = configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-2");
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static BiblioDBSocio getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Socios
     *
     * @return Número de filas de la tabla de datos
     */
    @Override
    public int[] countTB() {
        String query1 = "SELECT COUNT(*) FROM " + tableName;
        String query2 = String.format("SELECT idsoc FROM %s WHERE idsoc = (SELECT max(idsoc) FROM %s)", tableName, tableName);

        try (Connection con = DriverManager.getConnection(url, user, password);
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
     * Método para introducir una nueva entrada en la tabla de datos Socios
     *
     * @param socio Objeto Socio que registrar en la base de datos
     */
    @Override
    public void addTB(Socio socio) {
        String query1 = "SELECT * FROM " + tableName + " WHERE LOWER(nombre) = LOWER(?) AND LOWER(apellidos) = LOWER(?)";
        String query2 = "INSERT INTO " + tableName + " VALUES (?,?,?)";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2)) {
            pStmt1.setString(1, socio.getNombre());
            pStmt1.setString(2, socio.getApellidos());
            ResultSet rs = pStmt1.executeQuery();
            if (rs.next()) {
                rs.close();
                throw new SQLException("Socio ya registrado");
            } else {
                rs.close();
            }

            pStmt2.setInt(1, socio.getIdSoc());
            pStmt2.setString(2, socio.getNombre());
            pStmt2.setString(3, socio.getApellidos());
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("  nuevo socio agregado con éxito.");
            } else throw new SQLException("Ha habido un problema inesperado\nal intentar agregar el socio");
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Socios
     *
     * @return Lista de objetos Socio por cada entrada de la tabla de datos
     */
    @Override
    public List<Socio> searchTB() {
        String query = "SELECT * FROM " + tableName;
        List<Socio> listSocio = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listSocio.add(new Socio(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listSocio;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Socios,
     * junto a detalles de sus préstamos
     *
     * @return Lista de objetos Socio por cada entrada de la tabla de datos
     */
    @Override
    public List<Socio> searchDetailTB() {
        String query1 = "SELECT * FROM " + tableName;
        String query2 = "SELECT idlib FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-3") + " WHERE idsoc = ?";
        String query3 = "SELECT * FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1") + " WHERE idlib = ?";
        List<Socio> listSocio = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement stmt1 = con.createStatement();
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             ResultSet rs1 = stmt1.executeQuery(query1)) {
            List<Libro> libroList;
            ResultSet rs2 = pStmt2.getResultSet();
            ResultSet rs3;
            while (rs1.next()) {
                libroList = new ArrayList<>();
                pStmt2.setInt(1, rs1.getInt(1));
                rs2 = pStmt2.executeQuery();
                while (rs2.next()) {
                    pStmt3.setInt(1,rs2.getInt(1));
                    rs3 = pStmt3.executeQuery();
                    rs3.next();
                    libroList.add(new Libro(rs3.getInt(1),
                            rs3.getString(2),
                            rs3.getString(3),
                            rs3.getBoolean(4)));
                    rs3.close();
                }
                listSocio.add(new Socio(rs1.getInt(1),
                        rs1.getString(2),
                        rs1.getString(3),
                        libroList));
            }
            rs2.close();
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listSocio;
    }

    /**
     * Método para extraer entradas de la tabla de datos Socios
     * según un fragmento de texto dado para buscar en una de las
     * columnas de texto de la tabla
     *
     * @param opt  Número para indicar la columna de la tabla donde buscar
     * @param seed Fragmento de texto que buscar en las entradas de la tabla
     * @return Lista de objetos Socio que hayan salido de la búsqueda
     */
    public List<Socio> searchTB(int opt, String seed) {
        String query = "SELECT * FROM " + tableName + " WHERE LOWER(" + (opt == 2 ? "nombre" : "apellidos") + ") LIKE LOWER(?)";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<Socio> listSocio = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listSocio.add(new Socio(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)));
            }
            rs.close();
            if (listSocio.isEmpty()) {
                throw new SQLException("No se encuentran socios con los parámetros de búsqueda indicados");
            }
            return listSocio;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Socios
     * según su identificación numérica ID
     *
     * @param ID Identificación numérica de la entrada en la tabla
     * @return Objeto Socio con los datos de la entrada encontrada
     */
    public Socio searchTB(int ID) {
        String query = "SELECT * FROM " + tableName + " WHERE idsoc = ?";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                Socio newSocio = new Socio(ID, rs.getString(2), rs.getString(3));
                rs.close();
                return newSocio;
            } else {
                rs.close();
                throw new SQLException("No se encuentra socio con la ID indicada");
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para eliminar una entrada de la tabla de datos Socios
     * según su identificación numérica ID
     *
     * @param ID Identificación numérica de la entrada a eliminar
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(int ID) {
        String query1 = "SELECT * FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-3") + " WHERE idsoc = ?";
        String query2 = "DELETE FROM " + tableName + " WHERE idsoc = ?";
        String query3 = String.format("SELECT idsoc FROM %s WHERE idsoc = (SELECT max(idsoc) FROM %s)", tableName, tableName);

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             Statement stmt3 = con.createStatement()) {
            pStmt1.setInt(1, ID);
            ResultSet rs = pStmt1.executeQuery();
            if (rs.next()) {
                throw new SQLException("El socio aún tiene préstamos pendientes por devolver");
            }
            rs.close();

            pStmt2.setInt(1, ID);
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("Entrada de socio eliminada con éxito.");
                ResultSet rs3 = stmt3.executeQuery(query3);
                if (rs3.next()) {
                    int maxIDLib = rs3.getInt(1);
                    rs3.close();
                    return maxIDLib;
                } else {
                    rs3.close();
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
