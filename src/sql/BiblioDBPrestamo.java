/**
 * Paquete de comandos de conexión con la base de datos
 */
package sql;

import tables.Libro;
import tables.Prestamo;
import tables.Socio;

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
        String query2 = "SELECT prestado FROM libros WHERE idlib = ?";
        String query3 = "UPDATE libros SET prestado = ? WHERE idlib = ?";
        String query4 = "INSERT INTO prestamos VALUES (?,?,?,?)";

        try (Connection con = DriverManager.getConnection(url);
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
     * Método para extraer todas las entradas de la tabla de datos Préstamos,
     * junto a detalles de libro y socio asociados
     *
     * @return Lista de objetos Préstamo por cada entrada de la tabla de datos
     */
    @Override
    public List<Prestamo> searchDetailTB() {
        String query1 = "SELECT * FROM prestamos";
        String query2 = "SELECT * FROM socios WHERE idsoc = ?";
        String query3 = "SELECT * FROM libros WHERE idlib = ?";
        List<Prestamo> listPrestamo = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url);
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
