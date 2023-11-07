/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;
import utils.Utils;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.ResourceBundle;

/**
 * Clase principal de métodos de conexión a la base de datos
 * con Derby para la tabla de Préstamos; uso del patrón singleton
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public class LibDBLoan implements LibDAO<Loan> {
    /**
     * Instancia única de la clase
     */
    private static final LibDBLoan instance = new LibDBLoan();
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
                                     configProps.getProperty("database-table-3");
    /**
     * Campo 1 de la tabla de datos;
     */
    private final String field1 = configProps.getProperty("database-table-3-field-1");
    /**
     * Campo 2 de la tabla de datos;
     */
    private final String field2 = configProps.getProperty("database-table-2-field-1");
    /**
     * Campo 3 de la tabla de datos;
     */
    private final String field3 = configProps.getProperty("database-table-1-field-1");
    /**
     * Campo 3 de la tabla de datos;
     */
    private final String field4 = configProps.getProperty("database-table-3-field-4");

    /**
     * Constructor privado de la clase
     */
    private LibDBLoan() {
    }

    /**
     * Método del patrón singleton para obtener la instancia única de clase
     *
     * @return Instancia de clase
     */
    public static LibDBLoan getInstance() {
        return instance;
    }

    /**
     * Método para contabilizar las entradas de la tabla de datos Préstamos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Número de filas de la tabla de datos
     */
    @Override
    public int[] countTB(User currentUser, ResourceBundle rb) {
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
            System.err.printf("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage());
            return null;
        }
    }

    /**
     * Método para introducir una nueva entrada en la tabla de datos Préstamos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param loan        Objeto Préstamo que registrar en la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    @Override
    public void addTB(User currentUser, Loan loan, ResourceBundle rb) {
        String query1 = String.format("SELECT COUNT(*) FROM %s WHERE %s = ?", tableName, field2);
        String query2 = String.format("SELECT %s FROM %s WHERE %s = ?",
                configProps.getProperty("database-table-1-field-4"),
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1"), field3);
        String query3 = String.format("UPDATE %s SET %s = ? WHERE %s = ?",
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1"),
                configProps.getProperty("database-table-1-field-4"), field3);
        String query4 = String.format("INSERT INTO %s VALUES (?,?,?,?)", tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt1 = con.prepareStatement(query1);
             PreparedStatement pStmt2 = con.prepareStatement(query2);
             PreparedStatement pStmt3 = con.prepareStatement(query3);
             PreparedStatement pStmt4 = con.prepareStatement(query4)) {
            pStmt1.setInt(1, loan.getIdMember());
            ResultSet rs = pStmt1.executeQuery();
            rs.next();
            if (rs.getInt(1) >= Integer.parseInt(configProps.getProperty("database-table-2-maxloan"))) {
                rs.close();
                throw new SQLException(rb.getString("dao-loan-error-limit"));
            }
            rs.close();

            pStmt2.setInt(1, loan.getIdBook());
            ResultSet rs2 = pStmt2.executeQuery();
            if (rs2.next()) {
                if (rs2.getBoolean(1)) {
                    rs2.close();
                    throw new SQLException(rb.getString("dao-loan-error-lent"));
                }
            } else {
                rs2.close();
                throw new SQLException(rb.getString(rb.getString("dao-loan-error-exist")));
            }
            rs2.close();

            pStmt3.setBoolean(1, true);
            pStmt3.setInt(2, loan.getIdBook());
            if (pStmt3.executeUpdate() > 1) {
                pStmt3.setBoolean(1, false);
                pStmt3.executeUpdate();
                throw new SQLException(rb.getString("dao-loan-error-location-book"));
            }

            pStmt4.setInt(1, loan.getID());
            pStmt4.setInt(2, loan.getIdMember());
            pStmt4.setInt(3, loan.getIdBook());
            pStmt4.setDate(4, Date.valueOf(loan.getDateLoan()));
            if (pStmt4.executeUpdate() == 1) {
                System.out.printf("  %s.\n", rb.getString("dao-loan-add"));
            } else throw new SQLException(rb.getString("dao-loan-error-add"));

        } catch (SQLException sqle) {
            throw new RuntimeException(sqle.getMessage());
        }
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Préstamos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Lista de objetos Préstamo por cada entrada de la tabla de datos
     */
    @Override
    public List<Loan> searchTB(User currentUser, ResourceBundle rb) {
        String query = String.format("SELECT * FROM %s", tableName);
        List<Loan> listLoan = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                listLoan.add(new Loan(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
        } catch (SQLException sqle) {
            System.err.printf("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage());
        }

        return listLoan;
    }

    /**
     * Método para extraer todas las entradas de la tabla de datos Préstamos,
     * junto a detalles de libro y socio asociados
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Lista de objetos Préstamo por cada entrada de la tabla de datos
     */
    @Override
    public List<Loan> searchDetailTB(User currentUser, ResourceBundle rb) {
        String query1 = String.format("SELECT * FROM %s", tableName);
        String query2 = String.format("SELECT * FROM %s WHERE %s = ?",
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-2"), field2);
        String query3 = String.format("SELECT * FROM %s WHERE %s = ?",
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1"), field3);
        List<Loan> listLoan = new ArrayList<>();

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
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
                listLoan.add(new Loan(rs1.getInt(1),
                        rs1.getInt(2),
                        rs1.getInt(3),
                        rs1.getDate(4).toLocalDate(),
                        new Member(rs2.getInt(1), rs2.getString(2), rs2.getString(3)),
                        new Book(rs3.getInt(1), rs3.getString(2), rs3.getString(3), rs3.getBoolean(4))));
            }
            rs2.close();
            rs3.close();
        } catch (SQLException sqle) {
            System.err.printf("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage());
        }

        return listLoan;
    }

    /**
     * Método para extraer entradas de la tabla de datos Préstamos
     * según un ID concreto a buscar entre columnas de la tabla
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param opt         Número para indicar la columna de la tabla donde buscar
     * @param ID          ID que buscar en las entradas de la tabla
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Objeto Préstamo con la entrada que haya salido de la búsqueda
     */
    public List<Loan> searchTB(User currentUser, int opt, int ID, ResourceBundle rb) {
        String query = String.format("SELECT * FROM %s WHERE %s = ?",
                tableName, opt == 1 ? field1 : opt == 2 ? field2 : field3);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setInt(1, ID);
            List<Loan> listLoans = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listLoans.add(new Loan(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
            rs.close();
            if (listLoans.isEmpty()) {
                throw new SQLException(rb.getString("dao-loan-error-search"));
            }
            return listLoans;
        } catch (SQLException sqle) {
            throw new RuntimeException(String.format("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage()));
        }
    }

    /**
     * Método para extraer entradas de la tabla de datos Préstamos
     * según fecha de realización
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param date        Fecha de realización del préstamo
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Lista de objetos Préstamo de las entradas resultantes de la búsqueda
     */
    public List<Loan> searchTB(User currentUser, LocalDate date, ResourceBundle rb) {
        String query = String.format("SELECT * FROM %s WHERE %s = ?", tableName, field4);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
             PreparedStatement pStmt = con.prepareStatement(query)) {
            pStmt.setDate(1, Date.valueOf(date));
            List<Loan> listLoans = new ArrayList<>();
            ResultSet rs = pStmt.executeQuery();
            while (rs.next()) {
                listLoans.add(new Loan(rs.getInt(1),
                        rs.getInt(2),
                        rs.getInt(3),
                        rs.getDate(4).toLocalDate()));
            }
            rs.close();
            if (listLoans.isEmpty()) {
                throw new SQLException(rb.getString("dao-loan-error-search"));
            }
            return listLoans;
        } catch (SQLException sqle) {
            throw new RuntimeException(String.format("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage()));
        }
    }

    /**
     * Método para eliminar una entrada de la tabla de datos Préstamos
     * según su identificación numérica ID
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada a eliminar
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return ID máxima tras la eliminación de la entrada
     */
    @Override
    public int deleteTB(User currentUser, int ID, ResourceBundle rb) {
        String query1 = String.format("SELECT %s FROM %s WHERE %s = ?",
                field3, tableName, field1);
        String query2 = String.format("UPDATE %s SET %s = ? WHERE %s = ?",
                configProps.getProperty("database-name") + "." + configProps.getProperty("database-table-1"),
                configProps.getProperty("database-table-1-field-4"), field3);
        String query3 = String.format("DELETE FROM %s WHERE %s = ?", tableName, field1);
        String query4 = String.format("SELECT %s FROM %s WHERE %s = (SELECT max(%s) FROM %s)",
                field1, tableName, field1, field1, tableName);

        try (Connection con = DriverManager.getConnection(url, currentUser.getName(), currentUser.getPassword());
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
                throw new SQLException(rb.getString("dao-loan-error-location-loan"));
            }
            rs.close();

            pStmt2.setBoolean(1, false);
            pStmt2.setInt(2, idlib);
            int updates = pStmt2.executeUpdate();
            if (updates == 0) {
                throw new SQLException(rb.getString("dao-loan-error-avail"));
            } else if (updates > 1) {
                pStmt2.setBoolean(1, true);
                pStmt2.executeUpdate();
                throw new SQLException(rb.getString("dao-loan-error-location-book"));
            }

            pStmt3.setInt(1, ID);
            if (pStmt3.executeUpdate() == 1) {
                System.out.printf("  %s.\n", rb.getString("dao-loan-delete"));
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
            throw new RuntimeException(String.format("  %s\n%s\n", rb.getString("dao-general-error"), sqle.getMessage()));
        }
    }
}
