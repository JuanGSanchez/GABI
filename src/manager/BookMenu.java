/**
 * Paquete de menús del programa
 */
package manager;

import sql.reservoirs.LibDBBook;
import tables.Book;
import tables.User;

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
final class BookMenu extends EntityMenu {
    /**
     * Variable para almacenar aparte el texto del menú principal
     */
    private final String mainMenu;
    /**
     * Variable para almacenar aparte las opciones de submenús
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
    BookMenu(User currentUser, Properties configProps, ResourceBundle rb) {
        this.currentUser = currentUser;
        this.configProps = configProps;
        this.rb = rb;
        mainMenu = String.format("\n  %s:\n\t(1) " + rb.getString("program-book-menu-1") +
                                 "\n\t(2) " + rb.getString("program-book-menu-2") +
                                 "\n\t(3) " + rb.getString("program-book-menu-3") +
                                 "\n\t(4) " + rb.getString("program-book-menu-4") +
                                 "\n\t(0) %s",
                rb.getString("program-general-menu"), rb.getString("program-properties-field-1-singular").toLowerCase(),
                rb.getString("program-properties-field-1-plural").toLowerCase(),
                rb.getString("program-properties-field-1-plural").toLowerCase(),
                rb.getString("program-properties-field-1-singular").toLowerCase(),
                rb.getString("program-general-exit-menu"));
        searchMenu = String.format("\n\t(1) " + rb.getString("program-general-order-election") +
                                   "\n\t(2) " + rb.getString("program-general-order-election") +
                                   "\n\t(3) " + rb.getString("program-general-order-election") +
                                   "\n\t(0) %s",
                rb.getString("program-book-properties-1"),
                rb.getString("program-book-properties-2"),
                rb.getString("program-book-properties-3"),
                rb.getString("program-general-exit-order"));
        searchVar = new String[]{rb.getString("program-book-properties-1"),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-book-properties-2")),
                String.format(rb.getString("program-general-fragment"), rb.getString("program-book-properties-3"))};
    }

    /**
     * Método del menú principal del gestor de libros, desde el cual se acceden
     * a las acciones disponibles
     *
     * @param scan   Entrada de datos por teclado
     * @param nBook  Número de libros guardados dentro de la base de datos
     * @param idBook Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nBook y idBook
     */
    @Override
    int[] selectionMenu(Scanner scan, int nBook, int idBook) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        System.out.println("\n\t" + String.format(rb.getString("program-intro-menu-seed"),
                rb.getString("program-properties-field-1-plural")).toUpperCase());
        do {
            System.out.println(mainMenu);

            optionMenu = checkOptionInput(scan);

            switch (optionMenu) {
                case 1:
                    count = addBook(scan, nBook, idBook);
                    nBook = count[0];
                    idBook = count[1];
                    break;
                case 2:
                    if (nBook == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-1-plural").toLowerCase());
                    } else {
                        listBooks(scan);
                        System.out.printf(rb.getString("program-general-total") + ": %d\n",
                                rb.getString("program-properties-field-1-plural").toLowerCase(), nBook);
                    }
                    break;
                case 3:
                    if (nBook == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-1-plural").toLowerCase());
                    } else {
                        searchBooks(scan);
                    }
                    break;
                case 4:
                    if (nBook == 0) {
                        System.err.printf(rb.getString("program-error-avail") + "\n",
                                rb.getString("program-properties-field-1-plural").toLowerCase());
                    } else {
                        count = deleteBook(scan, nBook, idBook);
                        nBook = count[0];
                        idBook = count[1];
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

        return new int[]{nBook, idBook};
    }

    /**
     * Método para añadir libros a la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nBook  Número de libros guardados dentro de la base de datos
     * @param idBook Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nBook y idBook
     */
    private int[] addBook(Scanner scan, int nBook, int idBook) {
        boolean repeat;
        String title;
        String author;

        do {
            System.out.printf("    " + rb.getString("program-general-add-1") + "\n(%s)\n\n",
                    rb.getString("program-properties-field-1-singular"), rb.getString("program-general-cancel"));

            title = checkString(scan, 0, configProps.getProperty("database-table-1-field-2-maxchar"));
            if (title == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nBook, idBook};
            }

            author = checkString(scan, 1, configProps.getProperty("database-table-1-field-3-maxchar"));
            if (author == null) {
                System.out.printf("  %s, %s...\n", rb.getString("program-return-2"),
                        rb.getString("program-return-1").toLowerCase());
                return new int[]{nBook, idBook};
            }

            try {
                LibDBBook.getInstance().addDb(currentUser, new Book(idBook + 1, title, author), rb);
                nBook++;
                idBook++;
            } catch (RuntimeException re) {
                System.err.printf("  %s: %s\n", rb.getString("program-error-database"), re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nBook, idBook};
    }

    /**
     * Método para imprimir en pantalla los libros
     * registrados en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listBooks(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Book> arrayBooks;

        System.out.printf("    " + rb.getString("program-general-list") + "\n",
                rb.getString("program-properties-field-1-plural"));
        do {
            System.out.printf("\n  %s -%s\n", rb.getString("program-general-order"), searchMenu);

            opt = checkOptionInput(scan);

            switch (opt) {
                case 1:
                    arrayBooks = LibDBBook.getInstance().searchDB(currentUser, rb);
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-book-properties-1"));
                    arrayBooks.stream().sorted(Book::compareTo)
                            .forEach(book -> System.out.println(entityToString(book)));
                    isValid = true;
                    break;
                case 2:
                    arrayBooks = LibDBBook.getInstance().searchDB(currentUser, rb);
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-book-properties-2"));
                    arrayBooks.stream().sorted(Comparator.comparing(Book::getTitle).thenComparingInt(Book::getID))
                            .forEach(book -> System.out.println(entityToString(book)));
                    isValid = true;
                    break;
                case 3:
                    arrayBooks = LibDBBook.getInstance().searchDB(currentUser, rb);
                    System.out.printf(rb.getString("program-general-order-selection") + "...\n",
                            rb.getString("program-book-properties-3"));
                    arrayBooks.stream().sorted(Comparator.comparing(Book::getAuthor).thenComparing(Book::getTitle))
                            .forEach(book -> System.out.println(entityToString(book)));
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
     * Método para buscar libros en la base de datos
     * según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchBooks(Scanner scan) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-search") + "\n",
                    rb.getString("program-properties-field-1-plural"));
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
                    Book book = LibDBBook.getInstance().searchTB(currentUser, ID, rb);
                    System.out.println(entityToString(book));
                } else {
                    List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString, rb);
                    books.forEach(book -> System.out.println(entityToString(book)));
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.printf("\n%s - \n", rb.getString("program-general-repeat"));
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para eliminar libros de la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nBook  Número de libros guardados dentro de la base de datos
     * @param idBook Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nBook y idBook
     */
    private int[] deleteBook(Scanner scan, int nBook, int idBook) {
        boolean isValid;
        boolean repeat;
        int opt;
        int ID = 0;
        String fragString = null;

        do {
            isValid = false;
            System.out.printf("    " + rb.getString("program-general-delete-1") + "\n",
                    rb.getString("program-properties-field-1-singular"));
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
                    return new int[]{nBook, idBook};
                }

            } while (!isValid);

            try {
                if (opt == 1) {
                    idBook = LibDBBook.getInstance().deleteDB(currentUser, ID, rb);
                    nBook--;
                } else {
                    List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString, rb);
                    Set<Integer> idbooks = books.stream().map(Book::getID).collect(Collectors.toSet());
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
                                return new int[]{nBook, idBook};
                            } else if (!idbooks.add(ID)) {
                                idBook = LibDBBook.getInstance().deleteDB(currentUser, ID, rb);
                                nBook--;
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

        return new int[]{nBook, idBook};
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
