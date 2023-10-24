/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.Book;
import tables.Member;
import tables.User;
import utils.Utils;

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
public final class LibDBMember implements LibDAO<Member> {
    /**
     * Instancia única de la clase
     */
    private static final LibDBMember instance = new LibDBMember();
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
                                     configProps.getProperty("database-table-2");
    /**
     * Campo 1 de la tabla de datos;
     */
    private final String field1 = configProps.getProperty("database-table-2-field-1");
    /**
     * Campo 2 de la tabla de datos;
     */
    private final String field2 = configProps.getProperty("database-table-2-field-2");
    /**
     * Campo 3 de la tabla de datos;
     */
    private final String field3 = configProps.getProperty("database-table-2-field-3");

    /**
     * Constructor privado de la clase
     */
    private LibDBMember() {
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static LibDBMember getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Socios
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
     * Método para introducir una nueva entrada en la tabla de datos Socios
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param member      Objeto Socio que registrar en la base de datos
     */
    @Override
    public void addTB(User currentUser, Member member) {
        String query1 = String.format("SELECT COUNT(*) FROM %s", tableName);
        String query2 = String.format("SELECT * FROM %s WHERE LOWER(%s) = LOWER(?) AND LOWER(%s) = LOWER(?)",
                tableName, field2, field3);
        String query3 = String.format("INSERT INTO %s VALUES (?,?,?)", tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement s1 = con.createStatement();
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             ResultSet rs1 = s1.executeQuery(query1)) {
            rs1.next();
            if (rs1.getInt(1) >= Integer.parseInt(configProps.getProperty("database-table-2-maxsocs"))) {
                throw new SQLException("Límite de socios alcanzado");
            }

            pStmt2.setString(1, member.getName());
            pStmt2.setString(2, member.getSurname());
            ResultSet rs2 = pStmt2.executeQuery();
            if (rs2.next()) {
                rs2.close();
                throw new SQLException("Socio ya registrado");
            } else {
                rs2.close();
            }

            pStmt3.setInt(1, member.getIdMember());
            pStmt3.setString(2, member.getName());
            pStmt3.setString(3, member.getSurname());
            if (pStmt3.executeUpdate() == 1) {
                System.out.println("  nuevo socio agregado con éxito.");
            } else throw new SQLException("Ha habido un problema inesperado\nal intentar agregar el socio");
        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Socios
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos Socio por cada entrada de la tabla de datos
     */
    @Override
    public List<Member> searchTB(User currentUser) {
        String query = String.format("SELECT * FROM %s", tableName);
        List<Member> listMember = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listMember.add(new Member(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)));
            }
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listMember;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Socios,
     * junto a detalles de sus préstamos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos Socio por cada entrada de la tabla de datos
     */
    @Override
    public List<Member> searchDetailTB(User currentUser) {
        String query1 = String.format("SELECT * FROM %s", tableName);
        String query2 = String.format("SELECT %s FROM %s WHERE %s = ?", configProps.getProperty("database-table-1-field-1"),
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-3"), field1);
        String query3 = String.format("SELECT * FROM %s WHERE %s = ?",
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1"),
                configProps.getProperty("database-table-1-field-1"));
        List<Member> listMember = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt1 = con.createStatement();
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             ResultSet rs1 = stmt1.executeQuery(query1)) {
            List<Book> bookList;
            ResultSet rs2 = pStmt2.getResultSet();
            ResultSet rs3;
            while (rs1.next()) {
                bookList = new ArrayList<>();
                pStmt2.setInt(1, rs1.getInt(1));
                rs2 = pStmt2.executeQuery();
                while (rs2.next()) {
                    pStmt3.setInt(1, rs2.getInt(1));
                    rs3 = pStmt3.executeQuery();
                    rs3.next();
                    bookList.add(new Book(rs3.getInt(1),
                            rs3.getString(2),
                            rs3.getString(3),
                            rs3.getBoolean(4)));
                    rs3.close();
                }
                listMember.add(new Member(rs1.getInt(1),
                        rs1.getString(2),
                        rs1.getString(3),
                        bookList));
            }
            rs2.close();
        } catch (SQLException sqle) {
            System.err.println("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }

        return listMember;
    }

    /**
     * Método para extraer entradas de la tabla de datos Socios
     * según un fragmento de texto dado para buscar en una de las
     * columnas de texto de la tabla
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param opt         Número para indicar la columna de la tabla donde buscar
     * @param seed        Fragmento de texto que buscar en las entradas de la tabla
     * @return Lista de objetos Socio que hayan salido de la búsqueda
     */
    public List<Member> searchTB(User currentUser, int opt, String seed) {
        String query = String.format("SELECT * FROM %s WHERE LOWER(%s) LIKE LOWER(?)",
                tableName, opt == 2 ? field2 : field3);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setString(1, "%" + seed + "%");
            List<Member> listMember = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listMember.add(new Member(rs.getInt(1),
                        rs.getString(2),
                        rs.getString(3)));
            }
            rs.close();
            if (listMember.isEmpty()) {
                throw new SQLException("No se encuentran socios con los parámetros de búsqueda indicados");
            }
            return listMember;
        } catch (SQLException sqle) {
            throw new RuntimeException("  Error inesperado durante el contacto con la base de datos\n" + sqle.getMessage());
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Socios
     * según su identificación numérica ID
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada en la tabla
     * @return Objeto Socio con los datos de la entrada encontrada
     */
    public Member searchTB(User currentUser, int ID) {
        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, field1);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            ResultSet rs = pStmt.executeQuery();
            if (rs.next()) {
                Member newMember = new Member(ID, rs.getString(2), rs.getString(3));
                rs.close();
                return newMember;
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
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada a eliminar
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(User currentUser, int ID) {
        String query1 = String.format("SELECT %s FROM %s WHERE %s = ?",
                field1, configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-3"), field1);
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
                throw new SQLException("El socio aún tiene préstamos pendientes por devolver");
            }
            rs.close();

            pStmt2.setInt(1, ID);
            if (pStmt2.executeUpdate() == 1) {
                System.out.println("  entrada de socio eliminada con éxito.");
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
