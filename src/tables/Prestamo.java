/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Clase de la tabla de datos de préstamos
 * de libros a socios
 *
 * @author JuanGS
 * @version 1.0
 */
public class Prestamo implements Comparable<Prestamo>, Serializable {
    /**
     * ID del préstamo
     */
    private final int idPres;
    /**
     * ID del socio
     */
    private final int idSoc;
    /**
     * ID del libro
     */
    private final int idLib;
    /**
     * Fecha del préstamo
     */
    private final LocalDate fechaPres;
    /**
     * Libro asociado al préstamo
     */
    private final Libro libro;
    /**
     * Socio asociado al préstamo
     */
    private final Socio socio;

    /**
     * Constructor completo de la clase,
     * designado para creación de entrada
     *
     * @param idPres ID del préstamo
     * @param idSoc  ID del socio
     * @param idLib  ID del libro
     */
    public Prestamo(int idPres, int idSoc, int idLib) {
        this(idPres, idSoc, idLib, LocalDate.now());
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada
     *
     * @param idPres    ID del préstamo
     * @param idSoc     ID del socio
     * @param idLib     ID del libro
     * @param fechaPres fecha del préstamo
     */
    public Prestamo(int idPres, int idSoc, int idLib, LocalDate fechaPres) {
        this(idPres, idSoc, idLib, fechaPres, null, null);
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada con más detalle
     *
     * @param idPres    ID del préstamo
     * @param idSoc     ID del socio
     * @param idLib     ID del libro
     * @param fechaPres fecha del préstamo
     * @param libro     Libro asociado al préstamo
     * @param socio     Socio asociado al préstamo
     */
    public Prestamo(int idPres, int idSoc, int idLib, LocalDate fechaPres, Libro libro, Socio socio) {
        this.idPres = idPres;
        this.idSoc = idSoc;
        this.idLib = idLib;
        this.fechaPres = fechaPres;
        this.libro = libro;
        this.socio = socio;
    }

    /**
     * Método getter del ID del préstamo
     *
     * @return ID del préstamo
     */
    public final int getIdPres() {
        return idPres;
    }

    /**
     * Método getter del ID del socio
     *
     * @return ID del socio
     */
    public final int getIdSoc() {
        return idSoc;
    }

    /**
     * Método getter del ID del libro
     *
     * @return ID del libro
     */
    public final int getIdLib() {
        return idLib;
    }

    /**
     * Método getter de la fecha del préstamo
     *
     * @return Fecha del préstamo
     */
    public final LocalDate getFechaPres() {
        return fechaPres;
    }

    /**
     * Método getter de la variable libro
     *
     * @return Libro asociado al préstamo
     */
    public Libro getLibro() {
        return libro;
    }

    /**
     * Método getter de la variable socio
     *
     * @return Socio asociado al préstamo
     */
    public Socio getSocio() {
        return socio;
    }

    /**
     * Método sobreescrito de comparación entre préstamos,
     * fijándose en las ID de socio y libro, así como la fecha
     *
     * @param o Objeto externo con el que comparar
     * @return booleano resultado de la comparación
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Prestamo prestamo = (Prestamo) o;

        if (idSoc != prestamo.idSoc) return false;
        if (idLib != prestamo.idLib) return false;
        return fechaPres.equals(prestamo.fechaPres);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las ID de socio y libro,
     * así como la fecha
     */
    @Override
    public int hashCode() {
        int result = idSoc;
        result = 31 * result + idLib;
        result = 31 * result + fechaPres.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Información del préstamo
     */
    @Override
    public String toString() {
        String desc = this.getClass().getSimpleName() + " " + idPres + ": Libro " + idLib + " a Socio " + idSoc + " el "
                      + fechaPres.format(DateTimeFormatter.ISO_LOCAL_DATE);
        if (libro != null) {
            desc += "\n\t" + libro + "\n\t" + socio;
        }
        return desc;
    }

    /**
     * Método sobreescrito de ordenación de objetos Prestamo según
     * su identificación numérica ID
     *
     * @param otherPrestamo El otro objeto Prestamo al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Prestamo otherPrestamo) {
        return this.idPres - otherPrestamo.getIdPres();
    }
}
