/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.User;

import java.util.List;
import java.util.ResourceBundle;

/**
 * Interfaz principal de métodos de conexión a una base de datos
 * con las tablas manejadas
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public interface LibDAO<T> {

    /**
     * Método de conteo de entradas en la tabla de datos
     * asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Número de entradas en la base de datos
     */
    int[] countDB(User currentUser, ResourceBundle rb);

    /**
     * Método de introducción de una nueva entrada en la
     * tabla de datos asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param t           Objeto de la clase T, cuyos datos se introducen
     *                    en la tabla de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    void addDb(User currentUser, T t, ResourceBundle rb);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchDB(User currentUser, ResourceBundle rb);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T, añadiendo más detalles
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchDetailDB(User currentUser, ResourceBundle rb);

    /**
     * Método para eliminar una entrada en la tabla de datos
     * asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada a eliminar
     * @param rb          Recurso para la localización
     *                    del texto del programa
     * @return ID máxima actualizada de la tabla de datos
     */
    int deleteDB(User currentUser, int ID, ResourceBundle rb);

}
