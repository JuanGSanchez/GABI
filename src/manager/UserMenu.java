/**
 * Paquete de menús del programa
 */
package manager;

import sql.users.UserDerby;
import tables.User;

import java.util.*;
import java.util.stream.Collectors;

import static utils.Utils.*;

/**
 * Clase del menú del gestor de users en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
final class UserMenu {
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private final String mainMenu;
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private final String searchMenu;
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta
     */
    private final String[] searchVar;
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
     * Recurso para la localización del texto del programa
     */
    private final ResourceBundle rb;

    /**
     * Constructor de la clase, restringido al paquete
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param configProps Lista de propiedades comunes del programa
     */
    UserMenu(User currentUser, Properties configProps, ResourceBundle rb) {
        this.currentUser = currentUser;
        this.configProps = configProps;
        this.rb = rb;
        mainMenu = String.format("\n  %s:\n\t(1) " + rb.getString("program-user-menu-1") +
                                 "\n\t(2) " + rb.getString("program-user-menu-2") +
                                 "\n\t(3) " + rb.getString("program-user-menu-3") +
                                 "\n\t(4) " + rb.getString("program-user-menu-4") +
                                 "\n\t(0) %s",
                rb.getString("program-general-menu"), rb.getString("program-properties-field-4-singular").toLowerCase(),
                rb.getString("program-properties-field-4-plural").toLowerCase(),
                rb.getString("program-properties-field-4-plural").toLowerCase(),
                rb.getString("program-properties-field-4-singular").toLowerCase(),
                rb.getString("program-general-exit-menu"));
        searchMenu = String.format("\n\t(1) " + rb.getString("program-general-order-election") +
                                   "\n\t(2) " + rb.getString("program-general-order-election") +
                                   "\n\t(0) %s",
                rb.getString("program-user-properties-1"),
                rb.getString("program-user-properties-2"),
                rb.getString("program-general-exit-order"));
        searchVar = new String[]{rb.getString("program-user-properties-1"),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-user-properties-2"))};
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

        System.out.println("\n\t" + String.format(rb.getString("program-intro-menu-seed"),
                rb.getString("program-properties-field-4-plural")).toUpperCase());

        count = UserDerby.getInstance().countUser(currentUser);
        if (count != null) {
            nUser = count[0];
            idUser = count[1];
        }
        System.out.printf("\n" + rb.getString("program-user-showinfo") + "\n", nUser);

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
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-4-plural").toLowerCase());
                    } else {
                        listUsers(scan);
                        System.out.printf(rb.getString("program-general-total") + ": %d\n",
                                rb.getString("program-properties-field-4-plural").toLowerCase(), nUser);
                    }
                    break;
                case 3:
                    if (nUser == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-4-plural").toLowerCase());
                    } else {
                        searchUsers(scan);
                    }
                    break;
                case 4:
                    if (nUser == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-4-plural").toLowerCase());
                    } else {
                        count = deleteUser(scan, nUser, idUser);
                        nUser = count[0];
                        idUser = count[1];
                    }
                    break;
                case 0:
                    System.out.printf("  %s...\n", rb.getString("program-return-3"));
                    checkMenu = false;
                    break;
                default:
                    System.err.printf("  %s\n", rb.getString("program-error-entry"));
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
            System.out.printf("    " + rb.getString("program-general-add-1") + "\n(%s)\n\n",
                    rb.getString("program-properties-field-4-singular"), rb.getString("program-general-cancel"));

            name = checkString(scan, 4, configProps.getProperty("database-table-4-field-2-maxchar"));
            if (name == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nUser, idUser};
            }

            password = checkString(scan, 5, configProps.getProperty("database-table-4-field-2-maxchar"));
            if (password == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nUser, idUser};
            }

            try {
                UserDerby.getInstance().addUser(currentUser, new User(idUser + 1, name, password));
                nUser++;
                idUser++;
            } catch (RuntimeException re) {
                System.err.printf("  %s: %s\n", rb.getString("program-error-database"), re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
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

        System.out.printf("    " + rb.getString("program-general-list") + "\n",
                rb.getString("program-properties-field-4-plural"));
        do {
            System.out.printf("\n  %s -%s\n", rb.getString("program-general-order"), searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayUsers = UserDerby.getInstance().searchUser(currentUser);
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-user-properties-1"));
                    arrayUsers.stream().sorted(User::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    arrayUsers = UserDerby.getInstance().searchUser(currentUser);
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-user-properties-2"));
                    arrayUsers.stream().sorted(Comparator.comparing(User::getName).thenComparing(User::getIdUser)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 0:
                    System.out.printf("  %s...\n", rb.getString("program-return-1"));
                    return;
                default:
                    System.err.printf("  %s\n", rb.getString("program-error-entry"));
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
            System.out.printf("    " + rb.getString("program-general-search") + "\n",
                    rb.getString("program-properties-field-4-plural"));
            do {
                System.out.printf("\n  %s -%s\n", rb.getString("program-general-criteria"), searchMenu);

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

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
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
            System.out.printf("    " + rb.getString("program-general-delete-1") + "\n",
                    rb.getString("program-properties-field-4-singular"));
            do {
                System.out.printf("\n  %s -%s\n", rb.getString("program-general-criteria"), searchMenu);

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
                        System.out.printf(rb.getString("program-general-enter") + "\n(%s) -\n",
                                rb.getString("program-properties-field-4-singular").toLowerCase(),
                                rb.getString("program-general-cancel"));
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                                        rb.getString("program-return-1").toLowerCase());
                                return new int[]{nUser, idUser};
                            } else if (!idusers.add(ID)) {
                                idUser = UserDerby.getInstance().deleteUser(currentUser, ID);
                                nUser--;
                                isValid = false;
                            } else {
                                System.err.printf("  %s\n", rb.getString("program-error-id"));
                            }
                        } catch (InputMismatchException ime) {
                            System.err.printf("  %s\n", rb.getString("program-error-entry"));
                        }
                        scan.nextLine();
                    } while (isValid);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
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
                System.out.printf("%s %s -\n", rb.getString("program-general-intro"), searchVar[opt - 1]);
                obj = checkOptionInput(scan, String.format("  %s\n", rb.getString("program-error-entry")));
                break;
            case 2:
                System.out.printf("%s %s -\n", rb.getString("program-general-intro"), searchVar[opt - 1]);
                obj = scan.nextLine();
                break;
            case 0:
                System.out.printf("  %s...\n", rb.getString("program-return-1"));
                obj = 0.;
                break;
            default:
                System.err.printf("  %s\n", rb.getString("program-error-entry"));
        }

        return obj;
    }

}
