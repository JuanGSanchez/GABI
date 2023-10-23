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
            \t(1) Nuevo usuario
            \t(2) Listar usuarios
            \t(3) Buscar usuarios
            \t(4) Eliminar usuario
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private static final String searchMenu = """
            \t(1) Por ID
            \t(2) Por nombre
            \t(0) Salir""";
    /**
     * Objeto usuario para almacenar sus datos
     * de acceso a la base de datos
     */
    private final User currentUser;
    /**
     * Lista de propiedades comunes del programa
     */
    private final Properties configProps;

    /**
     * Constructor de la clase, restringido al paquete
     */
    UserMenu(User currentUser, Properties configProps) {
        this.currentUser = currentUser;
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

        count = UserDerby.getInstance().countUser(currentUser);
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
                    count = addUser(scan, nUser, idUser);
                    nUser = count[0];
                    idUser = count[1];
                    break;
                case 2:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        listUsers(scan);
                        System.out.println("Total de usuarios: " + nUser);
                    }
                    break;
                case 3:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        searchUsers(scan);
                    }
                    break;
                case 4:
                    if (nUser == 0) {
                        System.err.println("Error, no hay lista de usuarios disponible");
                    } else {
                        count = deleteUser(scan, nUser, idUser);
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
    int[] addUser(Scanner scan, int nUser, int idUser) {
        boolean isValid;
        boolean repeat;
        String name = null;
        String password = null;

        do {
            isValid = false;
            System.out.println("\n    Alta de Nuevo Usuario\n(-1 en cualquier momento para cancelar operación)\n");
            do {
                System.out.print("Introduce nombre - ");
                try {
                    name = scan.nextLine();
                    if ("".equals(name)) {
                        System.err.println("  Entrada vacía");
                    } else if (name.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nUser, idUser};
                    } else if (name.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (name.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  El nombre no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
                        name = name.toLowerCase();
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
                    password = scan.nextLine();
                    if ("".equals(password)) {
                        System.err.println("  Entrada vacía");
                    } else if (password.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nUser, idUser};
                    } else if (password.matches(".*[\\d¡!@#$%&ºª'`* .,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  La contraseña no puede contener números o caracteres especiales");
                    } else if (password.trim().length() > Integer.parseInt(configProps.getProperty("database-table-4-field-2-maxchar"))) {
                        System.err.printf("  La contraseña no puede superar los %s caracteres\n", configProps.getProperty("database-table-4-field-2-maxchar"));
                    } else {
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                UserDerby.getInstance().addUser(currentUser, new User(idUser + 1, name, password));
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
    void listUsers(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<User> arrayUsers = new ArrayList<>();

        System.out.println("    Listado de Usuarios");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);
            try {
                opt = scan.nextInt();
                arrayUsers = UserDerby.getInstance().searchUser(currentUser);
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
    void searchUsers(Scanner scan) {
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
                    User user = UserDerby.getInstance().searchUser(currentUser, ID);
                    System.out.println(user);
                } else {
                    List<User> users = UserDerby.getInstance().searchUser(currentUser, fragString);
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
    int[] deleteUser(Scanner scan, int nUser, int idUser) {
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
                    idUser = UserDerby.getInstance().deleteUser(currentUser, ID);
                    nUser--;
                } else {
                    List<User> users = UserDerby.getInstance().searchUser(currentUser, fragString);
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
                                idUser = UserDerby.getInstance().deleteUser(currentUser, ID);
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
