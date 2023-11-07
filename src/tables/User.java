/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serial;

/**
 * Clase de la tabla de datos de usuarios
 * permitidos en la base de datos
 *
 * @author JuanGS
 * @version 1.0
 */
public final class User extends Entity implements Comparable<User> {
    /**
     * Identificador de clase para serialización
     */
    @Serial
    private static final long serialVersionUID = 2311070005L;
    /**
     * Nombre de usuario
     */
    private final String name;
    /**
     * Contraseña del usuario
     */
    private final String password;

    /**
     * Constructor de la clase para devolver
     * el objeto sin contraseña
     *
     * @param ID   ID de usuario en la tabla de datos
     * @param name Nombre de usuario
     */
    public User(int ID, String name) {
        this(ID, name, "");
    }

    /**
     * Constructor completo de la clase
     * para la transmisión de datos del usuario
     * por el programa
     *
     * @param name     Nombre de usuario
     * @param password Contraseña del usuario
     */
    public User(String name, String password) {
        this(0, name, password);
    }

    /**
     * Constructor completo de la clase
     * para la entrada de datos
     *
     * @param ID       ID de usuario en la tabla de datos
     * @param name     Nombre de usuario
     * @param password Contraseña del usuario
     */
    public User(int ID, String name, String password) {
        super(ID);
        this.name = name;
        this.password = password;
    }

    /**
     * Método getter del nombre de usuario
     *
     * @return Nombre de usuario
     */
    public String getName() {
        return name;
    }

    /**
     * Método getter de la contraseña,
     * para pasarlo a la base de datos
     *
     * @return Contraseña del usuario
     */
    public String getPassword() {
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
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Información del usuario
     */
    @Override
    public String toString() {
        return super.toString() + String.format("%-10s", this.name);
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
        return this.getID() - otherUser.getID();
    }
}
