/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import tables.User;

import java.util.List;

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
     * @return Número de entradas en la base de datos
     */
    int[] countTB(User currentUser);

    /**
     * Método de introducción de una nueva entrada en la
     * tabla de datos asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param t           Objeto de la clase T, cuyos datos se introducen
     *                    en la tabla de datos
     */
    void addTB(User currentUser, T t);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchTB(User currentUser);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T, añadiendo más detalles
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchDetailTB(User currentUser);

    /**
     * Método para eliminar una entrada en la tabla de datos
     * asociada a la clase T
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param ID          Identificación numérica de la entrada a eliminar
     * @return ID máxima actualizada de la tabla de datos
     */
    int deleteTB(User currentUser, int ID);
}
