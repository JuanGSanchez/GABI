/**
 * Paquete de herramientas generales
 * del programa
 */
package utils;

import sql.reservoirs.LibDAO;
import tables.User;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Properties;
import java.util.Scanner;

/**
 * Clase de utilidades para la entrada de datos por consola
 */
public final class Utils {
    /**
     * Lista de expresiones reguladas para las entradas
     * de campos de texto
     */
    private static final String[] regex = new String[]{
            ".*[@#$%&ºª'`*_+=|/<>{}\\[\\]~].*",
            ".*[\\d¡!@#$%&ºª'`*.:;()_+=|/<>¿?{}\\[\\]~].*",
            ".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*",
            ".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*",
            ".*[\\d¡!@#$%&ºª'`*.,:;()_+=|/<>¿?{}\\[\\]~].*",
            ".*[@#$%&ºª'`*_+=|/<>{}\\[\\]~].*"
    };
    /**
     * Lista de nombres de los campos de texto
     */
    private static final String[] varName = new String[]{
            "título", "autor", "nombre", "apellidos", "nombre", "contraseña"
    };

    /**
     * Método para la lectura del archivo de propiedades
     *
     * @return Lista de propiedades del programa
     */
    public static Properties readProperties() {
        Properties configProps = new Properties();

        try (FileInputStream fis = new FileInputStream("src/utils/configuration.properties")) {
            configProps.load(fis);
        } catch (FileNotFoundException ffe) {
            System.err.println("  Error, no se encontró el archivo de propiedades del programa");
        } catch (IOException ioe) {
            System.err.println("  Error leyendo las propiedades del programa: " + ioe.getMessage());
        }

        return configProps;
    }

    /**
     * Método general de lectura para la opción
     * numérica de menús
     *
     * @param scan Entrada de datos por teclado
     * @return Índice numérico para el menú
     */
    public static int checkOptionInput(Scanner scan) {
        int opt;

        try {
            opt = scan.nextInt();
        } catch (InputMismatchException ime) {
            opt = -1;
        }
        scan.nextLine();

        return opt;
    }

    /**
     * Método general de lectura para la opción
     * numérica de menús, caso de mensaje de error
     *
     * @param scan Entrada de datos por teclado
     * @return Índice numérico para el menú
     */
    public static Integer checkOptionInput(Scanner scan, String errorMessage) {
        Integer opt = null;

        try {
            opt = scan.nextInt();
        } catch (InputMismatchException ime) {
            System.err.println(errorMessage);
        }
        scan.nextLine();

        return opt;
    }

    /**
     * Método general de lectura por consola de un campo de texto
     *
     * @param scan      Entrada de datos por teclado
     * @param opt       Índice numérico para la determinación del campo
     * @param charLimit Límite de caracteres del campo
     * @return Texto asociado al campo
     */
    public static String checkString(Scanner scan, int opt, String charLimit) {
        String s = null;
        boolean isValid = false;
        do {
            System.out.printf("Introduce el campo '%s' %s - ", varName[opt], opt == 1 ? "(nombre[, apellidos])" : "");
            try {
                s = scan.nextLine().trim();
                if (s.isEmpty()) {
                    System.err.println("  Entrada vacía");
                } else if (s.equals("-1")) {
                    return null;
                } else if (s.matches(regex[opt])) {
                    System.err.printf("  El campo '%s' no puede contener caracteres especiales\n", varName[opt]);
                } else if (s.length() > Integer.parseInt(charLimit)) {
                    System.err.printf("  El campo '%s' no puede superar los %s caracteres\n", varName[opt], charLimit);
                } else {
                    switch (opt) {
                        case 0:
                        case 2:
                        case 3:
                            s = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            break;
                        case 1:
                            if (s.matches(".*,.*")) {
                                String[] sArray = s.split(",");
                                if (sArray.length == 2) {
                                    for (int i = 0; i < sArray.length; i++) {
                                        sArray[i] = sArray[i].trim();
                                        sArray[i] = sArray[i].substring(0, 1).toUpperCase() + sArray[i].substring(1).toLowerCase();
                                    }
                                    s = String.format("%s %s", sArray[0], sArray[1]);
                                } else {
                                    System.err.println("  El nombre no sigue la estructura adecuada");
                                }
                            } else {
                                s = s.substring(0, 1).toUpperCase() + s.substring(1).toLowerCase();
                            }
                            break;
                        case 4:
                            s = s.toLowerCase();
                            break;
                        default:
                    }
                    isValid = true;
                }
            } catch (InputMismatchException ime) {
                System.err.println("  Entrada no válida");
            }
        } while (!isValid);

        return s;
    }

    /**
     * Método general para conectar con la base de datos
     * en el listado de su contenido, con o sin detalles
     *
     * @param scan        Entrada de datos por teclado
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param libDAO      Referencia general a la clase de conexión
     *                    a la tabla de datos
     * @param <T>         Clase asociada a la tabla de datos
     * @return Lista de contenido en la tabla de datos
     * en objetos de la clase T
     */
    public static <T> List<T> loadDataList(Scanner scan, User currentUser, LibDAO<T> libDAO) {
        System.out.println("\n  Introduce 1 para desplegar más detalles");
        if (scan.nextLine().equals("1")) {
            return libDAO.searchDetailTB(currentUser);
        } else {
            return libDAO.searchTB(currentUser);
        }
    }
}
