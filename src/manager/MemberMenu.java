/**
 * Paquete de menús del programa
 */
package manager;

import sql.reservoirs.LibDBMember;
import tables.Member;
import tables.User;

import java.util.*;
import java.util.stream.Collectors;

import static utils.Utils.*;

/**
 * Clase del menú del gestor de members en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class MemberMenu {
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private final String mainMenu;
    /**
     * Variable para almacenar las opciones de submenús
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
    MemberMenu(User currentUser, Properties configProps, ResourceBundle rb) {
        this.currentUser = currentUser;
        this.configProps = configProps;
        this.rb = rb;
        mainMenu = String.format("\n  %s:\n\t(1) " + rb.getString("program-member-menu-1") +
                                 "\n\t(2) " + rb.getString("program-member-menu-2") +
                                 "\n\t(3) " + rb.getString("program-member-menu-3") +
                                 "\n\t(4) " + rb.getString("program-member-menu-4") +
                                 "\n\t(0) %s",
                rb.getString("program-general-menu"), rb.getString("program-properties-field-2-singular").toLowerCase(),
                rb.getString("program-properties-field-2-plural").toLowerCase(),
                rb.getString("program-properties-field-2-plural").toLowerCase(),
                rb.getString("program-properties-field-2-singular").toLowerCase(),
                rb.getString("program-general-exit-menu"));
        searchMenu = String.format("\n\t(1) " + rb.getString("program-general-order-election") +
                                   "\n\t(2) " + rb.getString("program-general-order-election") +
                                   "\n\t(3) " + rb.getString("program-general-order-election") +
                                   "\n\t(0) %s",
                rb.getString("program-member-properties-1"),
                rb.getString("program-member-properties-2"),
                rb.getString("program-member-properties-3"),
                rb.getString("program-general-exit-order"));
        searchVar = new String[]{rb.getString("program-member-properties-1"),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-member-properties-2")),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-member-properties-3"))};
    }

    /**
     * Método del menú principal del gestor de members, desde el cual
     * se acceden a las acciones disponibles
     *
     * @param scan     Entrada de datos por teclado
     * @param nMember  Número de members registrados dentro de la base de datos
     * @param idMember Máxima ID de members dentro de la base de datos
     * @return Valores actualizados de nMember y idMember
     */
    int[] selectionMenu(Scanner scan, int nMember, int idMember) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        System.out.println("\n\t" + String.format(rb.getString("program-intro-menu-seed"),
                rb.getString("program-properties-field-2-plural")).toUpperCase());
        do {
            System.out.println(mainMenu);

            optionMenu = checkOptionInput(scan);

            switch (optionMenu) {
                case 1:
                    count = addMember(scan, nMember, idMember);
                    nMember = count[0];
                    idMember = count[1];
                    break;
                case 2:
                    if (nMember == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-2-plural").toLowerCase());
                    } else {
                        listMembers(scan);
                        System.out.printf(rb.getString("program-general-total") + ": %d\n",
                                rb.getString("program-properties-field-2-plural").toLowerCase(), nMember);
                    }
                    break;
                case 3:
                    if (nMember == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-2-plural").toLowerCase());
                    } else {
                        searchMembers(scan);
                    }
                    break;
                case 4:
                    if (nMember == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-2-plural").toLowerCase());
                    } else {
                        count = deleteMember(scan, nMember, idMember);
                        nMember = count[0];
                        idMember = count[1];
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

        return new int[]{nMember, idMember};
    }

    /**
     * Método para dar de alta nuevos socios en la base de datos
     *
     * @param scan     Entrada de datos por teclado
     * @param nMember  Número de socios registrados dentro de la base de datos
     * @param idMember Máxima ID de socios dentro de la base de datos
     * @return Valores actualizados de nMember y idMember
     */
    private int[] addMember(Scanner scan, int nMember, int idMember) {
        boolean repeat;
        String name;
        String surname;

        do {
            System.out.printf("    " + rb.getString("program-general-add-1") + "\n(%s)\n\n",
                    rb.getString("program-properties-field-2-singular"), rb.getString("program-general-cancel"));

            name = checkString(scan, 2, configProps.getProperty("database-table-2-field-2-maxchar"));
            if (name == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nMember, idMember};
            }

            surname = checkString(scan, 3, configProps.getProperty("database-table-2-field-3-maxchar"));
            if (surname == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nMember, idMember};
            }

            try {
                LibDBMember.getInstance().addTB(currentUser, new Member(nMember + 1, name, surname), rb);
                nMember++;
                idMember++;
            } catch (RuntimeException re) {
                System.err.printf("  %s: %s\n", rb.getString("program-error-database"), re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nMember, idMember};
    }

    /**
     * Método para imprimir en pantalla los socios
     * registrados en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listMembers(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Member> arrayMembers;

        System.out.printf("    " + rb.getString("program-general-list") + "\n",
                rb.getString("program-properties-field-2-plural"));
        do {
            System.out.printf("\n  %s -%s\n", rb.getString("program-general-order"), searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-member-properties-1"));
                    arrayMembers.stream().sorted(Member::compareTo)
                            .forEach(member -> System.out.println(MemberToString(member)));
                    isValid = true;
                    break;
                case 2:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-member-properties-2"));
                    arrayMembers.stream().sorted(Comparator.comparing(Member::getName).thenComparing(Member::getSurname))
                            .forEach(member -> System.out.println(MemberToString(member)));
                    isValid = true;
                    break;
                case 3:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-member-properties-3"));
                    arrayMembers.stream().sorted(Comparator.comparing(Member::getSurname).thenComparing(Member::getName))
                            .forEach(member -> System.out.println(MemberToString(member)));
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
     * Método para buscar members en la base de datos
     * según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchMembers(Scanner scan) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-search") + "\n",
                    rb.getString("program-properties-field-2-plural"));
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
                    Member member = LibDBMember.getInstance().searchTB(currentUser, ID, rb);
                    System.out.println(member);
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString, rb);
                    members.forEach(System.out::println);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para dar de baja socios de la base de datos
     *
     * @param scan     Entrada de datos por teclado
     * @param nMember  Número de members registrados dentro de la base de datos
     * @param idMember Máxima ID de members dentro de la base de datos
     * @return Valores actualizados de nMember y idMember
     */
    private int[] deleteMember(Scanner scan, int nMember, int idMember) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-delete-1") + "\n",
                    rb.getString("program-properties-field-2-singular"));
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
                    return new int[]{nMember, idMember};
                }

            } while (!isValid);

            try {
                if (opt == 1) {
                    idMember = LibDBMember.getInstance().deleteTB(currentUser, ID, rb);
                    nMember--;
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString, rb);
                    Set<Integer> idmembers = members.stream().map(Member::getIdMember).collect(Collectors.toSet());
                    members.stream().sorted(Member::compareTo)
                            .forEach(member -> System.out.println(MemberToString(member)));
                    do {
                        System.out.printf(rb.getString("program-general-enter") + "\n(%s) -\n",
                                rb.getString("program-properties-field-2-singular").toLowerCase(),
                                rb.getString("program-general-cancel"));
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                                        rb.getString("program-return-1").toLowerCase());
                                return new int[]{nMember, idMember};
                            } else if (!idmembers.add(ID)) {
                                idMember = LibDBMember.getInstance().deleteTB(currentUser, ID, rb);
                                nMember--;
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

        return new int[]{nMember, idMember};
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
            case 3:
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
