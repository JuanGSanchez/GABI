/**
 * Paquete de menús del programa
 */
package manager;

import sql.DatabaseBuilder;
import sql.reservoirs.LibDBBook;
import sql.reservoirs.LibDBLoan;
import sql.reservoirs.LibDBMember;
import sql.users.UserDerby;
import tables.User;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.ResourceBundle;
import java.util.Scanner;

import static utils.Utils.*;

/**
 * Clase del menú principal del programa de gestión Biblioteca
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public final class LibMenu {
    /**
     * Variable de entrada de datos por teclado
     */
    private static final Scanner scanMenu = new Scanner(System.in);
    /**
     * Lista de propiedades comunes del programa
     */
    private static final Properties configProps = readProperties();
    /**
     * Recurso para la localización del texto del programa
     */
    private static final ResourceBundle rb = readLanguage(configProps.getProperty("program-lang", "es"),
            configProps.getProperty("program-country", "ES"));
    /**
     * Variable para almacenar aparte el texto del menú principal,
     * versión para users
     */
    private static final String mainMenu1 = String.format("\n%s:\n  (1) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (2) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (3) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (0) %s",
            rb.getString("program-intro-menu"), rb.getString("program-properties-field-1-plural"),
            rb.getString("program-properties-field-2-plural"),
            rb.getString("program-properties-field-3-plural"),
            rb.getString("program-intro-menu-exit"));
    /**
     * Variable para almacenar aparte el texto del menú principal,
     * versión para el administrador del programa
     */
    private static final String mainMenu2 = String.format("\n%s:\n  (1) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (2) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (3) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (4) " + rb.getString("program-intro-menu-seed") +
                                                          "\n  (0) %s",
            rb.getString("program-intro-menu"), rb.getString("program-properties-field-1-plural"),
            rb.getString("program-properties-field-2-plural"),
            rb.getString("program-properties-field-3-plural"),
            rb.getString("program-properties-field-4-plural"),
            rb.getString("program-intro-menu-exit"));
    /**
     * Número de books guardados dentro de la base de datos
     */
    private static int nBooks;
    /**
     * Máxima ID de books dentro de la base de datos
     */
    private static int idBooks;
    /**
     * Número de members registrados dentro de la base de datos
     */
    private static int nMembers;
    /**
     * Máxima ID de members dentro de la base de datos
     */
    private static int idMembers;
    /**
     * Número de préstamos en activo dentro de la base de datos
     */
    private static int nLoans;
    /**
     * Máxima ID de préstamos dentro de la base de datos
     */
    private static int idLoans;

    /**
     * Constructor privado de la clase para evitar instancias
     */
    private LibMenu() {
    }

    /**
     * Inicializador del programa Biblioteca
     *
     * @param args Lista de argumentos String por consola (sin uso actualmente)
     */
    public static void main(String[] args) {
        boolean repeat = true;
        User currentUser = null;

        if (configProps.getProperty("database-isbuilt").equals("false")) {
            DatabaseBuilder.sqlExecuter(configProps);
            configProps.setProperty("database-isbuilt", "true");
            try (FileOutputStream fos = new FileOutputStream("src/utils/configuration.properties")) {
                configProps.store(fos, rb.getString("program-properties"));
            } catch (IOException ioe) {
                System.err.printf("  %s: %s\n", rb.getString("program-error-properties"), ioe.getMessage());
            }
        }

        do {
            System.out.printf("\t\t|- G.A.B.I -|\n%s\n", rb.getString("program-name"));

            if (args.length != 0) {
                if (args.length == 2) {
                    try {
                        UserDerby.getInstance().tryUser(args[0], args[1].toCharArray());
                        currentUser = new User(args[0], args[1]);
                    } catch (RuntimeException re) {
                        System.err.printf("\n  %s: %s\n\n", rb.getString("program-error-intro"), re.getMessage());
                    }
                } else {
                    System.err.printf("  %s\n", rb.getString("program-error-args"));
                }
                args = new String[]{};
            }

            if (currentUser == null) {
                currentUser = acessBlock();
                if (currentUser == null) {
                    System.out.printf("\n%s...\n", rb.getString("program-exit-2"));
                    System.exit(0);
                }
            }

            selectionBlock(currentUser);

            System.out.printf("\n%s: ", rb.getString("program-intro-log-3"));
            if (!scanMenu.nextLine().equals("1")) {
                System.out.printf("\n%s...\n", rb.getString("program-exit-2"));
                repeat = false;
            } else {
                currentUser = null;
                System.out.println();
            }
        } while (repeat);

    }

    /**
     * Método que encierra la identificación de usuario del programa
     *
     * @return Objeto de usuario verificado
     */
    private static User acessBlock() {
        boolean isUser = false;
        String name = "";
        String password = "";

        do {
            try {
                System.out.printf("\n%s: ", rb.getString("program-intro-log-1"));
                name = scanMenu.nextLine();
                if (name.equals("-1")) {
                    return null;
                }
//                Console console = System.console();
//                char[] password = console.readPassword(String.format("\n%s: ", rb.getString("program-intro-password"));
//                Arrays.fill(password, 'x');
                System.out.printf("\n%s: ", rb.getString("program-intro-log-2"));
                password = scanMenu.nextLine();
                if (password.equals("-1")) {
                    return null;
                }
                UserDerby.getInstance().tryUser(name, password.toCharArray());
                isUser = true;
//            } catch (IllegalFormatException ife) {
//                System.err.println("\n" + ife.getMessage() + "\n");
            } catch (RuntimeException re) {
                System.err.printf("\n%s: %s\n\n", rb.getString("program-error-intro"), re.getMessage());
            }
        } while (!isUser);

        return new User(name, password);
    }

    /**
     * Método que encierra la selección del gestor en el que introducirse
     * el usuario verificado
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    private static void selectionBlock(User currentUser) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        BookMenu lMenu = new BookMenu(currentUser, configProps, rb);
        MemberMenu sMenu = new MemberMenu(currentUser, configProps, rb);
        LoanMenu pMenu = new LoanMenu(currentUser, configProps, rb);

        do {
            count = LibDBBook.getInstance().countTB(currentUser);
            if (count != null) {
                nBooks = count[0];
                idBooks = count[1];
            }
            count = LibDBMember.getInstance().countTB(currentUser);
            if (count != null) {
                nMembers = count[0];
                idMembers = count[1];
            }
            count = LibDBLoan.getInstance().countTB(currentUser);
            if (count != null) {
                nLoans = count[0];
                idLoans = count[1];
            }
            System.out.printf("\n" + rb.getString("program-intro-showinfo") + "\n", nBooks, nMembers, nLoans);

            System.out.println(currentUser.getName().equals(configProps.getProperty("database-name")) ? mainMenu2 : mainMenu1);

            optionMenu = checkOptionInput(scanMenu);

            switch (optionMenu) {
                case 1:
                    count = lMenu.selectionMenu(scanMenu, nBooks, idBooks);
                    nBooks = count[0];
                    idBooks = count[1];
                    break;
                case 2:
                    count = sMenu.selectionMenu(scanMenu, nMembers, idMembers);
                    nMembers = count[0];
                    idMembers = count[1];
                    break;
                case 3:
                    count = pMenu.selectionMenu(scanMenu, nLoans, idLoans);
                    nLoans = count[0];
                    idLoans = count[1];
                    break;
                case 4:
                    if (currentUser.getName().equals(configProps.getProperty("database-name"))) {
                        UserMenu uMenu = new UserMenu(currentUser, configProps, rb);
                        uMenu.selectionMenu(scanMenu);
                    } else {
                        System.err.printf("  %s\n", rb.getString("program-error-entry"));
                    }
                    break;
                case 0:
                    System.out.printf("%s...\n", rb.getString("program-exit-1"));
                    checkMenu = false;
                    break;
                default:
                    System.err.printf("  %s\n", rb.getString("program-error-entry"));
            }
        } while (checkMenu);
    }

}
