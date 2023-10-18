/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de repositorios de datos
 */
package sql.reservoirs;

import java.util.List;

/**
 * Interfaz principal de métodos de conexión a una base de datos
 * con las tablas manejadas
 *
 * @author JuanGS
 * @version 1.0
 * @since 07-2023
 */
public interface BiblioDAO<T> {

    /**
     * Método de conteo de entradas en la tabla de datos
     * asociada a la clase T
     *
     * @return Número de entradas en la base de datos
     */
    int[] countTB(String user, String password);

    /**
     * Método de introducción de una nueva entrada en la
     * tabla de datos asociada a la clase T
     *
     * @param t Objeto de la clase T, cuyos datos se introducen
     *          en la tabla de datos
     */
    void addTB(String user, String password, T t);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T
     *
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchTB(String user, String password);

    /**
     * Método de búsqueda de entradas en la tabla de datos
     * asociada a la clase T, añadiendo más detalles
     *
     * @return Lista de objetos de la clase T, construidos
     * desde los datos recuperados de la tabla
     */
    List<T> searchDetailTB(String user, String password);

    /**
     * Método para eliminar una entrada en la tabla de datos
     * asociada a la clase T
     *
     * @param ID Identificación numérica de la entrada a eliminar
     */
    int deleteTB(String user, String password, int ID);
}
