/**
 * Paquete de menús del programa
 */
package manager;

import sql.users.UserDerby;
import tables.User;

import java.util.*;
import java.util.stream.Collectors;

import static utils.Utils.checkOptionInput;
import static utils.Utils.checkString;

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
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param configProps Lista de propiedades comunes del programa
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

            optionMenu = checkOptionInput(scan);

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
     * Método para añadir usuarios a la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nUser  Número de usuarios registrados dentro de la base de datos
     * @param idUser Máxima ID de usuario dentro de la base de datos
     * @return Valores actualizados de nUser y idUser
     */
    private int[] addUser(Scanner scan, int nUser, int idUser) {
        boolean repeat;
        String name;
        String password;

        do {
            System.out.println("\n    Alta de Nuevo Usuario\n(-1 en cualquier momento para cancelar operación)\n");

            name = checkString(scan, 4, configProps.getProperty("database-table-4-field-2-maxchar"));
            if (name == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nUser, idUser};
            }

            password = checkString(scan, 5, configProps.getProperty("database-table-4-field-2-maxchar"));
            if (password == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nUser, idUser};
            }

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
     * Método para imprimir en pantalla los usuarios
     * registrados en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listUsers(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<User> arrayUsers;

        System.out.println("    Listado de Usuarios");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayUsers = UserDerby.getInstance().searchUser(currentUser);
                    System.out.println("Ordenación por ID...");
                    arrayUsers.stream().sorted(User::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    arrayUsers = UserDerby.getInstance().searchUser(currentUser);
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
     * Método para buscar users en la base de datos
     * según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchUsers(Scanner scan) {
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

                opt = checkOptionInput(scan);

                Object o = checkCase(scan, opt);
                if (o instanceof String) {
                    fragString = (String) o;
                    isValid = true;
                } else if (o instanceof Integer) {
                    ID = (Integer) o;
                    isValid = true;
                } else if (o instanceof Double) {
                    return;
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
     * Método para eliminar usuarios de la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nUser  Número de users registrados en la base de datos
     * @param idUser Máxima ID de user dentro de la base de datos
     * @return Valores actualizados de nUser y idUser
     */
    private int[] deleteUser(Scanner scan, int nUser, int idUser) {
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

                opt = checkOptionInput(scan);

                Object o = checkCase(scan, opt);
                if (o instanceof String) {
                    fragString = (String) o;
                    isValid = true;
                } else if (o instanceof Integer) {
                    ID = (Integer) o;
                    isValid = true;
                } else if (o instanceof Double) {
                    return new int[]{nUser, idUser};
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
                            System.err.println("  Entrada no válida");
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

    /**
     * Método con la selección para
     * el criterio de búsqueda en la tabla de datos
     *
     * @param scan Entrada de datos por teclado
     * @param opt  Índice del criterio de búsqueda
     * @return Resultado de la lectura de opt
     */
    private Object checkCase(Scanner scan, int opt) {
        Object obj = null;

        switch (opt) {
            case 1:
                System.out.printf("Introduce %s -\n", searchVar[opt - 1]);
                obj = checkOptionInput(scan, "  Entrada no válida");
                break;
            case 2:
                System.out.printf("Introduce %s -\n", searchVar[opt - 1]);
                obj = scan.nextLine();
                break;
            case 0:
                System.out.println("  Volviendo al menú del gestor...");
                obj = 0.;
                break;
            default:
                System.err.println("  Entrada no válida");
        }

        return obj;
    }

}
