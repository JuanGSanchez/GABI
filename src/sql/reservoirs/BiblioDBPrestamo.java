/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.Libro;
import tables.Prestamo;
import tables.Socio;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Préstamos; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public class BiblioDBPrestamo implements BiblioDAO<Prestamo> {
    /**
     * Instancia única de la clase
     */
    private static final BiblioDBPrestamo instance = new BiblioDBPrestamo();
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
    private BiblioDBPrestamo() {
        configProps = new Properties();
        try (FileInputStream fis = new FileInputStream("src/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }
        url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");
        tableName = configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-3");
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static BiblioDBPrestamo getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Préstamos
     *
     * @return Número de filas de la tabla de datos
     */
    @Override
    public int[] countTB(String user, String password) {
        String query1 = "SELECT COUNT(*) FROM " + tableName;
        String query2 = String.format("SELECT idpres FROM %s WHERE idpres = (SELECT max(idpres) FROM %s)", tableName, tableName);

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
     * Método para introducir una nueva entrada en la tabla de datos Préstamos
     *
     * @param prestamo Objeto Préstamo que registrar en la base de datos
     */
    @Override
    public void addTB(String user, String password, Prestamo prestamo) {
        String query1 = "SELECT COUNT(*) FROM " + tableName + " WHERE idsoc = ?";
        String query2 = "SELECT prestado FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1") + " WHERE idlib = ?";
        String query3 = "UPDATE " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1") + " SET prestado = ? WHERE idlib = ?";
        String query4 = "INSERT INTO " + tableName + " VALUES (?,?,?,?)";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             PreparedStatement pStmt4 = con.prepareStatement(query4)) {
            pStmt1.setInt(1, prestamo.getIdSoc());
            ResultSet rs = pStmt1.executeQuery();
            rs.next();
            if (rs.getInt(1) >= 10) {
                rs.close();
                throw new SQLException("Límite de préstamos alcanzado por el socio");
            }
            rs.close();

            pStmt2.setInt(1, prestamo.getIdLib());
            ResultSet rs2 = pStmt2.executeQuery();
            if (rs2.next()) {
                if (rs2.getBoolean(1)) {
                    rs2.close();
                    throw new SQLException("Libro ya prestado");
                }
            } else {
                rs2.close();
                throw new SQLException("Libro inexistente");
            }
            rs2.close();

            pStmt3.setBoolean(1, true);
            pStmt3.setInt(2, prestamo.getIdLib());
            if (pStmt3.executeUpdate() > 1) {
                pStmt3.setBoolean(1, false);
                pStmt3.executeUpdate();
                throw new SQLException("Error inesperado en la localización del libro");
            }

            pStmt4.setInt(1, prestamo.getIdPres());
            pStmt4.setInt(2, prestamo.getIdSoc());
            pStmt4.setInt(3, prestamo.getIdLib());
            pStmt4.setDate(4, Date.valueOf(prestamo.getFechaPres()));
            if (pStmt4.executeUpdate() == 1) {
                System.out.println("  nuevo préstamo registrado con éxito.");
            } else throw new SQLException("Ha habido un problema inesperado\nal intentar registrar el préstamo");

        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Préstamos
     *
     * @return Lista de objetos Préstamo por cada entrada de la tabla de datos
     */
    @Override
    public List<Prestamo> searchTB(String user, String password) {
        String query = "SELECT * FROM " + tableName;
        List<Prestamo> listPrestamo = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listPrestamo.add(new Prestamo(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listPrestamo;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Préstamos,
     * junto a detalles de libro y socio asociados
     *
     * @return Lista de objetos Préstamo por cada entrada de la tabla de datos
     */
    @Override
    public List<Prestamo> searchDetailTB(String user, String password) {
        String query1 = "SELECT * FROM " + tableName;
        String query2 = "SELECT * FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-2") + " WHERE idsoc = ?";
        String query3 = "SELECT * FROM " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1") + " WHERE idlib = ?";
        List<Prestamo> listPrestamo = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, user, password);
             Statement stmt1 = con.createStatement();
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             ResultSet rs1 = stmt1.executeQuery(query1)) {
            ResultSet rs2 = pStmt2.getResultSet();
            ResultSet rs3 = pStmt3.getResultSet();
            while (rs1.next()) {
                pStmt2.setInt(1, rs1.getInt(2));
                pStmt3.setInt(1, rs1.getInt(3));
                rs2 = pStmt2.executeQuery();
                rs3 = pStmt3.executeQuery();
                rs2.next();
                rs3.next();
                listPrestamo.add(new Prestamo(rs1.getInt(1),
                        rs1.getInt(2),
                        rs1.getInt(3),
                        rs1.getDate(4).toLocalDate(),
                        new Socio(rs2.getInt(1), rs2.getString(2), rs2.getString(3)),
                        new Libro(rs3.getInt(1), rs3.getString(2), rs3.getString(3), rs3.getBoolean(4))));
            }
            rs2.close();
            rs3.close();
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listPrestamo;
    }

    /**
     * Método para extraer entradas de la tabla de datos Préstamos
     * según un ID concreto a buscar entre columnas de la tabla
     *
     * @param opt Número para indicar la columna de la tabla donde buscar
     * @param ID  ID que buscar en las entradas de la tabla
     * @return Objeto Préstamo con la entrada que haya salido de la búsqueda
     */
    public List<Prestamo> searchTB(String user, String password, int opt, int ID) {
        String query = "SELECT * FROM " + tableName + " WHERE " + (opt == 1 ? "idpres" : opt == 2 ? "idsoc" : "idlib") + " = ?";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            List<Prestamo> listPrestamos = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listPrestamos.add(new Prestamo(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
            rs.close();
            if (listPrestamos.isEmpty()) {
                throw new SQLException("No se encuentran préstamos con los parámetros de búsqueda indicados");
            }
            return listPrestamos;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Préstamos
     * según fecha de realización
     *
     * @param date Fecha de realización del préstamo
     * @return Lista de objetos Préstamo de las entradas resultantes de la búsqueda
     */
    public List<Prestamo> searchTB(String user, String password, LocalDate date) {
        String query = "SELECT * FROM " + tableName + " WHERE fechapres = ?";

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setDate(1, Date.valueOf(date));
            List<Prestamo> listPrestamos = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listPrestamos.add(new Prestamo(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
            rs.close();
            if (listPrestamos.isEmpty()) {
                throw new SQLException("No se encuentran préstamos con los parámetros de búsqueda indicados");
            }
            return listPrestamos;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para eliminar una entrada de la tabla de datos Préstamos
     * según su identificación numérica ID
     *
     * @param ID Identificación numérica de la entrada a eliminar
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(String user, String password, int ID) {
        String query1 = "SELECT idlib FROM " + tableName + " WHERE idpres = ?";
        String query2 = "UPDATE " + configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1") + " SET prestado = ? WHERE idlib = ?";
        String query3 = "DELETE FROM " + tableName + " WHERE idpres = ?";
        String query4 = String.format("SELECT idpres FROM %s WHERE idpres = (SELECT max(idpres) FROM %s)", tableName, tableName);

        try (Connection con = DriverManager.getConnection(url, user, password);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             Statement stmt4 = con.createStatement()) {
            pStmt1.setInt(1, ID);
            ResultSet rs = pStmt1.executeQuery();
            rs.next();
            int idlib = rs.getInt(1);
            if (rs.next()) {
                rs.close();
                throw new SQLException("Error inesperado en la localización del préstamo");
            }
            rs.close();

            pStmt2.setBoolean(1, false);
            pStmt2.setInt(2, idlib);
            int updates = pStmt2.executeUpdate();
            if (updates == 0) {
                throw new SQLException("Libro ya disponible o inexistente");
            } else if (updates > 1) {
                pStmt2.setBoolean(1, true);
                pStmt2.executeUpdate();
                throw new SQLException("Error inesperado en la localización del libro");
            }

            pStmt3.setInt(1, ID);
            if (pStmt3.executeUpdate() == 1) {
                System.out.println("Devolución de préstamo completada con éxito.");
                ResultSet rs4 = stmt4.executeQuery(query4);
                if (rs4.next()) {
                    int maxIDLib = rs4.getInt(1);
                    rs4.close();
                    return maxIDLib;
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
