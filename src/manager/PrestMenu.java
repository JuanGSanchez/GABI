/**
 * Paquete de menús del programa
 */
package manager;

import sql.BiblioDBLibro;
import sql.BiblioDBPrestamo;
import sql.BiblioDBSocio;
import tables.Libro;
import tables.Prestamo;
import tables.Socio;

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
final class PrestMenu {
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para libros
     */
    private static final String[] searchLibroVar = {"ID", "título o fragmento", "autor o fragmento"};
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para socios
     */
    private static final String[] searchSocioVar = {"ID", "nombre o fragmento", "apellido o fragmento"};
    /**
     * Lista con fragmentos de texto según el parámetro usado en la consulta para préstamos
     */
    private static final String[] searchVar = {"ID de Préstamo", "ID de Libro", "ID de Socio", "fecha de realización"};
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
    private static final String searchLibroMenu = """
            \t(1) Por ID
            \t(2) Por título
            \t(3) Por autor
            \t(0) Salir""";
    /**
     * Variable para almacenar las opciones de submenús para socios
     */
    private static final String searchSocioMenu = """
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
     * Constructor privado de la clase para evitar instancias
     */
    private PrestMenu() {
    }

    /**
     * Método del menú principal del gestor de Préstamos, desde el cual
     * se acceden a las acciones disponibles
     *
     * @param scan   Entrada de datos por teclado
     * @param nPres  Número de préstamos en activo dentro de la base de datos
     * @param idPres Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nPres y idPres
     */
    static int[] seleccionMenu(Scanner scan, int nPres, int idPres) {
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
            switch (optionMenu) {
                case 1:
                    scan.nextLine();
                    count = addPrestamo(scan, nPres, idPres);
                    nPres = count[0];
                    idPres = count[1];
                    break;
                case 2:
                    scan.nextLine();
                    if (nPres == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        listPrestamos(scan);
                        System.out.println("Total de préstamos activos: " + nPres);
                    }
                    break;
                case 3:
                    scan.nextLine();
                    if (nPres == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        searchPrestamos(scan);
                    }
                    break;
                case 4:
                    scan.nextLine();
                    if (nPres == 0) {
                        System.err.println("Error, no hay lista de préstamos disponible");
                    } else {
                        count = deletePrestamo(scan, nPres, idPres);
                        nPres = count[0];
                        idPres = count[1];
                    }
                    break;
                case 0:
                    scan.nextLine();
                    System.out.println("Volviendo al menú principal...");
                    checkMenu = false;
                    break;
                default:
                    System.err.println("Entrada no válida");
                    scan.nextLine();
            }
        } while (checkMenu);

        return new int[]{nPres, idPres};
    }

    /**
     * Método para registrar nuevos préstamos en la base de datos
     *
     * @param scan   Entrada de datos por teclado
     * @param nPres  Número de préstamos en activo dentro de la base de datos
     * @param idPres Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nPres y idPres
     */
    private static int[] addPrestamo(Scanner scan, int nPres, int idPres) {
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
                    System.out.println("\n  Selecciona criterio de búsqueda:\n" + searchSocioMenu);
                    try {
                        opt = scan.nextInt();
                    } catch (InputMismatchException ime) {
                        opt = -1;
                    }
                    scan.nextLine();
                    switch (opt) {
                        case 1:
                            System.out.println("Introduce " + searchSocioVar[opt - 1] + " -");
                            idSoc = scan.nextInt();
                            scan.nextLine();
                            isValid = true;
                            break;
                        case 2:
                        case 3:
                            System.out.println("Introduce " + searchSocioVar[opt - 1] + " -");
                            fragString = scan.nextLine();
                            isValid = true;
                            break;
                        case 0:
                            System.out.println("  Volviendo al menú del gestor...");
                            return new int[]{nPres, idPres};
                        default:
                            System.err.println("  Entrada no válida");
                    }
                } while (!isValid);

                try {
                    if (opt != 1) {
                        List<Socio> socios = BiblioDBSocio.getInstance().searchTB(opt, fragString);
                        Set<Integer> idsocs = socios.stream().map(Socio::getIdSoc).collect(Collectors.toSet());
                        socios.stream().sorted(Socio::compareTo).forEach(System.out::println);
                        do {
                            System.out.println("Introduce ID del socio de la lista anterior\n" +
                                               "(-1 para cancelar operación):");
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                    return new int[]{nPres, idPres};
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
                        BiblioDBSocio.getInstance().searchTB(idSoc);
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
                    System.out.println("\nSelecciona criterio de búsqueda:\n" + searchLibroMenu);
                    try {
                        opt = scan.nextInt();
                    } catch (InputMismatchException ime) {
                        opt = -1;
                    }
                    scan.nextLine();
                    switch (opt) {
                        case 1:
                            System.out.println("Introduce " + searchLibroVar[opt - 1] + " -");
                            idLib = scan.nextInt();
                            scan.nextLine();
                            isValid = true;
                            break;
                        case 2:
                        case 3:
                            System.out.println("Introduce " + searchLibroVar[opt - 1] + " -");
                            fragString = scan.nextLine();
                            isValid = true;
                            break;
                        case 0:
                            System.out.println("  Volviendo al menú del gestor...");
                            return new int[]{nPres, idPres};
                        default:
                            System.err.println("  Entrada no válida");
                    }
                } while (!isValid);

                try {
                    if (opt != 1) {
                        List<Libro> libros = BiblioDBLibro.getInstance().searchTB(opt, fragString);
                        Set<Integer> idlibs = libros.stream().map(Libro::getIdLib).collect(Collectors.toSet());
                        libros.stream().sorted(Libro::compareTo).forEach(System.out::println);
                        do {
                            System.out.println("Introduce ID del libro de la lista anterior\n" +
                                               "(-1 para cancelar operación):");
                            try {
                                ID = scan.nextInt();
                                if (ID == -1) {
                                    System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                    return new int[]{nPres, idPres};
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
                        BiblioDBLibro.getInstance().searchTB(idLib);
                    }
                    isPossible = true;
                } catch (RuntimeException re) {
                    System.err.println(re.getMessage());
                    isPossible = false;
                }
            } while (!isPossible);

            try {
                BiblioDBPrestamo.getInstance().addTB(new Prestamo(nPres + 1, idSoc, idLib));
                ++nPres;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nPres, idPres};
    }

    /**
     * Método para imprimir en pantalla los préstamos
     * registrados al momento en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private static void listPrestamos(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Prestamo> arrayPrestamos;

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
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchDetailTB();
                    } else {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchTB();
                    }
                    System.out.println("Ordenación por ID del préstamo...");
                    arrayPrestamos.stream().sorted(Prestamo::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchDetailTB();
                    } else {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchTB();
                    }
                    System.out.println("Ordenación por ID del socio...");
                    arrayPrestamos.stream().sorted(Comparator.comparing(Prestamo::getIdSoc)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchDetailTB();
                    } else {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchTB();
                    }
                    System.out.println("Ordenación por ID del libro...");
                    arrayPrestamos.stream().sorted(Comparator.comparing(Prestamo::getIdLib)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 4:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchDetailTB();
                    } else {
                        arrayPrestamos = BiblioDBPrestamo.getInstance().searchTB();
                    }
                    System.out.println("Ordenación por fecha...");
                    arrayPrestamos.stream().sorted(Comparator.comparing(Prestamo::getFechaPres)).forEach(System.out::println);
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
     * Método para buscar préstamos en la base de datos según
     * ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private static void searchPrestamos(Scanner scan) {
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
                        ID = scan.nextInt();
                        scan.nextLine();
                        isValid = true;
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
                List<Prestamo> prestamos;
                if (opt < 4) {
                    prestamos = BiblioDBPrestamo.getInstance().searchTB(opt, ID);
                } else {
                    prestamos = BiblioDBPrestamo.getInstance().searchTB(date);
                }
                prestamos.forEach(System.out::println);
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
     * @param nPres  Número de préstamos en activo dentro de la base de datos
     * @param idPres Máxima ID de préstamos dentro de la base de datos
     * @return Valores actualizados de nPres y idPres
     */
    private static int[] deletePrestamo(Scanner scan, int nPres, int idPres) {
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
                        ID = scan.nextInt();
                        scan.nextLine();
                        isValid = true;
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
                        return new int[]{nPres, idPres};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                List<Prestamo> prestamos;
                Set<Integer> idpres;
                if (opt == 1) {
                    idPres = BiblioDBPrestamo.getInstance().deleteTB(ID);
                    nPres--;
                } else {
                    if (opt == 2 || opt == 3) {
                        prestamos = BiblioDBPrestamo.getInstance().searchTB(opt, ID);
                    } else {
                        prestamos = BiblioDBPrestamo.getInstance().searchTB(date);
                    }
                    idpres = prestamos.stream().map(Prestamo::getIdPres).collect(Collectors.toSet());
                    prestamos.stream().sorted(Prestamo::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del préstamo a eliminar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nPres, idPres};
                            } else if (!idpres.add(ID)) {
                                idPres = BiblioDBLibro.getInstance().deleteTB(ID);
                                nPres--;
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

        return new int[]{nPres, idPres};
    }

}
