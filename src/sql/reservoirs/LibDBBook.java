/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.Book;
import tables.User;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Libros; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public final class LibDBBook implements LibDAO<Book> {
    /**
     * Instancia única de la clase
     */
    private static final LibDBBook instance = new LibDBBook();
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
     * Campo 1 de la tabla de datos;
     */
    private final String field1;
    /**
     * Campo 2 de la tabla de datos;
     */
    private final String field2;
    /**
     * Campo 3 de la tabla de datos;
     */
    private final String field3;
    /**
     * Campo 4 de la tabla de datos;
     */
    private final String field4;

    /**
     * Constructor privado de la clase
     */
    private LibDBBook() {
        configProps = new Properties();
        try (FileInputStream fis = new FileInputStream("src/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }
        url = configProps.getProperty("database-url") + "/" + configProps.getProperty("database");
        tableName = configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1");
        field1 = configProps.getProperty("database-table-1-field-1");
        field2 = configProps.getProperty("database-table-1-field-2");
        field3 = configProps.getProperty("database-table-1-field-3");
        field4 = configProps.getProperty("database-table-1-field-4");
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static LibDBBook getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Libros
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Número de filas de la tabla de datos
     */
    @Override
    public int[] countTB(User currentUser) {
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
     * Método para introducir una nueva entrada en la tabla de datos Libros
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param book        Objeto Libro que registrar en la base de datos
     */
    @Override
    public void addTB(User currentUser, Book book) {
        String query1 = String.format("SELECT * FROM %s WHERE LOWER(%s) = LOWER(?) AND LOWER(%s) = LOWER(?)",
                tableName, field2, field3);
        String query2 = String.format("INSERT INTO %s VALUES (?,?,?,?)", tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2)) {
            pStmt1.setString(1, book.getTitle());
            pStmt1.setString(2, book.getAuthor());
            ResultSet rs = pStmt1.executeQuery();
            if (rs.next()) {
                rs.close();
                throw new SQLException("Libro ya registrado");
            } else {
                rs.close();
            }

            pStmt2.setInt(1, book.getIdBook());
            pStmt2.setString(2, book.getTitle());
            pStmt2.setString(3, book.getAuthor());
            pStmt2.setBoolean(4,book.isLent());
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
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos Libro por cada entrada de la tabla de datos
     */
    @Override
    public List<Book> searchTB(User currentUser) {
        String query = String.format("SELECT * FROM %s", tableName);
        List<Book> listBook = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listBook.add(new Book(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listBook;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Libros;
     * implementación invisible al no poseer estos objetos mayores detalles
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos Libro por cada entrada de la tabla de datos
     */
    @Override
    public List<Book> searchDetailTB(User currentUser) {
        return null;
    }

    /**
     * Método para extraer entradas de la tabla de datos Libros
     * según un fragmento de texto dado para buscar en una de las
     * columnas de texto de la tabla
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param opt         Número para indicar la columna de la tabla donde buscar
     * @param seed        Fragmento de texto que buscar en las entradas de la tabla
     * @return Lista de objetos Libro que hayan salido de la búsqueda
     */
    public List<Book> searchTB(User currentUser, int opt, String seed) {
        String query = String.format("SELECT * FROM %s WHERE LOWER(%s) LIKE LOWER(?)",
                tableName, opt == 2 ? field2 : field3);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<Book> listBooks = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listBooks.add(new Book(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4)));
            }
            rs.close();
            if (listBooks.isEmpty()) {
                throw new SQLException("No se encuentran libros con los parámetros de búsqueda indicados");
            }
            return listBooks;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Libros
     * según su identificación numérica ID
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada en la tabla
     * @return Objeto Libro con los datos de la entrada encontrada
     */
    public Book searchTB(User currentUser, int ID) {
        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, field1);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                Book newBook = new Book(ID,
                        rs.getString(2),
                        rs.getString(3),
                        rs.getBoolean(4));
                rs.close();
                return newBook;
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
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada a eliminar
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(User currentUser, int ID) {
        String query1 = String.format("SELECT %s FROM %s WHERE %s = ?", field4, tableName, field1);
        String query2 = String.format("DELETE FROM %s WHERE %s = ?", tableName, field1);
        String query3 = String.format("SELECT %s FROM %s WHERE %s = (SELECT max(%s) FROM %s)",
                field1, tableName, field1, field1, tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
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
