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
     * Lista con fragmentos de texto según el parámetro usado en la consulta
     */
    private static final String[] searchVar = {"ID", "nombre o fragmento", "apellido o fragmento"};
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
              
              Seleccione una acción:
            \t(1) Alta socio
            \t(2) Directorio socios
            \t(3) Buscar socios
            \t(4) Baja socio
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús
     */
    private static final String searchMenu = """
            \t(1) Por ID
            \t(2) Por nombre
            \t(3) Por apellidos
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
    MemberMenu(User currentUser, Properties configProps) {
        this.currentUser = currentUser;
        this.configProps = configProps;
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

        System.out.println("\n\tGESTOR SOCIOS");
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
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        listMembers(scan);
                        System.out.println("Total de socios: " + nMember);
                    }
                    break;
                case 3:
                    if (nMember == 0) {
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        searchMembers(scan);
                    }
                    break;
                case 4:
                    if (nMember == 0) {
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        count = deleteMember(scan, nMember, idMember);
                        nMember = count[0];
                        idMember = count[1];
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
            System.out.println("\n    Alta de Nuevo Socio\n(-1 en cualquier momento para cancelar operación)\n");

            name = checkString(scan, 2, configProps.getProperty("database-table-2-field-2-maxchar"));
            if (name == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nMember, idMember};
            }

            surname = checkString(scan, 3, configProps.getProperty("database-table-2-field-3-maxchar"));
            if (surname == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nMember, idMember};
            }

            try {
                LibDBMember.getInstance().addTB(currentUser, new Member(nMember + 1, name, surname));
                nMember++;
                idMember++;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
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

        System.out.println("    Listado de Socios");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.println("Ordenación por ID...");
                    arrayMembers.stream().sorted(Member::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.println("Ordenación por nombre...");
                    arrayMembers.stream().sorted(Comparator.comparing(Member::getName).thenComparing(Member::getSurname)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    arrayMembers = loadDataList(scan, currentUser, LibDBMember.getInstance());
                    System.out.println("Ordenación por apellidos...");
                    arrayMembers.stream().sorted(Comparator.comparing(Member::getSurname).thenComparing(Member::getName)).forEach(System.out::println);
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
            System.out.println("    Buscador de Socios");
            do {
                System.out.println("\nSelecciona criterio de búsqueda -\n" + searchMenu);

                opt = checkOptionInput(scan);

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
                    case 3:
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
                    Member member = LibDBMember.getInstance().searchTB(currentUser, ID);
                    System.out.println(member);
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString);
                    members.forEach(System.out::println);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
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
            System.out.println("    Baja de Socios");
            do {
                System.out.println("\nSelecciona criterio de búsqueda -\n" + searchMenu);

                opt = checkOptionInput(scan);

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
                    case 3:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        fragString = scan.nextLine();
                        isValid = true;
                        break;
                    case 0:
                        System.out.println("  Volviendo al menú del gestor...");
                        return new int[]{nMember, idMember};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    idMember = LibDBMember.getInstance().deleteTB(currentUser, ID);
                    nMember--;
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString);
                    Set<Integer> idmembers = members.stream().map(Member::getIdMember).collect(Collectors.toSet());
                    members.stream().sorted(Member::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del socio a retirar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nMember, idMember};
                            } else if (!idmembers.add(ID)) {
                                idMember = LibDBMember.getInstance().deleteTB(currentUser, ID);
                                nMember--;
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

        return new int[]{nMember, idMember};
    }

}
