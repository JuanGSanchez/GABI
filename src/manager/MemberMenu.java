/**
 * Paquete de menús del programa
 */
package manager;

import sql.reservoirs.LibDBMember;
import tables.Member;

import java.util.*;
import java.util.stream.Collectors;

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
            \t(1) Alta Socio
            \t(2) Directorio Socios
            \t(3) Buscar Socios
            \t(4) Baja Socio
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
     * Nombre de usuario pasado para la base de datos
     */
    private final String user;
    /**
     * Contraseña de usuario para la base de datos
     */
    private final String password;

    /**
     * Constructor de la clase, restringido al paquete
     */
    MemberMenu(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Método del menú principal del gestor de members, desde el cual
     * se acceden a las acciones disponibles
     *
     * @param scan  Entrada de datos por teclado
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
            try {
                optionMenu = scan.nextInt();
            } catch (InputMismatchException ime) {
                optionMenu = -1;
            }
            scan.nextLine();
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
                        System.out.println("Total de members: " + nMember);
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
     * Método para dar de alta nuevos members en la base de datos
     *
     * @param scan  Entrada de datos por teclado
     * @param nMember  Número de members registrados dentro de la base de datos
     * @param idMember Máxima ID de members dentro de la base de datos
     * @return Valores actualizados de nMember y idMember
     */
    private int[] addMember(Scanner scan, int nMember, int idMember) {
        boolean isValid;
        boolean repeat;
        String name = null;
        String surname = null;

        do {
            isValid = false;
            System.out.println("\n    Alta de Nuevo Socio\n(-1 en cualquier momento para cancelar operación)\n");
            do {
                System.out.print("Introduce nombre - ");
                try {
                    name = scan.nextLine();
                    if ("".equals(name)) {
                        System.err.println("  Entrada vacía");
                    } else if (name.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nMember, idMember};
                    } else if (name.matches(".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (name.trim().length() > 20) {
                        System.err.println("  El nombre no puede superar los 20 caracteres");
                    } else {
                        name = name.trim();
                        name = name.substring(0, 1).toUpperCase() + name.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            isValid = false;
            do {
                System.out.print("Introduce apellidos - ");
                try {
                    surname = scan.nextLine();
                    if ("".equals(surname)) {
                        System.err.println("  Entrada vacía");
                    } else if (surname.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nMember, idMember};
                    } else if (surname.matches(".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  Los apellidos no pueden contener números o caracteres especiales");
                    } else if (surname.trim().length() > 40) {
                        System.err.println("  El título no puede superar los 40 caracteres");
                    } else {
                        surname = surname.trim();
                        surname = surname.substring(0, 1).toUpperCase() + surname.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                LibDBMember.getInstance().addTB(user, password, new Member(nMember + 1, name, surname));
                ++nMember;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nMember, idMember};
    }

    /**
     * Método para imprimir en pantalla los members registrados
     * en la base de datos
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
            try {
                opt = scan.nextInt();
            } catch (InputMismatchException ime) {
                opt = -1;
            }
            scan.nextLine();
            switch (opt) {
                case 1:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayMembers = LibDBMember.getInstance().searchDetailTB(user, password);
                    } else {
                        arrayMembers = LibDBMember.getInstance().searchTB(user, password);
                    }
                    System.out.println("Ordenación por ID...");
                    arrayMembers.stream().sorted(Member::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayMembers = LibDBMember.getInstance().searchDetailTB(user, password);
                    } else {
                        arrayMembers = LibDBMember.getInstance().searchTB(user, password);
                    }
                    System.out.println("Ordenación por nombre...");
                    arrayMembers.stream().sorted(Comparator.comparing(Member::getName).thenComparing(Member::getSurname)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayMembers = LibDBMember.getInstance().searchDetailTB(user, password);
                    } else {
                        arrayMembers = LibDBMember.getInstance().searchTB(user, password);
                    }
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
     * Método para buscar members en la base de datos según
     * ciertos criterios
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
                    Member member = LibDBMember.getInstance().searchTB(user, password, ID);
                    System.out.println(member);
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(user, password, opt, fragString);
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
     * Método para dar de baja un member de la base de datos
     *
     * @param scan  Entrada de datos por teclado
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
                    idMember = LibDBMember.getInstance().deleteTB(user, password, ID);
                    nMember--;
                } else {
                    List<Member> members = LibDBMember.getInstance().searchTB(user, password, opt, fragString);
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
                                idMember = LibDBMember.getInstance().deleteTB(user, password, ID);
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