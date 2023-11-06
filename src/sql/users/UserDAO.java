/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de usuarios
 */
package sql.users;

import tables.User;

import java.util.List;
import java.util.ResourceBundle;

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
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    void tryUser(String name, char[] password, ResourceBundle rb);

    /**
     * Método para contar el número de usuarios registrados
     * en la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    int[] countUser(User currentUser, ResourceBundle rb);

    /**
     * Método para añadir un nuevo usuario a la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    void addUser(User currentUser, User newUser, ResourceBundle rb);

    /**
     * Método para buscar usuarios de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    List<User> searchUser(User currentUser, ResourceBundle rb);

    /**
     * Método para eliminar un usuario de la base de datos
     *
     * @param currentUser Objeto de usuario con sus datos
     *                    de acceso a la base de datos
     * @param rb          Recurso para la localización
     *                    del texto del programa
     */
    int deleteUser(User currentUser, int ID, ResourceBundle rb);

}
