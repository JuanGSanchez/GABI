/**
 * Paquete de menús del programa
 */
package manager;

import sql.users.UserDerby;
import tables.Usuario;

import java.util.*;

/**
 * Clase del menú del gestor de usuarios en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
final class UserMenu {
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
                          
              Seleccione una acción:
            \t(1) Nuevo usuario
            \t(2) Listar usuarios
            \t(3) Buscar usuarios
            \t(4) Eliminar usuarios
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private static final String searchMenu = """
            \t(1) Por ID
            \t(2) Por nombre
            \t(0) Salir""";
    /**
     * Nombre de usuario pasado para la base de datos
     */
    private final String user;
    /**
     * Contraseña de usuario para la base de datos
     */
    private final String password;
    /**
     * Lista de propiedades comunes del programa
     */
    private final Properties configProps;

    /**
     * Constructor de la clase, restringido al paquete
     */
    UserMenu(String user, String password, Properties configProps) {
        this.user = user;
        this.password = password;
        this.configProps = configProps;
    }

    void selectionMenu(Scanner scan) {
        boolean checkMenu = true;
        int optionMenu;
        int nUsers = 0;
        int idUsers = 0;
        int[] count;

        System.out.println("\n\tGESTOR USUARIOS");

        count = UserDerby.getInstance().countUser(user, password.toCharArray());
        if (count != null) {
            nUsers = count[0];
            idUsers = count[1];
        }
        System.out.printf("\nHay %d usuarios registrados actualmente\n", nUsers);

        do {
            System.out.println(mainMenu);
            try {
                optionMenu = scan.nextInt();
            } catch (InputMismatchException ime) {
                optionMenu = -1;
            }
            scan.nextLine();
            switch (optionMenu) {
                case 1:
                    count = addUsuario(scan, nUsers, idUsers);
                    nUsers = count[0];
                    idUsers = count[1];
                    break;
                case 2:
                    if (nUsers == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        listUsuarios(scan);
                    }
                    break;
                case 3:
                    if (nUsers == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        searchUsuarios(scan);
                    }
                    break;
                case 4:
                    if (nUsers == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        count = deleteUsuarios(scan, nUsers, idUsers);
                        nUsers = count[0];
                        idUsers = count[1];
                    }
                    break;
                case 0:
                    System.out.println("Volviendo al menú principal...");
                    checkMenu = false;
                    break;
                default:
                    System.err.println("Entrada no válida");
            }
        } while (checkMenu);

    }

    int[] addUsuario(Scanner scan, int nUsers, int idUsers) {
        boolean isValid;
        boolean repeat;
        String nombre = null;
        String passwd = null;

        do {
            isValid = false;
            System.out.println("\n    Alta de Nuevo Usuario\n(-1 en cualquier momento para cancelar operación)\n");
            do {
                System.out.print("Introduce nombre - ");
                try {
                    nombre = scan.nextLine();
                    if ("".equals(nombre)) {
                        System.err.println("  Entrada vacía");
                    } else if (nombre.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nUsers, idUsers};
                    } else if (nombre.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (nombre.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  El nombre no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
                        nombre = nombre.trim();
                        nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            isValid = false;
            do {
                System.out.print("Introduce contraseña - ");
                try {
                    passwd = scan.nextLine();
                    if ("".equals(passwd)) {
                        System.err.println("  Entrada vacía");
                    } else if (passwd.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nUsers, idUsers};
                    } else if (passwd.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  La contraseña no puede contener números o caracteres especiales");
                    } else if (passwd.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  La contraseña no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
                        passwd = passwd.trim();
                        passwd = passwd.substring(0, 1).toUpperCase() + passwd.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                UserDerby.getInstance().addUser(user, password.toCharArray(), nombre, passwd.toCharArray());
                nUsers++;
                idUsers++;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nUsers, idUsers};

    }

    void listUsuarios(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Usuario> arrayUsuarios = new ArrayList<>();

        System.out.println("    Listado de Usuarios");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);
            try {
                opt = scan.nextInt();
                arrayUsuarios = UserDerby.getInstance().searchUser(user, password.toCharArray());
            } catch (InputMismatchException ime) {
                opt = -1;
            }
            scan.nextLine();
            switch (opt) {
                case 1:
                    System.out.println("Ordenación por ID...");
                    isValid = true;
                    break;
                case 2:
                    System.out.println("Ordenación por título...");
                    isValid = true;
                    break;
                case 3:
                    System.out.println("Ordenación por autor...");
                    isValid = true;
                    break;
                case 0:
                    System.out.println("  Volviendo al menú del gestor...");
                    return;
                default:
                    System.err.println("  Entrada no válida");
            }
        } while (!isValid);

    }

    void searchUsuarios(Scanner scan) {

    }

    int[] deleteUsuarios(Scanner scan, int nUsers, int idUsers) {

        return new int[]{nUsers, idUsers};

    }
}
