/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de usuarios
 */
package sql.users;

import tables.User;

import java.util.List;

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
     * @param name     Nombre del usuario
     * @param password Contraseña del usuario
     */
    void tryUser(String name, char[] password);

    /**
     * Método para contar el número de usuarios registrados
     * en la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    int[] countUser(User currentUser);

    /**
     * Método para añadir un nuevo usuario a la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    void addUser(User currentUser, User newUser);

    /**
     * Método para buscar usuarios de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    List<User> searchUser(User currentUser);

    /**
     * Método para eliminar un usuario de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     */
    int deleteUser(User currentUser, int ID);

}
