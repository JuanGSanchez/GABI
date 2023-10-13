/**
 * Paquete de comandos de conexión con la base de datos
 */
package sql;

import tables.Libro;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Libros; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public final class BiblioDBLibro implements BiblioDAO<Libro> {
    /**
     * Instancia única de la clase
     */
    private static final BiblioDBLibro instance = new BiblioDBLibro();
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
    private BiblioDBLibro() {
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static BiblioDBLibro getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Libros
     *
     * @return Número de filas de la tabla de datos
     */
    @Override
    public int[] countTB() {
        String query1 = "SELECT COUNT(*) FROM libros";
        String query2 = "SELECT idlib FROM libros WHERE idlib = (SELECT max(idlib) FROM libros)";

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
     * Método para introducir una nueva entrada en la tabla de datos Libros
     *
     * @param libro Objeto Libro que registrar en la base de datos
     */
    @Override
    public void addTB(Libro libro) {
        String query1 = "SELECT * FROM libros WHERE LOWER(titulo) = LOWER(?) AND LOWER(autor) = LOWER(?)";
        String query2 = "INSERT INTO libros VALUES (?,?,?,FALSE)";

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2)) {
            pStmt1.setString(1, libro.getTitulo());
            pStmt1.setString(2, libro.getAutor());
            ResultSet rs = pStmt1.executeQuery();
            if (rs.next()) {
                rs.close();
                throw new SQLException("Libro ya registrado");
            } else {
                rs.close();
            }

            pStmt2.setInt(1, libro.getIdLib());
            pStmt2.setString(2, libro.getTitulo());
            pStmt2.setString(3, libro.getAutor());
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("  nuevo libro agregado con éxito.");
            } else throw new SQLException("Ha habido un problema inesperado\nal intentar agregar el libro");
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Libros
     *
     * @return Lista de objetos Libro por cada entrada de la tabla de datos
     */
    @Override
    public List<Libro> searchTB() {
        String query = "SELECT * FROM libros";
        List<Libro> listLibro = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url);
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listLibro.add(new Libro(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listLibro;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Libros;
     * implementación invisible al no poseer estos objetos mayores detalles
     *
     * @return Lista de objetos Libro por cada entrada de la tabla de datos
     */
    @Override
    public List<Libro> searchDetailTB() {
        return null;
    }

    /**
     * Método para extraer entradas de la tabla de datos Libros
     * según un fragmento de texto dado para buscar en una de las
     * columnas de texto de la tabla
     *
     * @param opt  Número para indicar la columna de la tabla donde buscar
     * @param seed Fragmento de texto que buscar en las entradas de la tabla
     * @return Lista de objetos Libro que hayan salido de la búsqueda
     */
    public List<Libro> searchTB(int opt, String seed) {
        String query = "SELECT * FROM libros WHERE LOWER(" + (opt == 2 ? "titulo" : "autor") + ") LIKE LOWER(?)";

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<Libro> listLibros = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listLibros.add(new Libro(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4)));
            }
            rs.close();
            if (listLibros.isEmpty()) {
                throw new SQLException("No se encuentran libros con los parámetros de búsqueda indicados");
            }
            return listLibros;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Libros
     * según su identificación numérica ID
     *
     * @param ID Identificación numérica de la entrada en la tabla
     * @return Objeto Libro con los datos de la entrada encontrada
     */
    public Libro searchTB(int ID) {
        String query = "SELECT * FROM libros WHERE idlib = ?";

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                Libro newLibro = new Libro(ID,
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4));
                rs.close();
                return newLibro;
            } else {
                rs.close();
                throw new SQLException("No se encuentra libro con la ID indicada");
            }
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para eliminar una entrada de la tabla de datos Libros
     * según su identificación numérica ID
     *
     * @param ID Identificación numérica de la entrada a eliminar
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(int ID) {
        String query1 = "SELECT prestado FROM libros WHERE idlib = ?";
        String query2 = "DELETE FROM libros WHERE idlib = ?";
        String query3 = "SELECT idlib FROM libros WHERE idlib = (SELECT max(idlib) FROM libros)";

        try (Connection con = DriverManager.getConnection(url);
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             Statement stmt3 = con.createStatement()) {
            pStmt1.setInt(1, ID);
            ResultSet rs = pStmt1.executeQuery();
            if (rs.next()) {
                if (rs.getBoolean(1)) {
                    rs.close();
                    throw new SQLException("El libro se encuentra prestado aún");
                }
                rs.close();
            } else {
                rs.close();
                throw new SQLException("No se encuentra libro con la ID indicada");
            }

            pStmt2.setInt(1, ID);
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("Entrada de libro eliminada con éxito.");
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
