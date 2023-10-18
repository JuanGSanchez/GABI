/**
 * Paquete de comandos de conexión con la base de datos
 */
package sql;

/**
 * Interfaz principal de métodos de conexión a una base de datos
 * para la gestión de usuarios
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
public interface UserDAO {
    /**
     * Método para probar la conexión de un usuario y contraseña
     * suministrados, comprobando su validez de conexión
     *
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    void tryUser(String user, char[] password);

    /**
     * Método para añadir un nuevo usuario a la base de datos
     *
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    void addUser(String user, char[] password);

    /**
     * Método para eliminar un usuario de la base de datos
     *
     * @param user     Nombre del usuario
     * @param password Contraseña asociada al usuario
     */
    void deleteUser(String user, char[] password);

}
