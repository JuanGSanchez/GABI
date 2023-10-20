/**
 * Paquete de clases de las tablas de datos
 */
package tables;

/**
 * Clase de la tabla de datos de usuarios
 * permitidos en la base de datos
 *
 * @author JuanGS
 * @version 1.0
 */
public class User implements Comparable<User> {
    /**
     * ID de usuario en la tabla de datos
     */
    private final int idUser;
    /**
     * Nombre de usuario
     */
    private final String name;
    /**
     * Constraseña del usuario
     */
    private final String password;

    /**
     * Constructor de la clase para devolver
     * el objeto sin contraseña
     *
     * @param idUser ID de usuario en la tabla de datos
     * @param name   Nombre de usuario
     */
    public User(int idUser, String name) {
        this(idUser, name, "");
    }

    /**
     * Constructor completo de la clase
     * para la entrada de datos
     *
     * @param idUser   ID de usuario en la tabla de datos
     * @param name     Nombre de usuario
     * @param password Constraseña del usuario
     */
    public User(int idUser, String name, String password) {
        this.idUser = idUser;
        this.name = name;
        this.password = password;
    }

    /**
     * Método getter de la ID del usuario
     *
     * @return ID de usuario en la tabla de datos
     */
    public final int getIdUser() {
        return idUser;
    }

    /**
     * Método getter del nombre de usuario
     *
     * @return Nombre de usuario
     */
    public final String getName() {
        return name;
    }

    /**
     * Método getter de la contraseña,
     * para pasarlo a la base de datos
     *
     * @return Constraseña del usuario
     */
    public final String getPassword() {
        return password;
    }

    /**
     * Método equals de la clase,
     * comparando nombres
     *
     * @param o Objeto con el que comparar
     * @return Booleano según coincidan dos objetos Usuario
     * con el mismo nombre
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        User user = (User) o;

        return name.equals(user.name);
    }

    /**
     * Método de código hash de la clase
     *
     * @return Código hash del objeto
     */
    @Override
    public int hashCode() {
        return name.hashCode();
    }

    /**
     * Método de descripción del ojeto
     *
     * @return Texto de descripción del objeto
     */
    @Override
    public String toString() {
        return String.format("Usuario %d: %-10s", this.idUser, this.name);
    }

    /**
     * Método de comparación entre objetos Usuario
     * para ordenación natural según el nombre
     *
     * @param otherUser otro objeto Usuario con el que comparar
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(User otherUser) {
        return this.idUser - otherUser.getIdUser();
    }
}
