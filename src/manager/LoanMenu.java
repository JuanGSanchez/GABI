/**
 * Paquete de menús del programa
 */
package manager;

import sql.reservoirs.LibDBBook;
import sql.reservoirs.LibDBLoan;
import sql.reservoirs.LibDBMember;
import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase del menú del gestor de libros en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class LoanMenu {
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para libros
     */
    private static final String[] searchBookVar = {"ID", "título o fragmento", "autor o fragmento"};
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para socios
     */
    private static final String[] searchMemberVar = {"ID", "nombre o fragmento", "apellido o fragmento"};
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para préstamos
     */
    private static final String[] searchVar = {"ID de Préstamo", "ID de Socio", "ID de Libro", "fecha de realización"};
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
                          
              Seleccione una acción:
            \t(1) Nuevo préstamo
            \t(2) Listar préstamos
            \t(3) Buscar préstamos
            \t(4) Retirar préstamo
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private static final String searchBookMenu = """
            \t(1) Por ID
            \t(2) Por título
            \t(3) Por autor
            \t(0) Salir""";
    /**
     * Variable para almacenar las opciones de submenús para socios
     */
    private static final String searchMemberMenu = """
            \t(1) Por ID
            \t(2) Por nombre
            \t(3) Por apellidos
            \t(0) Salir""";
    /**
     * Variable para almacenar las opciones de submenús para préstamos
     */
    private static final String searchMenu = """
            \t(1) Por ID de Préstamos
            \t(2) Por ID de Socio
            \t(3) Por ID de Libro
            \t(4) Por fecha de realización
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
    LoanMenu(User currentUser, Properties configProps) {
        this.currentUser = currentUser;
        this.configProps = configProps;
    }

    /**
     * Método del menú principal del gestor de Préstamos, desde el cual
     * se acceden a las acciones disponibles
     *
     * @param scan   Entrada de datos por teclado
     * @param nLoan  Número de préstamos en activo dentro de la base de datos
     * @param idLoan Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nLoan y idLoan
     */
    int[] selectionMenu(Scanner scan, int nLoan, int idLoan) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        System.out.println("\n\tGESTOR PRESTAMOS");
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
                    count = addLoan(scan, nLoan, idLoan);
                    nLoan = count[0];
                    idLoan = count[1];
                    break;
                case 2:
                    if (nLoan == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        listLoans(scan);
                        System.out.println("Total de préstamos activos: " + nLoan);
                    }
                    break;
                case 3:
                    if (nLoan == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        searchLoans(scan);
                    }
                    break;
                case 4:
                    if (nLoan == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        count = deleteLoan(scan, nLoan, idLoan);
                        nLoan = count[0];
                        idLoan = count[1];
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

        return new int[]{nLoan, idLoan};
    }

    /**
     * Método para registrar nuevos préstamos en la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nLoan  Número de préstamos en activo dentro de la base de datos
     * @param idLoan Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nLoan y idLoan
     */
    private int[] addLoan(Scanner scan, int nLoan, int idLoan) {
        boolean isValid;
        boolean isPossible;
        boolean repeat;
        int idSoc = 0;
        int idLib = 0;
        int opt;
        int ID;
        String fragString = null;

        do {
            isValid = false;
            System.out.println("\n    Nuevo Préstamo\n");

            do {
                System.out.println("Introduce socio receptor del préstamo - ");
                do {
                    System.out.println("\n  Selecciona criterio de búsqueda:\n" + searchMemberMenu);
                    try {
                        opt = scan.nextInt();
                    } catch (InputMismatchException ime) {
                        opt = -1;
                    }
                    scan.nextLine();
                    switch (opt) {
                        case 1:
                            System.out.println("Introduce " + searchMemberVar[opt - 1] + " -");
                            try {
                                idSoc = scan.nextInt();
                                isValid = true;
                            } catch (InputMismatchException ime) {
                                System.err.println("  Entrada no válida");
                            }
                            scan.nextLine();
                            break;
                        case 2:
                        case 3:
                            System.out.println("Introduce " + searchMemberVar[opt - 1] + " -");
                            fragString = scan.nextLine();
                            isValid = true;
                            break;
                        case 0:
                            System.out.println("  Volviendo al menú del gestor...");
                            return new int[]{nLoan, idLoan};
                        default:
                            System.err.println("  Entrada no válida");
                    }
                } while (!isValid);

                try {
                    if (opt != 1) {
                        List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString);
                        Set<Integer> idsocs = members.stream().map(Member::getIdMember).collect(Collectors.toSet());
                        members.stream().sorted(Member::compareTo).forEach(System.out::println);
                        do {
                            System.out.println("Introduce ID del member de la lista anterior\n" +
                                               "(-1 para cancelar operación):");
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                    return new int[]{nLoan, idLoan};
                                } else if (!idsocs.add(ID)) {
                                    idSoc = ID;
                                    isValid = false;
                                } else {
                                    System.err.println("El ID proporcionado no se encuentra en la lista");
                                }
                            } catch (InputMismatchException ime) {
                                System.err.println("Entrada no válida");
                            }
                            scan.nextLine();
                        } while (isValid);
                    } else {
                        LibDBMember.getInstance().searchTB(currentUser, idSoc);
                    }
                    isPossible = true;
                } catch (RuntimeException re) {
                    System.err.println(re.getMessage());
                    isPossible = false;
                }
            } while (!isPossible);

            do {
                System.out.println("Introduce libro a ser prestado - ");
                do {
                    System.out.println("\nSelecciona criterio de búsqueda:\n" + searchBookMenu);
                    try {
                        opt = scan.nextInt();
                    } catch (InputMismatchException ime) {
                        opt = -1;
                    }
                    scan.nextLine();
                    switch (opt) {
                        case 1:
                            System.out.println("Introduce " + searchBookVar[opt - 1] + " -");
                            try {
                                idLib = scan.nextInt();
                                isValid = true;
                            } catch (InputMismatchException ime) {
                                System.err.println("  Entrada no válida");
                            }
                            scan.nextLine();
                            break;
                        case 2:
                        case 3:
                            System.out.println("Introduce " + searchBookVar[opt - 1] + " -");
                            fragString = scan.nextLine();
                            isValid = true;
                            break;
                        case 0:
                            System.out.println("  Volviendo al menú del gestor...");
                            return new int[]{nLoan, idLoan};
                        default:
                            System.err.println("  Entrada no válida");
                    }
                } while (!isValid);

                try {
                    if (opt != 1) {
                        List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString);
                        Set<Integer> idlibs = books.stream().map(Book::getIdBook).collect(Collectors.toSet());
                        books.stream().sorted(Book::compareTo).forEach(System.out::println);
                        do {
                            System.out.println("Introduce ID del book de la lista anterior\n" +
                                               "(-1 para cancelar operación):");
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                    return new int[]{nLoan, idLoan};
                                } else if (!idlibs.add(ID)) {
                                    idLib = ID;
                                    isValid = false;
                                } else {
                                    System.err.println("El ID proporcionado no se encuentra en la lista");
                                }
                            } catch (InputMismatchException ime) {
                                System.err.println("Entrada no válida");
                            }
                            scan.nextLine();
                        } while (isValid);
                    } else {
                        LibDBBook.getInstance().searchTB(currentUser, idLib);
                    }
                    isPossible = true;
                } catch (RuntimeException re) {
                    System.err.println(re.getMessage());
                    isPossible = false;
                }
            } while (!isPossible);

            try {
                LibDBLoan.getInstance().addTB(currentUser, new Loan(nLoan + 1, idSoc, idLib));
                ++nLoan;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nLoan, idLoan};
    }

    /**
     * Método para imprimir en pantalla los préstamos
     * registrados al momento en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listLoans(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Loan> arrayLoans;

        System.out.println("    Listado de Préstamos");
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
                        arrayLoans = LibDBLoan.getInstance().searchDetailTB(currentUser);
                    } else {
                        arrayLoans = LibDBLoan.getInstance().searchTB(currentUser);
                    }
                    System.out.println("Ordenación por ID del préstamo...");
                    arrayLoans.stream().sorted(Loan::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayLoans = LibDBLoan.getInstance().searchDetailTB(currentUser);
                    } else {
                        arrayLoans = LibDBLoan.getInstance().searchTB(currentUser);
                    }
                    System.out.println("Ordenación por ID del socio...");
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getIdMember)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayLoans = LibDBLoan.getInstance().searchDetailTB(currentUser);
                    } else {
                        arrayLoans = LibDBLoan.getInstance().searchTB(currentUser);
                    }
                    System.out.println("Ordenación por ID del libro...");
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getIdBook)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 4:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayLoans = LibDBLoan.getInstance().searchDetailTB(currentUser);
                    } else {
                        arrayLoans = LibDBLoan.getInstance().searchTB(currentUser);
                    }
                    System.out.println("Ordenación por fecha...");
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getDateLoan)).forEach(System.out::println);
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
     * Método para buscar préstamos en la base de datos
     * según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchLoans(Scanner scan) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString;
        LocalDate date = null;

        do {
            isValid = false;
            System.out.println("    Buscador de Préstamos");
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
                    case 2:
                    case 3:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        try {
                            ID = scan.nextInt();
                            isValid = true;
                        } catch (InputMismatchException ime) {
                            System.err.println("  Entrada no válida");
                        }
                        scan.nextLine();
                        break;
                    case 4:
                        boolean isDone = false;
                        do {
                            System.out.println("Introduce " + searchVar[opt - 1] + " (dd-mm-aaaa) -");
                            fragString = scan.nextLine();
                            try {
                                date = LocalDate.parse(fragString, DateTimeFormatter.ofPattern("dd-MM-yyyy"));
                                isDone = true;
                            } catch (RuntimeException re) {
                                System.err.println("  Formato de fecha no válido");
                            }
                        } while (!isDone);
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
                List<Loan> loans;
                if (opt < 4) {
                    loans = LibDBLoan.getInstance().searchTB(currentUser, opt, ID);
                } else {
                    loans = LibDBLoan.getInstance().searchTB(currentUser, date);
                }
                loans.forEach(System.out::println);
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para retirar préstamos resueltos de la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nLoan  Número de préstamos en activo dentro de la base de datos
     * @param idLoan Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nLoan y idLoan
     */
    private int[] deleteLoan(Scanner scan, int nLoan, int idLoan) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString;
        LocalDate date = null;

        do {
            isValid = false;
            System.out.println("    Devolución de Préstamos");
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
                    case 2:
                    case 3:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        try {
                            ID = scan.nextInt();
                            isValid = true;
                        } catch (InputMismatchException ime) {
                            System.err.println("  Entrada no válida");
                        }
                        scan.nextLine();
                        break;
                    case 4:
                        boolean isDone = false;
                        do {
                            System.out.println("Introduce " + searchVar[opt - 1] + " (" + configProps.getProperty("database-table-3-field-4-format-text") + ") -");
                            fragString = scan.nextLine();
                            try {
                                date = LocalDate.parse(fragString, DateTimeFormatter.ofPattern(configProps.getProperty("database-table-3-field-4-format-code")));
                                isDone = true;
                            } catch (RuntimeException re) {
                                System.err.println("  Formato de fecha no válido");
                            }
                        } while (!isDone);
                        isValid = true;
                        break;
                    case 0:
                        System.out.println("  Volviendo al menú del gestor...");
                        return new int[]{nLoan, idLoan};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                List<Loan> loans;
                Set<Integer> idloans;
                if (opt == 1) {
                    idLoan = LibDBLoan.getInstance().deleteTB(currentUser, ID);
                    nLoan--;
                } else {
                    if (opt == 2 || opt == 3) {
                        loans = LibDBLoan.getInstance().searchTB(currentUser, opt, ID);
                    } else {
                        loans = LibDBLoan.getInstance().searchTB(currentUser, date);
                    }
                    idloans = loans.stream().map(Loan::getIdLoan).collect(Collectors.toSet());
                    loans.stream().sorted(Loan::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del préstamo a eliminar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nLoan, idLoan};
                            } else if (!idloans.add(ID)) {
                                idLoan = LibDBBook.getInstance().deleteTB(currentUser, ID);
                                nLoan--;
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

        return new int[]{nLoan, idLoan};
    }

}
