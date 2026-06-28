/**
 * Paquete de comandos de conexión con la base de datos
 * para la gestión de usuarios
 */
package sql.users;

import sql.reservoirs.LibDAO;
import tables.User;

import java.util.ResourceBundle;

/**
 * Interfaz principal de métodos de conexión a una base de datos
 * para la gestión de usuarios
 *
 * @author JuanGS
 * @version 1.0
 * @since 10-2023
 */
public interface UserDAO extends LibDAO<User> {
    /**
     * Método para probar la conexión de un usuario y contraseña
     * suministrados, comprobando su validez de conexión
     *
     * @param name     Nombre del usuario
     * @param password Contraseña del usuario
     * @param rb       Recurso para la localización
     *                 del texto del programa
     */
    void tryDB(String name, char[] password, ResourceBundle rb);

}