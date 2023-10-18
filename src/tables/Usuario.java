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
public class Usuario implements Comparable<Usuario> {
    /**
     * ID de usuario en la tabla de datos
     */
    private final int idUser;
    /**
     * Nombre de usuario
     */
    private final String nombre;
    /**
     * Constraseña del usuario
     */
    private final String password;

    /**
     * Constructor de la clase para devolver
     * el objeto sin contraseña
     *
     * @param idUser ID de usuario en la tabla de datos
     * @param nombre Nombre de usuario
     */
    public Usuario(int idUser, String nombre) {
        this(idUser, nombre, "");
    }

    /**
     * Constructor completo de la clase
     * para la entrada de datos
     *
     * @param idUser   ID de usuario en la tabla de datos
     * @param nombre   Nombre de usuario
     * @param password Constraseña del usuario
     */
    public Usuario(int idUser, String nombre, String password) {
        this.idUser = idUser;
        this.nombre = nombre;
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
    public final String getNombre() {
        return nombre;
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

        Usuario usuario = (Usuario) o;

        return nombre.equals(usuario.nombre);
    }

    /**
     * Método de código hash de la clase
     *
     * @return Código hash del objeto
     */
    @Override
    public int hashCode() {
        return nombre.hashCode();
    }

    /**
     * Método de descripción del ojeto
     *
     * @return Texto de descripción del objeto
     */
    @Override
    public String toString() {
        return String.format("Usuario %d: %-10s", this.idUser, this.nombre);
    }

    /**
     * Método de comparación entre objetos Usuario
     * para ordenación natural según el nombre
     *
     * @param otherUsuario otro objeto Usuario con el que comparar
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Usuario otherUsuario) {
        return this.nombre.compareTo(otherUsuario.getNombre());
    }
}
