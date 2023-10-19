/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

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
     * Lista de libros prestados
     */
    private final List<Libro> libroList;

    /**
     * Constructor completo de la clase
     * designado para creación de entrada
     *
     * @param idSoc     ID del socio
     * @param nombre    Nombre del socio
     * @param apellidos Apellidos del socio
     */
    public Socio(int idSoc, String nombre, String apellidos) {
        this(idSoc, nombre, apellidos, null);
    }

    /**
     * Constructor completo de la clase
     * designado para recolección de entrada
     *
     * @param idSoc     ID del socio
     * @param nombre    Nombre del socio
     * @param apellidos Apellidos del socio
     * @param libroList Lista de libros prestados
     */
    public Socio(int idSoc, String nombre, String apellidos, List<Libro> libroList) {
        this.idSoc = idSoc;
        this.nombre = nombre;
        this.apellidos = apellidos;
        this.libroList = libroList;
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
     * Método getter de la variable libroList
     *
     * @return Lista de libros prestados
     */
    public List<Libro> getLibroList() {
        return new ArrayList<>(libroList);
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
        String desc = this.getClass().getSimpleName() + " " + idSoc + ":   " + String.format("%-20s", nombre)
                      + "   " + String.format("%-40s", apellidos);
        if (libroList != null) {
            if (!libroList.isEmpty()) desc += "\n\t";
            desc += libroList.stream().sorted(Comparator.comparing(Libro::getIdLib)).map(Libro::toString).collect(Collectors.joining("\n\t"));
            desc += "\n\t " + libroList.size() + " " + (libroList.size() == 1 ? "libro prestado" : "libros prestados") + " en total";
        }

        return desc;
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
