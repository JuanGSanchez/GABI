/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serial;
import java.io.Serializable;

/**
 * Clase base de las entidades o tipos de objetos
 * manejados en el programa
 */
abstract class Entity implements Serializable {
    /**
     * Identificador de clase para serialización
     */
    @Serial
    private static final long serialVersionUID = 2311070001L;
    /**
     * ID del objeto
     */
    private final int ID;

    /**
     * Constructor base de la clase
     *
     * @param id ID del objeto
     */
    protected Entity(int id) {
        ID = id;
    }

    /**
     * Método getter de la variable ID
     *
     * @return ID del objeto
     */
    public final int getID() {
        return ID;
    }

    /**
     * Método sobreescrito de descripción
     * del objeto, partiendo de su tipo
     * e ID asociada
     *
     * @return Semilla de descripción del objeto
     */
    @Override
    public String toString() {
        return "%s " + this.ID + ":  ";
    }
}
