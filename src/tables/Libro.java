/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serializable;

/**
 * Clase de la tabla de datos de libros,
 * entidades susceptibles de prestarse
 *
 * @author JuanGS
 * @version 1.0
 */
public class Libro implements Comparable<Libro>, Serializable {
    /**
     * ID del libro
     */
    private final int idLib;
    /**
     * Título del libro
     */
    private final String titulo;
    /**
     * Autor (principal) del libro
     */
    private final String autor;
    /**
     * Estado de disponibilidad del libro
     */
    private final boolean prestado;

    /**
     * Constructor completo de la clase,
     * designado para creación de entrada
     *
     * @param idLib  ID del libro
     * @param titulo Título del libro
     * @param autor  Autor (principal) del libro
     */
    public Libro(int idLib, String titulo, String autor) {
        this(idLib, titulo, autor, false);
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada
     *
     * @param idLib    ID del libro
     * @param titulo   Título del libro
     * @param autor    Autor (principal) del libro
     * @param prestado Estado de disponibilidad del libro
     */
    public Libro(int idLib, String titulo, String autor, boolean prestado) {
        this.idLib = idLib;
        this.titulo = titulo;
        this.autor = autor;
        this.prestado = prestado;
    }

    /**
     * Método getter de la variable ID
     *
     * @return ID del libro
     */
    public final int getIdLib() {
        return idLib;
    }

    /**
     * Método getter de la variable título
     *
     * @return Título del libro
     */
    public final String getTitulo() {
        return titulo;
    }

    /**
     * Método getter de la variable autor
     *
     * @return Autor (principal) del libro
     */
    public final String getAutor() {
        return autor;
    }

    /**
     * Método getter de la variable Prestado
     *
     * @return estado "prestado" del libro
     */
    public final boolean isPrestado() {
        return prestado;
    }

    /**
     * Método sobreescrito de comparación entre objetos Libro,
     * fijándose en el nombre y autor principal del libro
     *
     * @param o Objeto externo con el que comparar
     * @return booleano resultado de la comparación
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Libro libro = (Libro) o;

        if (!titulo.equalsIgnoreCase(libro.titulo)) return false;
        return autor.equalsIgnoreCase(libro.autor);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las variables nombre
     * y autor del libro
     */
    @Override
    public int hashCode() {
        int result = titulo.hashCode();
        result = 31 * result + autor.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Descripción del libro
     */
    @Override
    public String toString() {
        return this.getClass().getSimpleName() + " " + idLib + ":   " + String.format("%-120s", titulo)
               + "   ;   " + String.format("%-60s", autor) + String.format(" (%s)", prestado ? "prestado" : "disponible");
    }

    /**
     * Método sobreescrito de ordenación de objetos Libro según
     * su identificación numérica ID
     *
     * @param otherLibro El otro objeto Libro al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Libro otherLibro) {
        return this.idLib - otherLibro.getIdLib();
    }
}
