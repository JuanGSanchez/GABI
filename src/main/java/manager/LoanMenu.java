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

import static utils.Utils.*;

/**
 * Clase del menú del gestor de libros en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class LoanMenu extends EntityMenu {
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private final String mainMenu;
    /**
     * Variable para almacenar las opciones de submenús para préstamos
     */
    private final String searchMenu;
    /**
     * Variable para almacenar las opciones de submenús para socios
     */
    private final String searchMemberMenu;
    /**
     * Variable para almacenar las opciones de submenús para libros
     */
    private final String searchBookMenu;
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para préstamos
     */
    private final String[] searchVar;
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para socios
     */
    private final String[] searchMemberVar;
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para libros
     */
    private final String[] searchBookVar;
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
    LoanMenu(User currentUser, Properties configProps, ResourceBundle rb) {
        this.currentUser = currentUser;
        this.configProps = configProps;
        this.rb = rb;
        mainMenu = String.format("\n  %s:\n\t(1) " + rb.getString("program-loan-menu-1") +
                                 "\n\t(2) " + rb.getString("program-loan-menu-2") +
                                 "\n\t(3) " + rb.getString("program-loan-menu-3") +
                                 "\n\t(4) " + rb.getString("program-loan-menu-4") +
                                 "\n\t(0) %s",
                rb.getString("program-general-menu"), rb.getString("program-properties-field-3-singular").toLowerCase(),
                rb.getString("program-properties-field-3-plural").toLowerCase(),
                rb.getString("program-properties-field-3-plural").toLowerCase(),
                rb.getString("program-properties-field-3-singular").toLowerCase(),
                rb.getString("program-general-exit-menu"));
        searchMenu = String.format("\n\t(1) " + rb.getString("program-general-order-election") +
                                   "\n\t(2) " + rb.getString("program-general-order-election") +
                                   "\n\t(3) " + rb.getString("program-general-order-election") +
                                   "\n\t(4) " + rb.getString("program-general-order-election") +
                                   "\n\t(0) %s",
                rb.getString("program-loan-properties-1"),
                rb.getString("program-loan-properties-2"),
                rb.getString("program-loan-properties-3"),
                rb.getString("program-loan-properties-4"),
                rb.getString("program-general-exit-order"));
        searchMemberMenu = String.format("\n  %s -\n\t(1) " + rb.getString("program-general-order-election") +
                                         "\n\t(2) " + rb.getString("program-general-order-election") +
                                         "\n\t(3) " + rb.getString("program-general-order-election") +
                                         "\n\t(0) %s",
                rb.getString("program-general-order"), rb.getString("program-member-properties-1"),
                rb.getString("program-member-properties-2"),
                rb.getString("program-member-properties-3"),
                rb.getString("program-general-exit-order"));
        searchBookMenu = String.format("\n  %s -\n\t(1) " + rb.getString("program-general-order-election") +
                                       "\n\t(2) " + rb.getString("program-general-order-election") +
                                       "\n\t(3) " + rb.getString("program-general-order-election") +
                                       "\n\t(0) %s",
                rb.getString("program-general-order"), rb.getString("program-book-properties-1"),
                rb.getString("program-book-properties-2"),
                rb.getString("program-book-properties-3"),
                rb.getString("program-general-exit-order"));
        searchVar = new String[]{rb.getString("program-loan-properties-1"),
                rb.getString("program-loan-properties-2"),
                rb.getString("program-loan-properties-3"),
                rb.getString("program-loan-properties-4")};
        searchMemberVar = new String[]{rb.getString("program-member-properties-1"),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-member-properties-2")),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-member-properties-3"))};
        searchBookVar = new String[]{rb.getString("program-book-properties-1"),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-book-properties-2")),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-book-properties-3"))};
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
    @Override
    int[] selectionMenu(Scanner scan, int nLoan, int idLoan) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        System.out.println("\n\t" + String.format(rb.getString("program-intro-menu-seed"),
                rb.getString("program-properties-field-3-plural")).toUpperCase());
        do {
            System.out.println(mainMenu);

            optionMenu = checkOptionInput(scan);

            switch (optionMenu) {
                case 1:
                    count = addLoan(scan, nLoan, idLoan);
                    nLoan = count[0];
                    idLoan = count[1];
                    break;
                case 2:
                    if (nLoan == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-3-plural").toLowerCase());
                    } else {
                        listLoans(scan);
                        System.out.printf(rb.getString("program-general-total") + ": %d\n",
                                rb.getString("program-properties-field-3-plural").toLowerCase(), nLoan);
                    }
                    break;
                case 3:
                    if (nLoan == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-3-plural").toLowerCase());
                    } else {
                        searchLoans(scan);
                    }
                    break;
                case 4:
                    if (nLoan == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-3-plural").toLowerCase());
                    } else {
                        count = deleteLoan(scan, nLoan, idLoan);
                        nLoan = count[0];
                        idLoan = count[1];
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
        int opt;
        int ID;
        int idMember = 0;
        int idBook = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-add-2") + "\n\n",
                    rb.getString("program-properties-field-3-singular"));

            do {
                System.out.printf("%s -\n", rb.getString("program-loan-add-1"));
                do {
                    System.out.println(searchMemberMenu);

                    opt = checkOptionInput(scan);

                    Object o = checkCaseAdd(scan, opt, searchMemberVar);
                    if (o instanceof String) {
                        fragString = (String) o;
                        isValid = true;
                    } else if (o instanceof Integer) {
                        idMember = (Integer) o;
                        isValid = true;
                    } else if (o instanceof Double) {
                        return new int[]{nLoan, idLoan};
                    }

                } while (!isValid);

                try {
                    if (opt == 1) {
                        LibDBMember.getInstance().searchTB(currentUser, idMember, rb);
                    } else {
                        List<Member> members = LibDBMember.getInstance().searchTB(currentUser, opt, fragString, rb);
                        Set<Integer> idsocs = members.stream().map(Member::getID).collect(Collectors.toSet());
                        members.stream().sorted(Member::compareTo)
                                .forEach(member -> System.out.println(entityToString(member)));
                        do {
                            System.out.printf(rb.getString("program-general-enter") + "\n(%s) -\n",
                                    rb.getString("program-properties-field-2-singular").toLowerCase(),
                                    rb.getString("program-general-cancel"));
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                                            rb.getString("program-return-1").toLowerCase());
                                    return new int[]{nLoan, idLoan};
                                } else if (!idsocs.add(ID)) {
                                    idMember = ID;
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
                    isPossible = true;
                } catch (RuntimeException re) {
                    System.err.println(re.getMessage());
                    isPossible = false;
                }
            } while (!isPossible);

            do {
                System.out.printf("%s -\n", rb.getString("program-loan-add-2"));
                do {
                    System.out.println(searchBookMenu);

                    opt = checkOptionInput(scan);

                    Object o = checkCaseAdd(scan, opt, searchBookVar);
                    if (o instanceof String) {
                        fragString = (String) o;
                        isValid = true;
                    } else if (o instanceof Integer) {
                        idBook = (Integer) o;
                        isValid = true;
                    } else if (o instanceof Double) {
                        return new int[]{nLoan, idLoan};
                    }

                } while (!isValid);

                try {
                    if (opt == 1) {
                        LibDBBook.getInstance().searchTB(currentUser, idBook, rb);
                    } else {
                        List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString, rb);
                        Set<Integer> idlibs = books.stream().map(Book::getID).collect(Collectors.toSet());
                        books.stream().sorted(Book::compareTo)
                                .forEach(book -> System.out.println(entityToString(book)));
                        do {
                            System.out.printf(rb.getString("program-general-enter") + "\n(%s) -\n",
                                    rb.getString("program-properties-field-1-singular").toLowerCase(),
                                    rb.getString("program-general-cancel"));
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                                            rb.getString("program-return-1").toLowerCase());
                                    return new int[]{nLoan, idLoan};
                                } else if (!idlibs.add(ID)) {
                                    idBook = ID;
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
                    isPossible = true;
                } catch (RuntimeException re) {
                    System.err.println(re.getMessage());
                    isPossible = false;
                }
            } while (!isPossible);

            try {
                LibDBLoan.getInstance().addDb(currentUser, new Loan(idLoan + 1, idMember, idBook), rb);
                nLoan++;
                idLoan++;
            } catch (RuntimeException re) {
                System.err.printf("  %s: %s\n", rb.getString("program-error-database"), re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
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

        System.out.printf("    " + rb.getString("program-general-list") + "\n",
                rb.getString("program-properties-field-3-plural"));
        do {
            System.out.printf("\n  %s -%s\n", rb.getString("program-general-order"), searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayLoans = loadDataList(scan, currentUser, LibDBLoan.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-loan-properties-1"));
                    arrayLoans.stream().sorted(Loan::compareTo)
                            .forEach(loan -> System.out.println(entityToString(loan)));
                    isValid = true;
                    break;
                case 2:
                    arrayLoans = loadDataList(scan, currentUser, LibDBLoan.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-loan-properties-2"));
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getIdMember))
                            .forEach(loan -> System.out.println(entityToString(loan)));
                    isValid = true;
                    break;
                case 3:
                    arrayLoans = loadDataList(scan, currentUser, LibDBLoan.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-loan-properties-3"));
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getIdBook))
                            .forEach(loan -> System.out.println(entityToString(loan)));
                    isValid = true;
                    break;
                case 4:
                    arrayLoans = loadDataList(scan, currentUser, LibDBLoan.getInstance());
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-loan-properties-4"));
                    arrayLoans.stream().sorted(Comparator.comparing(Loan::getDateLoan).thenComparing(Loan::getID))
                            .forEach(loan -> System.out.println(entityToString(loan)));
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
        LocalDate date = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-search") + "\n",
                    rb.getString("program-properties-field-3-plural"));
            do {
                System.out.printf("\n  %s -%s\n", rb.getString("program-general-criteria"), searchMenu);

                opt = checkOptionInput(scan);

                Object o = checkCase(scan, opt);
                if (o instanceof Integer) {
                    ID = (Integer) o;
                    isValid = true;
                } else if (o instanceof LocalDate) {
                    date = (LocalDate) o;
                    isValid = true;
                } else if (o instanceof Double) {
                    return;
                }

            } while (!isValid);

            try {
                List<Loan> loans;
                if (opt < 4) {
                    loans = LibDBLoan.getInstance().searchTB(currentUser, opt, ID, rb);
                } else {
                    loans = LibDBLoan.getInstance().searchTB(currentUser, date, rb);
                }
                loans.forEach(loan -> System.out.println(entityToString(loan)));
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
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
        LocalDate date = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-delete-2") + "\n",
                    rb.getString("program-properties-field-3-singular"));
            do {
                System.out.printf("\n  %s -%s\n", rb.getString("program-general-criteria"), searchMenu);

                opt = checkOptionInput(scan);

                Object o = checkCase(scan, opt);
                if (o instanceof Integer) {
                    ID = (Integer) o;
                    isValid = true;
                } else if (o instanceof LocalDate) {
                    date = (LocalDate) o;
                    isValid = true;
                } else if (o instanceof Double) {
                    return new int[]{nLoan, idLoan};
                }

            } while (!isValid);

            try {
                if (opt == 1) {
                    idLoan = LibDBLoan.getInstance().deleteDB(currentUser, ID, rb);
                    nLoan--;
                } else {
                    List<Loan> loans;
                    if (opt == 2 || opt == 3) {
                        loans = LibDBLoan.getInstance().searchTB(currentUser, opt, ID, rb);
                    } else {
                        loans = LibDBLoan.getInstance().searchTB(currentUser, date, rb);
                    }
                    Set<Integer> idloans = loans.stream().map(Loan::getID).collect(Collectors.toSet());
                    loans.stream().sorted(Loan::compareTo)
                            .forEach(loan -> System.out.println(entityToString(loan)));
                    do {
                        System.out.printf(rb.getString("program-general-enter") + "\n(%s) -\n",
                                rb.getString("program-properties-field-3-singular").toLowerCase(),
                                rb.getString("program-general-cancel"));
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                                        rb.getString("program-return-1").toLowerCase());
                                return new int[]{nLoan, idLoan};
                            } else if (!idloans.add(ID)) {
                                idLoan = LibDBBook.getInstance().deleteDB(currentUser, ID, rb);
                                nLoan--;
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

        return new int[]{nLoan, idLoan};
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
            case 2:
            case 3:
                System.out.printf("%s %s -\n", rb.getString("program-general-intro"), searchVar[opt - 1]);
                obj = checkOptionInput(scan, String.format("  %s\n", rb.getString("program-error-entry")));
                break;
            case 4:
                boolean isDone = false;
                do {
                    System.out.printf("%s %s (%s) -", rb.getString("program-general-intro"), searchVar[opt - 1],
                            configProps.getProperty("database-table-3-field-4-format-text"));
                    obj = scan.nextLine();
                    try {
                        obj = LocalDate.parse((String) obj, DateTimeFormatter.ofPattern(configProps.getProperty("database-table-3-field-4-format-code")));
                        isDone = true;
                    } catch (RuntimeException re) {
                        System.err.printf("  %s\n", rb.getString("program-error-date"));
                    }
                } while (!isDone);
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

    /**
     * Método con la selección para
     * el criterio de búsqueda en las tablas de datos adicionales
     *
     * @param scan    Entrada de datos por teclado
     * @param opt     Índice del criterio de búsqueda
     * @param listVar Lista de nombres de los campos en la tabla
     * @return Resultado de la lectura de opt
     */
    private Object checkCaseAdd(Scanner scan, int opt, String[] listVar) {
        Object obj = null;

        switch (opt) {
            case 1:
                System.out.printf("%s %s -\n", rb.getString("program-general-intro"), listVar[opt - 1]);
                obj = checkOptionInput(scan, String.format("  %s\n", rb.getString("program-error-entry")));
                break;
            case 2:
            case 3:
                System.out.printf("%s %s -\n", rb.getString("program-general-intro"), listVar[opt - 1]);
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
