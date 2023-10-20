/**
 * Paquete de menús del programa
 */
package manager;

import sql.users.UserDerby;
import tables.User;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase del menú del gestor de users en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
final class UserMenu {
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta
     */
    private static final String[] searchVar = {"ID", "nombre o fragmento"};
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
                          
              Seleccione una acción:
            \t(1) Nuevo user
            \t(2) Listar users
            \t(3) Buscar users
            \t(4) Eliminar users
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private static final String searchMenu = """
            \t(1) Por ID
            \t(2) Por nombre
            \t(0) Salir""";
    /**
     * Nombre de user pasado para la base de datos
     */
    private final String user;
    /**
     * Contraseña de user para la base de datos
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

    /**
     * Método del menú principal del gestor de users, desde el cual se acceden
     * a las acciones disponibles
     *
     * @param scan Entrada de datos por teclado
     */
    void selectionMenu(Scanner scan) {
        boolean checkMenu = true;
        int optionMenu;
        int nUser = 0;
        int idUser = 0;
        int[] count;

        System.out.println("\n\tGESTOR USUARIOS");

        count = UserDerby.getInstance().countUser(user, password.toCharArray());
        if (count != null) {
            nUser = count[0];
            idUser = count[1];
        }
        System.out.printf("\nHay %d usuarios registrados actualmente\n", nUser);

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
                    count = addUsuario(scan, nUser, idUser);
                    nUser = count[0];
                    idUser = count[1];
                    break;
                case 2:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        System.out.println("Total de users: " + nUser);
                        listUsuarios(scan);
                    }
                    break;
                case 3:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        searchUsuarios(scan);
                    }
                    break;
                case 4:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        count = deleteUsuarios(scan, nUser, idUser);
                        nUser = count[0];
                        idUser = count[1];
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

    /**
     * Método para añadir users a la base de datos
     *
     * @param scan    Entrada de datos por teclado
     * @param nUser  Número de users registrados en la base de datos
     * @param idUser Máxima ID de user dentro de la base de datos
     * @return Valores actualizados de nUser y idUser
     */
    int[] addUsuario(Scanner scan, int nUser, int idUser) {
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
                        return new int[]{nUser, idUser};
                    } else if (nombre.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (nombre.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  El nombre no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
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
                        return new int[]{nUser, idUser};
                    } else if (passwd.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  La contraseña no puede contener números o caracteres especiales");
                    } else if (passwd.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  La contraseña no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                UserDerby.getInstance().addUser(user, password.toCharArray(), new User(idUser + 1, nombre, passwd));
                nUser++;
                idUser++;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nUser, idUser};

    }

    /**
     * Método para imprimir en pantalla los users registrados en la base de
     * datos
     *
     * @param scan Entrada de datos por teclado
     */
    void listUsuarios(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<User> arrayUsers = new ArrayList<>();

        System.out.println("    Listado de Usuarios");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);
            try {
                opt = scan.nextInt();
                arrayUsers = UserDerby.getInstance().searchUser(user, password.toCharArray());
            } catch (InputMismatchException ime) {
                opt = -1;
            }
            scan.nextLine();
            switch (opt) {
                case 1:
                    System.out.println("Ordenación por ID...");
                    arrayUsers.stream().sorted(User::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("Ordenación por nombre...");
                    arrayUsers.stream().sorted(Comparator.comparing(User::getName).thenComparing(User::getIdUser)).forEach(System.out::println);
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

    /**
     * Método para buscar users en la base de datos según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    void searchUsuarios(Scanner scan) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.println("    Buscador de Usuarios");
            do {
                System.out.println("\nSelecciona criterio de búsqueda -\n" + searchMenu);
                try {
                    opt = scan.nextInt();
                } catch (InputMismatchException ime) {
                    opt = -1;
                }
                scan.nextLine();
                switch (opt) {
                    case 1:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        try {
                            ID = scan.nextInt();
                            isValid = true;
                        } catch (InputMismatchException ime) {
                            System.err.println("  Entrada no válida");
                        }
                        scan.nextLine();
                        break;
                    case 2:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        fragString = scan.nextLine();
                        isValid = true;
                        break;
                    case 0:
                        System.out.println("  Volviendo al menú del gestor...");
                        return;
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    User user = UserDerby.getInstance().searchUser(this.user, password.toCharArray(), ID);
                    System.out.println(user);
                } else {
                    List<User> users = UserDerby.getInstance().searchUser(user, password.toCharArray(), fragString);
                    users.forEach(System.out::println);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para eliminar users de la base de datos
     *
     * @param scan    Entrada de datos por teclado
     * @param nUser  Número de users registrados en la base de datos
     * @param idUser Máxima ID de user dentro de la base de datos
     * @return Valores actualizados de nUser y idUser
     */
    int[] deleteUsuarios(Scanner scan, int nUser, int idUser) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.println("    Baja de Usuarios");
            do {
                System.out.println("\nSelecciona criterio de búsqueda -\n" + searchMenu);
                try {
                    opt = scan.nextInt();
                } catch (InputMismatchException ime) {
                    opt = -1;
                }
                scan.nextLine();
                switch (opt) {
                    case 1:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        try {
                            ID = scan.nextInt();
                            isValid = true;
                        } catch (InputMismatchException ime) {
                            System.err.println("  Entrada no válida");
                        }
                        scan.nextLine();
                        break;
                    case 2:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        fragString = scan.nextLine();
                        isValid = true;
                        break;
                    case 0:
                        System.out.println("  Volviendo al menú del gestor...");
                        return new int[]{nUser, idUser};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    idUser = UserDerby.getInstance().deleteUser(user, password.toCharArray(), ID);
                    nUser--;
                } else {
                    List<User> users = UserDerby.getInstance().searchUser(user, password.toCharArray(), fragString);
                    Set<Integer> idusers = users.stream().map(User::getIdUser).collect(Collectors.toSet());
                    users.stream().sorted(User::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del usuario a eliminar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nUser, idUser};
                            } else if (!idusers.add(ID)) {
                                idUser = UserDerby.getInstance().deleteUser(user, password.toCharArray(), ID);
                                nUser--;
                                isValid = false;
                            } else {
                                System.err.println("El ID proporcionado no se encuentra en la lista");
                            }
                        } catch (InputMismatchException ime) {
                            System.err.println("Entrada no válida");
                        }
                        scan.nextLine();
                    } while (isValid);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nUser, idUser};

    }
}
