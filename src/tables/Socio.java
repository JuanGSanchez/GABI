/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serializable;

/**
 * Clase de la tabla de datos de socios,
 * personas registradas que solicitan los libros
 *
 * @author JuanGS
 * @version 1.0
 */
public class Socio implements Comparable<Socio>, Serializable {
    /**
     * Límite de préstamos de un socio
     */
    public static final int MAX_PREST = 10;
    /**
     * ID del socio
     */
    private final int idSoc;
    /**
     * Nombre del socio
     */
    private final String nombre;
    /**
     * Apellidos del socio
     */
    private final String apellidos;

    /**
     * Constructor completo de la clase
     *
     * @param idSoc     ID del socio
     * @param nombre    Nombre del socio
     * @param apellidos Apellidos del socio
     */
    public Socio(int idSoc, String nombre, String apellidos) {
        this.idSoc = idSoc;
        this.nombre = nombre;
        this.apellidos = apellidos;
    }

    /**
     * Método getter de la variable ID
     *
     * @return ID del socio
     */
    public final int getIdSoc() {
        return idSoc;
    }

    /**
     * Método getter de la variable nombre
     *
     * @return Nombre del socio
     */
    public final String getNombre() {
        return nombre;
    }

    /**
     * Método getter de la variable apellidos
     *
     * @return Apellidos del socio
     */
    public final String getApellidos() {
        return apellidos;
    }

    /**
     * Método sobreescrito de comparación entre objetos Socio,
     * fijándose en el nombre y apellidos del Socio
     *
     * @param o Objeto externo con el que comparar
     * @return booleano resultado de la comparación
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Socio socio = (Socio) o;

        if (!nombre.equalsIgnoreCase(socio.nombre)) return false;
        return apellidos.equalsIgnoreCase(socio.apellidos);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las variables nombre
     * y apellidos del socio
     */
    @Override
    public int hashCode() {
        int result = nombre.hashCode();
        result = 31 * result + apellidos.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Descripción del socio
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + idSoc + ":   " + String.format("%-20s", nombre)
               + "   " + String.format("%-40s", apellidos);
    }

    /**
     * Método sobreescrito de ordenación de objetos Socio según
     * su identificación numérica ID
     *
     * @param otherSocio El otro objeto Libro al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Socio otherSocio) {
        return this.idSoc - otherSocio.getIdSoc();
    }
}
