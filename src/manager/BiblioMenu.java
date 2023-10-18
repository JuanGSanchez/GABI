/**
 * Paquete de menús del programa
 */
package manager;

import sql.BiblioDBLibro;
import sql.BiblioDBPrestamo;
import sql.BiblioDBSocio;
import sql.UserDerby;

import java.util.InputMismatchException;
import java.util.Scanner;

/**
 * Clase del menú principal del programa de gestión Biblioteca
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public final class BiblioMenu {
    /**
     * Variable de entrada de datos por teclado
     */
    private static final Scanner scanMenu = new Scanner(System.in);
    /**
     * Variable para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
                            
            Seleccione una de las opciones:
              (1) Gestor de Libros
              (2) Gestor de Socios
              (3) Gestor de Préstamos
              (0) Salir del sistema""";
    /**
     * Número de libros guardados dentro de la base de datos
     */
    private static int nLibros;
    /**
     * Máxima ID de libros dentro de la base de datos
     */
    private static int idLibros;
    /**
     * Número de socios registrados dentro de la base de datos
     */
    private static int nSocios;
    /**
     * Máxima ID de socios dentro de la base de datos
     */
    private static int idSocios;
    /**
     * Número de préstamos en activo dentro de la base de datos
     */
    private static int nPrestamos;
    /**
     * Máxima ID de préstamos dentro de la base de datos
     */
    private static int idPrestamos;

    /**
     * Constructor privado de la clase para evitar instancias
     */
    private BiblioMenu() {
    }

    /**
     * Inicializador del programa Biblioteca
     *
     * @param args Lista de argumentos String por consola (sin uso actualmente)
     */
    public static void main(String[] args) {
        boolean repeat = true;
        String name = null;
        String password = null;

        do {
            System.out.println("\t\t|- G.A.B.I -|\n(Gestor Autónomo de Biblioteca Interactivo)");

            if (args.length != 0) {
                if (args.length == 2) {
                    try {
                        UserDerby.getInstance().tryUser(args[0], args[1].toCharArray());
                        name = args[0];
                        password = args[1];
                    } catch (RuntimeException re) {
                        System.err.println("\nError en el acceso a la base de datos: " + re.getMessage() + "\n");
                    }
                } else {
                    System.err.println("  Error en la entrada de argumentos al programa, número incorrecto de parámetros");
                }
                args = new String[]{};
            }

            if (name == null) {
                String[] accessData = acessBlock();
                name = accessData[0];
                password = accessData[1];
                accessData = null;
                if (name.isEmpty()) {
                    System.out.println("\nSaliendo...");
                    System.exit(0);
                }
            }

            selectionBlock(name, password);

            System.out.print("\nIntroduce 1 para cambiar de usuario: ");
            if (!scanMenu.nextLine().equals("1")){
                System.out.println("\nSaliendo...");
                repeat = false;
            } else {
                name = null;
                System.out.println();
            }
        } while (repeat);

    }

    /**
     * Método que encierra la identificación de usuario del programa
     *
     * @return Datos de acceso del usuario verificado
     */
    private static String[] acessBlock() {
        boolean isUser = false;
        String name = "";
        String password = "";

        do {
            try {
                System.out.print("\nIntroduce nombre de usuario (-1 para salir): ");
                name = scanMenu.nextLine();
                if (name.equals("-1")) {
                    return new String[]{"", ""};
                }
//                Console console = System.console();
//                char[] password = console.readPassword("\nIntroduce contraseña: ");
//                Arrays.fill(password, 'x');
                System.out.print("\nIntroduce contraseña (-1 para salir): ");
                password = scanMenu.nextLine();
                if (password.equals("-1")) {
                    return new String[]{"", ""};
                }
                UserDerby.getInstance().tryUser(name, password.toCharArray());
                isUser = true;
//            } catch (IllegalFormatException ife) {
//                System.err.println("\nError en la lectura por consola\n");
            } catch (RuntimeException re) {
                System.err.println("\nError en el acceso a la base de datos: " + re.getMessage() + "\n");
            }
        } while (!isUser);

        return new String[]{name, password};
    }

    /**
     * Método que encierra la selección del gestor en el que introducirse
     * el usuario verificado
     *
     * @param name     Nombre del usuario para el acceso a la base de datos
     * @param password Contraseña del usuario para el acceso a la base de datos
     */
    private static void selectionBlock(String name, String password) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        LibroMenu lMenu = new LibroMenu(name, password);
        SocioMenu sMenu = new SocioMenu(name, password);
        PrestMenu pMenu = new PrestMenu(name, password);

        do {
            count = BiblioDBLibro.getInstance().countTB(name, password);
            if (count != null) {
                nLibros = count[0];
                idLibros = count[1];
            }
            count = BiblioDBSocio.getInstance().countTB(name, password);
            if (count != null) {
                nSocios = count[0];
                idSocios = count[1];
            }
            count = BiblioDBPrestamo.getInstance().countTB(name, password);
            if (count != null) {
                nPrestamos = count[0];
                idPrestamos = count[1];
            }
            System.out.printf("\nHay %d libros, %d socios y %d préstamos registrados actualmente\n", nLibros, nSocios, nPrestamos);

            System.out.println(mainMenu);
            try {
                optionMenu = scanMenu.nextInt();
            } catch (InputMismatchException ime) {
                optionMenu = -1;
            }
            switch (optionMenu) {
                case 1:
                    scanMenu.nextLine();
                    count = lMenu.seleccionMenu(scanMenu, nLibros, idLibros);
                    nLibros = count[0];
                    idLibros = count[1];
                    break;
                case 2:
                    scanMenu.nextLine();
                    count = sMenu.seleccionMenu(scanMenu, nSocios, idSocios);
                    nSocios = count[0];
                    idSocios = count[1];
                    break;
                case 3:
                    scanMenu.nextLine();
                    count = pMenu.seleccionMenu(scanMenu, nPrestamos, idPrestamos);
                    nPrestamos = count[0];
                    idPrestamos = count[1];
                    break;
                case 0:
                    System.out.println("Saliendo del sistema...");
                    scanMenu.nextLine();
                    checkMenu = false;
                    break;
                default:
                    System.err.println("Entrada no válida");
                    scanMenu.nextLine();
            }
        } while (checkMenu);
    }

}
