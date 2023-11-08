/**
 * Paquete de herramientas generales
 * del programa
 */
package utils;

import sql.reservoirs.LibDAO;
import tables.Book;
import tables.Loan;
import tables.Member;
import tables.User;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
    private static ResourceBundle resourceBundle;
    /**
     * Lista de nombres de los campos de texto
     */
    private static String[] varName;

    /**
     * Método para la lectura del archivo de propiedades
     *
     * @return Lista de propiedades del programa
     */
    public static Properties readProperties() {
        Properties configProps = new Properties();

        try (FileInputStream fis = new FileInputStream("src/utils/configuration.properties")) {
            configProps.load(fis);
        } catch (IOException ioe) {
            System.err.println(ioe.getLocalizedMessage());
        }

        return configProps;
    }

    /**
     * Método para la lectura del texto para el programa
     * en el idioma seleccionado
     *
     * @param language Abreviatura del idioma
     * @param country  Abreviatura del país
     * @return Recurso con el texto del programa
     * en el idioma seleccionado
     */
    public static ResourceBundle readLanguage(String language, String country) {
        Locale locale = new Locale(language, country);
        Locale.setDefault(locale);
        resourceBundle = ResourceBundle.getBundle("statements", locale);
        varName = new String[]{resourceBundle.getString("program-book-properties-2"),
                resourceBundle.getString("program-book-properties-3"),
                resourceBundle.getString("program-member-properties-2"),
                resourceBundle.getString("program-member-properties-3"),
                resourceBundle.getString("program-user-properties-2"),
                resourceBundle.getString("program-user-properties-3")};
        return resourceBundle;
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
            System.out.printf(resourceBundle.getString("program-utils-enter") + " - ",
                    varName[opt], opt == 1 ? String.format("(%s)", resourceBundle.getString("program-utils-fullname")) : "");
            try {
                s = scan.nextLine().trim();
                if (s.isEmpty()) {
                    System.err.printf("  %s\n", resourceBundle.getString("program-error-empty"));
                } else if (s.equals("-1")) {
                    return null;
                } else if (s.matches(regex[opt])) {
                    System.err.printf("  " + resourceBundle.getString("program-error-character-special") + "\n", varName[opt]);
                } else if (s.length() > Integer.parseInt(charLimit)) {
                    System.err.printf("  " + resourceBundle.getString("program-error-character-limit") + "\n", varName[opt], charLimit);
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
                                    System.err.printf("  %s\n", resourceBundle.getString("program-error-character-structure"));
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
                System.err.printf("  %s\n", resourceBundle.getString("program-error-entry"));
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
        System.out.printf("\n  %s\n", resourceBundle.getString("program-utils-details"));
        if (scan.nextLine().equals("1")) {
            return libDAO.searchDetailDB(currentUser, resourceBundle);
        } else {
            return libDAO.searchDB(currentUser, resourceBundle);
        }
    }

    /**
     * Método para componer la descripción localizada
     * del objeto Book
     *
     * @param book Objeto Book a describir
     * @return Descripción localizada del objeto
     */
    public static String entityToString(Book book) {
        return String.format(book.toString(),
                resourceBundle.getString("program-properties-field-1-singular"),
                book.isLent() ? resourceBundle.getString("dao-book-lent-true") : resourceBundle.getString("dao-book-lent-false"));
    }

    /**
     * Método para componer la descripción localizada
     * del objeto Member
     *
     * @param member Objeto Member a describir
     * @return Descripción localizada del objeto
     */
    public static String entityToString(Member member) {
        String desc = String.format(member.toString(),
                resourceBundle.getString("program-properties-field-2-singular"));
        List<Book> listBook = member.getListBook();
        if (listBook != null) {
            if (!listBook.isEmpty()) desc += "\n\t";
            desc += listBook.stream().sorted(Comparator.comparing(Book::getID))
                    .map(Utils::entityToString).collect(Collectors.joining("\n\t"));
            desc += "\n\t " + listBook.size() + " " + String.format(resourceBundle.getString("dao-book-lent-total"),
                    (listBook.size() == 1 ? resourceBundle.getString("dao-book-lent-true-singular") : resourceBundle.getString("dao-book-lent-true-plural")));
        }

        return desc;
    }

    /**
     * Método para componer la descripción localizada
     * del objeto Loan
     *
     * @param loan Objeto Loan a describir
     * @return Descripción localizada del objeto
     */
    public static String entityToString(Loan loan) {
        String desc = String.format(loan.toString(),
                resourceBundle.getString("program-properties-field-3-singular"),
                resourceBundle.getString("program-properties-field-1-singular"),
                resourceBundle.getString("dao-loan-desc-1"), resourceBundle.getString("dao-loan-desc-2"));
        if (loan.getBook() != null) {
            desc += "\n\t" + Utils.entityToString(loan.getMember()) + "\n\t" + Utils.entityToString(loan.getBook());
        }
        return desc;
    }

    /**
     * Método para componer la descripción localizada
     * del objeto User
     *
     * @param user Objeto User a describir
     * @return Descripción localizada del objeto
     */
    public static String entityToString(User user) {
        return String.format(user.toString(),
                resourceBundle.getString("program-properties-field-4-singular"));
    }
}
