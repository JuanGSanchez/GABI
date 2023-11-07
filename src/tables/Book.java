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
public class Book implements Comparable<Book>, Serializable {
    /**
     * ID del libro
     */
    private final int idBook;
    /**
     * Título del libro
     */
    private final String title;
    /**
     * Autor (principal) del libro
     */
    private final String author;
    /**
     * Estado de disponibilidad del libro
     */
    private final boolean lent;

    /**
     * Constructor completo de la clase,
     * designado para creación de entrada
     *
     * @param idBook ID del libro
     * @param title  Título del libro
     * @param author Autor (principal) del libro
     */
    public Book(int idBook, String title, String author) {
        this(idBook, title, author, false);
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada
     *
     * @param idBook ID del libro
     * @param title  Título del libro
     * @param author Autor (principal) del libro
     * @param lent   Estado de disponibilidad del libro
     */
    public Book(int idBook, String title, String author, boolean lent) {
        this.idBook = idBook;
        this.title = title;
        this.author = author;
        this.lent = lent;
    }

    /**
     * Método getter de la variable ID
     *
     * @return ID del libro
     */
    public final int getIdBook() {
        return idBook;
    }

    /**
     * Método getter de la variable título
     *
     * @return Título del libro
     */
    public final String getTitle() {
        return title;
    }

    /**
     * Método getter de la variable autor
     *
     * @return Autor (principal) del libro
     */
    public final String getAuthor() {
        return author;
    }

    /**
     * Método getter de la variable Prestado
     *
     * @return estado "prestado" del libro
     */
    public final boolean isLent() {
        return lent;
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

        Book book = (Book) o;

        if (!title.equalsIgnoreCase(book.title)) return false;
        return author.equalsIgnoreCase(book.author);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las variables nombre
     * y autor del libro
     */
    @Override
    public int hashCode() {
        int result = title.hashCode();
        result = 31 * result + author.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Descripción del libro
     */
    @Override
    public String toString() {
        return "%s " + idBook + ":  " + String.format("%-120s", title) + "  ;  " + String.format("%-60s", author) + "  (%s)";
    }

    /**
     * Método sobreescrito de ordenación de objetos Libro según
     * su identificación numérica ID
     *
     * @param otherBook El otro objeto Libro al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Book otherBook) {
        return this.idBook - otherBook.getIdBook();
    }
}
