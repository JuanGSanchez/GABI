/**
 * Paquete de menús del programa
 */
package manager;

import sql.reservoirs.BiblioDBSocio;
import tables.Socio;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Clase del menú del gestor de socios en el programa
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
final class SocioMenu {
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
    SocioMenu(String user, String password) {
        this.user = user;
        this.password = password;
    }

    /**
     * Método para dar de alta nuevos socios en la base de datos
     *
     * @param scan  Entrada de datos por teclado
     * @param nSoc  Número de socios registrados dentro de la base de datos
     * @param idSoc Máxima ID de socios dentro de la base de datos
     * @return Valores actualizados de nSoc y idSoc
     */
    private int[] addSocio(Scanner scan, int nSoc, int idSoc) {
        boolean isValid;
        boolean repeat;
        String nombre = null;
        String apellidos = null;

        do {
            isValid = false;
            System.out.println("\n    Alta de Nuevo Socio\n(-1 en cualquier momento para cancelar operación)\n");
            do {
                System.out.print("Introduce nombre - ");
                try {
                    nombre = scan.nextLine();
                    if ("".equals(nombre)) {
                        System.err.println("  Entrada vacía");
                    } else if (nombre.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nSoc, idSoc};
                    } else if (nombre.matches(".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  El nombre no puede contener números o caracteres especiales");
                    } else if (nombre.trim().length() > 20) {
                        System.err.println("  El nombre no puede superar los 20 caracteres");
                    } else {
                        nombre = nombre.trim();
                        nombre = nombre.substring(0, 1).toUpperCase() + nombre.substring(1).toLowerCase();
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
                    apellidos = scan.nextLine();
                    if ("".equals(apellidos)) {
                        System.err.println("  Entrada vacía");
                    } else if (apellidos.equals("-1")) {
                        System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                        return new int[]{nSoc, idSoc};
                    } else if (apellidos.matches(".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*")) {
                        System.err.println("  Los apellidos no pueden contener números o caracteres especiales");
                    } else if (apellidos.trim().length() > 40) {
                        System.err.println("  El título no puede superar los 40 caracteres");
                    } else {
                        apellidos = apellidos.trim();
                        apellidos = apellidos.substring(0, 1).toUpperCase() + apellidos.substring(1).toLowerCase();
                        isValid = true;
                    }
                } catch (InputMismatchException ime) {
                    System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                BiblioDBSocio.getInstance().addTB(user, password, new Socio(nSoc + 1, nombre, apellidos));
                ++nSoc;
            } catch (RuntimeException re) {
                System.err.println("  Error durante el registro en la base de datos: " + re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);

        return new int[]{nSoc, idSoc};
    }

    /**
     * Método para imprimir en pantalla los socios registrados
     * en la base de datos
     *
     * @param scan Entrada de datos por teclado
     */
    private void listSocios(Scanner scan) {
        boolean isValid = false;
        int opt;
        List<Socio> arraySocios;

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
                        arraySocios = BiblioDBSocio.getInstance().searchDetailTB(user, password);
                    } else {
                        arraySocios = BiblioDBSocio.getInstance().searchTB(user, password);
                    }
                    System.out.println("Ordenación por ID...");
                    arraySocios.stream().sorted(Socio::compareTo).forEach(System.out::println);
                    isValid = true;
                    break;
                case 2:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arraySocios = BiblioDBSocio.getInstance().searchDetailTB(user, password);
                    } else {
                        arraySocios = BiblioDBSocio.getInstance().searchTB(user, password);
                    }
                    System.out.println("Ordenación por nombre...");
                    arraySocios.stream().sorted(Comparator.comparing(Socio::getNombre).thenComparing(Socio::getApellidos)).forEach(System.out::println);
                    isValid = true;
                    break;
                case 3:
                    System.out.println("\nIntroduce 1 para desplegar más detalles");
                    if (scan.nextLine().equals("1")) {
                        arraySocios = BiblioDBSocio.getInstance().searchDetailTB(user, password);
                    } else {
                        arraySocios = BiblioDBSocio.getInstance().searchTB(user, password);
                    }
                    System.out.println("Ordenación por apellidos...");
                    arraySocios.stream().sorted(Comparator.comparing(Socio::getApellidos).thenComparing(Socio::getNombre)).forEach(System.out::println);
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
     * Método para buscar socios en la base de datos según
     * ciertos criterios
     *
     * @param scan Entrada de datos por teclado
     */
    private void searchSocios(Scanner scan) {
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
                        ID = scan.nextInt();
                        scan.nextLine();
                        isValid = true;
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
                    Socio socio = BiblioDBSocio.getInstance().searchTB(user, password, ID);
                    System.out.println(socio);
                } else {
                    List<Socio> socios = BiblioDBSocio.getInstance().searchTB(user, password, opt, fragString);
                    socios.forEach(System.out::println);
                }
            } catch (RuntimeException re) {
                System.err.println(re.getMessage());
            }

            System.out.println("\nIntroduce 1 para repetir operación - ");
            repeat = scan.nextLine().equals("1");
        } while (repeat);
    }

    /**
     * Método para dar de baja un socio de la base de datos
     *
     * @param scan  Entrada de datos por teclado
     * @param nSoc  Número de socios registrados dentro de la base de datos
     * @param idSoc Máxima ID de socios dentro de la base de datos
     * @return Valores actualizados de nSoc y idSoc
     */
    private int[] deleteSocio(Scanner scan, int nSoc, int idSoc) {
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
                        ID = scan.nextInt();
                        scan.nextLine();
                        isValid = true;
                        break;
                    case 2:
                    case 3:
                        System.out.println("Introduce " + searchVar[opt - 1] + " -");
                        fragString = scan.nextLine();
                        isValid = true;
                        break;
                    case 0:
                        System.out.println("  Volviendo al menú del gestor...");
                        return new int[]{nSoc, idSoc};
                    default:
                        System.err.println("  Entrada no válida");
                }
            } while (!isValid);

            try {
                if (opt == 1) {
                    idSoc = BiblioDBSocio.getInstance().deleteTB(user, password, ID);
                    nSoc--;
                } else {
                    List<Socio> socios = BiblioDBSocio.getInstance().searchTB(user, password, opt, fragString);
                    Set<Integer> idsocs = socios.stream().map(Socio::getIdSoc).collect(Collectors.toSet());
                    socios.stream().sorted(Socio::compareTo).forEach(System.out::println);
                    do {
                        System.out.println("Introduce ID del socio a retirar de la lista anterior\n" +
                                           "(-1 para cancelar operación) -");
                        try {
                            ID = scan.nextInt();
                            if (ID == -1) {
                                System.out.println("  Operación cancelada, volviendo al menú del gestor...");
                                return new int[]{nSoc, idSoc};
                            } else if (!idsocs.add(ID)) {
                                idSoc = BiblioDBSocio.getInstance().deleteTB(user, password, ID);
                                nSoc--;
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

        return new int[]{nSoc, idSoc};
    }

    /**
     * Método del menú principal del gestor de socios, desde el cual
     * se acceden a las acciones disponibles
     *
     * @param scan  Entrada de datos por teclado
     * @param nSoc  Número de socios registrados dentro de la base de datos
     * @param idSoc Máxima ID de socios dentro de la base de datos
     * @return Valores actualizados de nSoc y idSoc
     */
    int[] seleccionMenu(Scanner scan, int nSoc, int idSoc) {
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
                    count = addSocio(scan, nSoc, idSoc);
                    nSoc = count[0];
                    idSoc = count[1];
                    break;
                case 2:
                    if (nSoc == 0) {
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        listSocios(scan);
                        System.out.println("Total de socios: " + nSoc);
                    }
                    break;
                case 3:
                    if (nSoc == 0) {
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        searchSocios(scan);
                    }
                    break;
                case 4:
                    if (nSoc == 0) {
                        System.err.println("Error, no hay lista de socios disponible");
                    } else {
                        count = deleteSocio(scan, nSoc, idSoc);
                        nSoc = count[0];
                        idSoc = count[1];
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

        return new int[]{nSoc, idSoc};
    }

}
