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
import utils.Utils;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.Scanner;

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
    private static final Properties configProps = Utils.readProperties();
    /**
     * Variable para almacenar aparte el texto del menú principal,
     * versión para users
     */
    private static final String mainMenu1 = """
                            
            Seleccione una de las opciones:
              (1) Gestor de Libros
              (2) Gestor de Socios
              (3) Gestor de Préstamos
              (0) Salir del sistema""";
    /**
     * Variable para almacenar aparte el texto del menú principal,
     * versión para el administrador del programa
     */
    private static final String mainMenu2 = """
                            
            Seleccione una de las opciones:
              (1) Gestor de Libros
              (2) Gestor de Socios
              (3) Gestor de Préstamos
              (4) Gestor de Usuarios
              (0) Salir del sistema""";
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
                configProps.store(fos, "Propiedades del programa GABI");
            } catch (IOException ioe) {
                System.err.println("  Error guardando las propiedades actualizadas: " + ioe.getMessage());
            }
        }

        do {
            System.out.println("\t\t|- G.A.B.I -|\n(Gestor Autónomo de Biblioteca Interactivo)");

            if (args.length != 0) {
                if (args.length == 2) {
                    try {
                        UserDerby.getInstance().tryUser(args[0], args[1].toCharArray());
                        currentUser = new User(args[0], args[1]);
                    } catch (RuntimeException re) {
                        System.err.println("\nError en el acceso a la base de datos: " + re.getMessage() + "\n");
                    }
                } else {
                    System.err.println("  Error en la entrada de argumentos al programa, número incorrecto de parámetros");
                }
                args = new String[]{};
            }

            if (currentUser == null) {
                currentUser = acessBlock();
                if (currentUser == null) {
                    System.out.println("\nSaliendo...");
                    System.exit(0);
                }
            }

            selectionBlock(currentUser);

            System.out.print("\nIntroduce 1 para cambiar de usuario: ");
            if (!scanMenu.nextLine().equals("1")) {
                System.out.println("\nSaliendo...");
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
                System.out.print("\nIntroduce nombre de usuario (-1 para salir): ");
                name = scanMenu.nextLine();
                if (name.equals("-1")) {
                    return null;
                }
//                Console console = System.console();
//                char[] password = console.readPassword("\nIntroduce contraseña: ");
//                Arrays.fill(password, 'x');
                System.out.print("\nIntroduce contraseña (-1 para salir): ");
                password = scanMenu.nextLine();
                if (password.equals("-1")) {
                    return null;
                }
                UserDerby.getInstance().tryUser(name, password.toCharArray());
                isUser = true;
//            } catch (IllegalFormatException ife) {
//                System.err.println("\nError en la lectura por consola\n");
            } catch (RuntimeException re) {
                System.err.println("\nError en el acceso a la base de datos: " + re.getMessage() + "\n");
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

        BookMenu lMenu = new BookMenu(currentUser, configProps);
        MemberMenu sMenu = new MemberMenu(currentUser, configProps);
        LoanMenu pMenu = new LoanMenu(currentUser, configProps);

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
            System.out.printf("\nHay %d libros, %d socios y %d préstamos registrados actualmente\n", nBooks, nMembers, nLoans);

            System.out.println(currentUser.getName().equals(configProps.getProperty("database-name")) ? mainMenu2 : mainMenu1);

            optionMenu = Utils.checkOptionInput(scanMenu);

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
                        UserMenu uMenu = new UserMenu(currentUser, configProps);
                        uMenu.selectionMenu(scanMenu);
                    } else {
                        System.err.println("  Entrada no válida");
                    }
                    break;
                case 0:
                    System.out.println("Saliendo del sistema...");
                    checkMenu = false;
                    break;
                default:
                    System.err.println("  Entrada no válida");
            }
        } while (checkMenu);
    }

}
