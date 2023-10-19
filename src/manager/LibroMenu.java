/**
 * Paquete de menús del programa
 */
package manager;

import java.util.*;
import java.util.stream.Collectors;

import sql.reservoirs.BiblioDBLibro;
import tables.Libro;

/**
 * Clase del menú del gestor de libros en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class LibroMenu {
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
    LibroMenu(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Método para añadir libros a la base de datos
     *
     * @param scan  Entrada de datos por teclado
     * @param nLib  Número de libros guardados dentro de la base de datos
     * @param idLib Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nLib y idLib
     */
    private int[] addLibro(Scanner scan, int nLib, int idLib) {
        boolean isValid;
        boolean repeat;
        String titulo = null;
        String autor = null;

        do {
            isValid = false;
            System.out.println("\n    Alta de Nuevo Libro\n(-1 en cualquier momento para cancelar operación)\n");
            do {
                System.out.print("Introduce título - ");
                try {
                    titulo = scan.nextLine();
                    if ("".equals(titulo)) {
                        System.err.println("  Entrada vacía");
                    } else if (titulo.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nLib, idLib};
                    } else if (titulo.matches(".*[@#$%&ºª'`*_+=|/<>{}\\[\\]~].*")) {
                        System.err.println("  El título no puede contener caracteres especiales");
                    } else if (titulo.trim().length() > 120) {
                        System.err.println("  El título no puede superar los 120 caracteres");
                    } else {
                        titulo = titulo.trim();
                        titulo = titulo.substring(0, 1).toUpperCase() + titulo.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            isValid = false;
            do {
                System.out.print("Introduce nombre de autor principal (nombre[, apellidos] - ");
                try {
                    autor = scan.nextLine();
                    if ("".equals(autor)) {
                        System.err.println("  Entrada vacía");
                    } else if (autor.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nLib, idLib};
                    } else if (autor.matches(".*[\\d¡!@#$%&ºª'`*.:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (autor.trim().length() > 60) {
                        System.err.println("  El nombre no puede superar los 60 caracteres");
                    } else if (autor.matches(".*,.*")) {
                        autor = autor.trim();
                        String[] autorArray = autor.split(",");
                        if (autorArray.length == 2) {
                            for (int i = 0; i < autorArray.length; i++) {
                                autorArray[i] = autorArray[i].trim();
                                autorArray[i] = autorArray[i].substring(0, 1).toUpperCase() + autorArray[i].substring(1).toLowerCase();
                            }
                            autor = String.format("%s %s", autorArray[0], autorArray[1]);
                            isValid = true;
                        } else {
                            System.err.println("  El nombre no sigue la estructura adecuada");
                        }
                    } else {
                        autor = autor.trim();
                        autor = autor.substring(0, 1).toUpperCase() + autor.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                BiblioDBLibro.getInstance().addTB(user, password, new Libro(idLib + 1, titulo, autor));
                nLib++;
                idLib++;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nLib, idLib};

    }

    /**
     * Método para imprimir en pantalla los libros registrados en la base de
     * datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listLibros(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Libro> arrayLibros = new ArrayList<>();

        System.out.println("    Listado de Libros");
        do {
            System.out.println("\nSelecciona ordenación de listado -\n" + searchMenu);
            try {
                opt = scan.nextInt();
                arrayLibros = BiblioDBLibro.getInstance().searchTB(user, password);
            } catch (InputMismatchException ime) {
                opt = -1;
            }
            scan.nextLine();
            switch (opt) {
                case 1:
                    System.out.println("Ordenación por ID...");
                    arrayLibros.stream().sorted(Libro::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("Ordenación por título...");
                    arrayLibros.stream().sorted(Comparator.comparing(Libro::getTitulo).thenComparingInt(Libro::getIdLib)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("Ordenación por autor...");
                    arrayLibros.stream().sorted(Comparator.comparing(Libro::getAutor).thenComparing(Libro::getTitulo)).forEach(System.out::println);
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
     * Método para buscar libros en la base de datos según ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchLibros(Scanner scan) {
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
                    Libro libro = BiblioDBLibro.getInstance().searchTB(user, password, ID);
                    System.out.println(libro);
                } else {
                    List<Libro> libros = BiblioDBLibro.getInstance().searchTB(user, password, opt, fragString);
                    libros.forEach(System.out::println);
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
     * @param scan  Entrada de datos por teclado
     * @param nLib  Número de libros guardados dentro de la base de datos
     * @param idLib Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nLib y idLib
     */
    private int[] deleteLibro(Scanner scan, int nLib, int idLib) {
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
                        return new int[]{nLib, idLib};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    idLib = BiblioDBLibro.getInstance().deleteTB(user, password, ID);
                    nLib--;
                } else {
                    List<Libro> libros = BiblioDBLibro.getInstance().searchTB(user, password, opt, fragString);
                    Set<Integer> idlibs = libros.stream().map(Libro::getIdLib).collect(Collectors.toSet());
                    libros.stream().sorted(Libro::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del libro a eliminar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nLib, idLib};
                            } else if (!idlibs.add(ID)) {
                                idLib = BiblioDBLibro.getInstance().deleteTB(user, password, ID);
                                nLib--;
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

        return new int[]{nLib, idLib};
    }

    /**
     * Método del menú principal del gestor de libros, desde el cual se acceden
     * a las acciones disponibles
     *
     * @param scan  Entrada de datos por teclado
     * @param nLib  Número de libros guardados dentro de la base de datos
     * @param idLib Máxima ID de libros dentro de la base de datos
     * @return Valores actualizados de nLib y idLib
     */
    int[] seleccionMenu(Scanner scan, int nLib, int idLib) {
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
                    count = addLibro(scan, nLib, idLib);
                    nLib = count[0];
                    idLib = count[1];
                    break;
                case 2:
                    if (nLib == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        listLibros(scan);
                        System.out.println("Total de libros: " + nLib);
                    }
                    break;
                case 3:
                    if (nLib == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        searchLibros(scan);
                    }
                    break;
                case 4:
                    if (nLib == 0) {
                        System.err.println("Error, no hay lista de libros disponible");
                    } else {
                        count = deleteLibro(scan, nLib, idLib);
                        nLib = count[0];
                        idLib = count[1];
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

        return new int[]{nLib, idLib};
    }

}
