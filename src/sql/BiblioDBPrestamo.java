/**
 * Paquete de comandos de conexión con la base de datos
 */
package sql;

import tables.Prestamo;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

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
    private final String url = "jdbc:derby://localhost:1527/biblioteca;create=true";
    /**
     * Nombre de usuario para el acceso a la base de datos
     */
    private final String user = "root";
    /**
     * Contraseña de cuenta para el acceso a la base de datos
     */
    private final String password = "root";

    /**
     * Constructor privado de la clase
     */
    private BiblioDBPrestamo() {
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
    public int[] countTB() {
        String query1 = "SELECT COUNT(*) FROM prestamos";
        String query2 = "SELECT idpres FROM prestamos WHERE idpres = (SELECT max(idpres) FROM prestamos)";

        try (Connection con = DriverManager.getConnection(url);
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
    public void addTB(Prestamo prestamo) {
        String query1 = "SELECT COUNT(*) FROM prestamos WHERE idsoc = ?";
        String query2 = "UPDATE libros SET prestado = ? WHERE idlib = ?";
        String query3 = "INSERT INTO prestamos VALUES (?,?,?,?)";

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3)) {
            pStmt1.setInt(1, prestamo.getIdSoc());
            ResultSet rs = pStmt1.executeQuery();
            rs.next();
            if (rs.getInt(1) == 10) {
                rs.close();
                throw new SQLException("Límite de préstamos alcanzado por el socio");
            }
            rs.close();

            pStmt2.setBoolean(1, true);
            pStmt2.setInt(2, prestamo.getIdLib());
            int updates = pStmt2.executeUpdate();
            if (updates == 0) {
                throw new SQLException("Libro ya prestado o inexistente");
            } else if (updates > 1) {
                pStmt2.setBoolean(1, false);
                pStmt2.executeUpdate();
                throw new SQLException("Error inesperado en la localización del libro");
            }

            pStmt3.setInt(1, prestamo.getIdPres());
            pStmt3.setInt(2, prestamo.getIdSoc());
            pStmt3.setInt(3, prestamo.getIdLib());
            pStmt3.setDate(4, Date.valueOf(prestamo.getFechaPres()));
            if (pStmt3.executeUpdate() == 1) {
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
    public List<Prestamo> searchTB() {
        String query = "SELECT * FROM prestamos";
        List<Prestamo> listPrestamo = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url);
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
     * Método para extraer entradas de la tabla de datos Préstamos
     * según un ID concreto a buscar entre columnas de la tabla
     *
     * @param opt Número para indicar la columna de la tabla donde buscar
     * @param ID  ID que buscar en las entradas de la tabla
     * @return Objeto Préstamo con la entrada que haya salido de la búsqueda
     */
    public List<Prestamo> searchTB(int opt, int ID) {
        String query = "SELECT * FROM prestamos WHERE " + (opt == 1 ? "idpres" : opt == 2 ? "idsoc" : "idlib") + " = ?";

        try (Connection con = DriverManager.getConnection(url);
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
    public List<Prestamo> searchTB(LocalDate date) {
        String query = "SELECT * FROM prestamos WHERE fechapres = ?";

        try (Connection con = DriverManager.getConnection(url);
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
    public int deleteTB(int ID) {
        String query1 = "SELECT idlib FROM prestamos WHERE idpres = ?";
        String query2 = "UPDATE libros SET prestado = ? WHERE idlib = ?";
        String query3 = "DELETE FROM prestamos WHERE idpres = ?";
        String query4 = "SELECT idpres FROM prestamos WHERE idpres = (SELECT max(idpres) FROM prestamos)";

        try (Connection con = DriverManager.getConnection(url);
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
