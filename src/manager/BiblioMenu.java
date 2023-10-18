/**
 * Paquete de menús del programa
 */
package manager;

import sql.BiblioDBLibro;
import sql.BiblioDBPrestamo;
import sql.BiblioDBSocio;
import sql.UserDerby;

import java.util.IllegalFormatException;
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
        boolean isUser = false;
        boolean checkMenu = true;
        int optionMenu;
        int[] count;
        String name;
        String password;

        System.out.println("\t\t|- G.A.B.I -|\n(Gestor Autónomo de Biblioteca Interactivo)");

        do {
            try {
                System.out.print("\nIntroduce nombre de usuario: ");
                name = scanMenu.nextLine();
//                Console console = System.console();
//                char[] password = console.readPassword("\nIntroduce contraseña: ");
//                Arrays.fill(password, 'x');
                System.out.print("\nIntroduce contraseña: ");
                password = scanMenu.nextLine();
                UserDerby.getInstance().tryUser(name, password.toCharArray());
                isUser = true;
            } catch (IllegalFormatException ife) {
                System.err.println("\nError en la lectura por consola\n");
            } catch (RuntimeException re) {
                System.err.println("\nError en el acceso a la base de datos: " + re.getMessage() + "\n");
            }
        } while (!isUser);

        do {
            count = BiblioDBLibro.getInstance().countTB();
            if (count != null) {
                nLibros = count[0];
                idLibros = count[1];
            }
            count = BiblioDBSocio.getInstance().countTB();
            if (count != null) {
                nSocios = count[0];
                idSocios = count[1];
            }
            count = BiblioDBPrestamo.getInstance().countTB();
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
                    count = LibroMenu.seleccionMenu(scanMenu, nLibros, idLibros);
                    nLibros = count[0];
                    idLibros = count[1];
                    break;
                case 2:
                    scanMenu.nextLine();
                    count = SocioMenu.seleccionMenu(scanMenu, nSocios, idSocios);
                    nSocios = count[0];
                    idSocios = count[1];
                    break;
                case 3:
                    scanMenu.nextLine();
                    count = PrestMenu.seleccionMenu(scanMenu, nPrestamos, idPrestamos);
                    nPrestamos = count[0];
                    idPrestamos = count[1];
                    break;
                case 0:
                    System.out.println("Saliendo del sistema...");
                    checkMenu = false;
                    break;
                default:
                    System.err.println("Entrada no válida");
                    scanMenu.nextLine();
            }
        } while (checkMenu);
    }

}
