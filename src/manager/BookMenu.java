/**
 * Paquete de menús del programa
 */
package manager;

import java.util.*;
import java.util.stream.Collectors;

import sql.reservoirs.LibDBBook;
import tables.Book;
import tables.User;
import utils.Utils;

/**
 * Clase del menú del gestor de books en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class BookMenu {
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta
     */
    private static final String[] searchVar = {"ID", "título o fragmento", "autor o fragmento"};
    /**
     * Método para almacenar aparte el texto del menú principal
     */
    private static final String mainMenu = """
              
              Seleccione una acción:
            \t(1) Añadir libro
            \t(2) Listar libros
            \t(3) Buscar libros
            \t(4) Eliminar libro
            \t(0) Volver al menú principal""";
    /**
     * Variable para almacenar las opciones de submenús
     */
    private static final String searchMenu = """
            \t(1) Por ID
            \t(2) Por título
            \t(3) Por autor
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
    BookMenu(User currentUser, Properties configProps) {
        this.currentUser = currentUser;
        this.configProps = configProps;
    }

    /**
     * Método del menú principal del gestor de books, desde el cual se acceden
     * a las acciones disponibles
     *
     * @param scan   Entrada de datos por teclado
     * @param nBook  Número de books guardados dentro de la base de datos
     * @param idBook Máxima ID de books dentro de la base de datos
     * @return Valores actualizados de nBook y idBook
     */
    int[] selectionMenu(Scanner scan, int nBook, int idBook) {
        boolean checkMenu = true;
        int optionMenu;
        int[] count;

        System.out.println("\n\tGESTOR LIBROS");
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
                    count = addBook(scan, nBook, idBook);
                    nBook = count[0];
                    idBook = count[1];
                    break;
                case 2:
                    if (nBook == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        listBooks(scan);
                        System.out.println("Total de libros: " + nBook);
                    }
                    break;
                case 3:
                    if (nBook == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        searchBooks(scan);
                    }
                    break;
                case 4:
                    if (nBook == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        count = deleteBook(scan, nBook, idBook);
                        nBook = count[0];
                        idBook = count[1];
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
            System.out.println("\n    Alta de Nuevo Libro\n(-1 en cualquier momento para cancelar operación)\n");

            title = Utils.checkString(scan, 0, configProps.getProperty("database-table-1-field-2-maxchar"));
            if (title == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nBook, idBook};
            }

            author = Utils.checkString(scan, 1, configProps.getProperty("database-table-1-field-3-maxchar"));
            if (author == null) {
                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                return new int[]{nBook, idBook};
            }

            try {
                LibDBBook.getInstance().addTB(currentUser, new Book(idBook + 1, title, author));
                nBook++;
                idBook++;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
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
        List<Book> arrayBooks = new ArrayList<>();

        System.out.println("    Listado de Libros");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);
            try {
                opt = scan.nextInt();
                arrayBooks = LibDBBook.getInstance().searchTB(currentUser);
            } catch (InputMismatchException ime) {
                opt = -1;
            }
            scan.nextLine();
            switch (opt) {
                case 1:
                    System.out.println("Ordenación por ID...");
                    arrayBooks.stream().sorted(Book::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("Ordenación por título...");
                    arrayBooks.stream().sorted(Comparator.comparing(Book::getTitle).thenComparingInt(Book::getIdBook)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("Ordenación por autor...");
                    arrayBooks.stream().sorted(Comparator.comparing(Book::getAuthor).thenComparing(Book::getTitle)).forEach(System.out::println);
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
     * Método para buscar books en la base de datos
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
            System.out.println("    Buscador de Libros");
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
                    Book book = LibDBBook.getInstance().searchTB(currentUser, ID);
                    System.out.println(book);
                } else {
                    List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString);
                    books.forEach(System.out::println);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para eliminar libros de la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nBook  Número de books guardados dentro de la base de datos
     * @param idBook Máxima ID de books dentro de la base de datos
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
            System.out.println("    Baja de Libros");
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
                        return new int[]{nBook, idBook};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    idBook = LibDBBook.getInstance().deleteTB(currentUser, ID);
                    nBook--;
                } else {
                    List<Book> books = LibDBBook.getInstance().searchTB(currentUser, opt, fragString);
                    Set<Integer> idbooks = books.stream().map(Book::getIdBook).collect(Collectors.toSet());
                    books.stream().sorted(Book::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del libro a eliminar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nBook, idBook};
                            } else if (!idbooks.add(ID)) {
                                idBook = LibDBBook.getInstance().deleteTB(currentUser, ID);
                                nBook--;
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

        return new int[]{nBook, idBook};
    }

}
