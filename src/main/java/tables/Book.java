/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serial;

/**
 * Clase de la tabla de datos de libros,
 * entidades susceptibles de prestarse
 *
 * @author JuanGS
 * @version 1.0
 */
public final class Book extends Entity implements Comparable<Book> {
    /**
     * Identificador de clase para serialización
     */
    @Serial
    private static final long serialVersionUID = 2311070002L;
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
     * @param ID     ID del libro
     * @param title  Título del libro
     * @param author Autor (principal) del libro
     */
    public Book(int ID, String title, String author) {
        this(ID, title, author, false);
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada
     *
     * @param ID     ID del libro
     * @param title  Título del libro
     * @param author Autor (principal) del libro
     * @param lent   Estado de disponibilidad del libro
     */
    public Book(int ID, String title, String author, boolean lent) {
        super(ID);
        this.title = title;
        this.author = author;
        this.lent = lent;
    }

    /**
     * Método getter de la variable título
     *
     * @return Título del libro
     */
    public String getTitle() {
        return title;
    }

    /**
     * Método getter de la variable autor
     *
     * @return Autor (principal) del libro
     */
    public String getAuthor() {
        return author;
    }

    /**
     * Método getter de la variable Prestado
     *
     * @return estado "prestado" del libro
     */
    public boolean isLent() {
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
        return super.toString() + String.format("%-120s", title) + "  ;  " + String.format("%-60s", author) + "  (%s)";
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
        return this.getID() - otherBook.getID();
    }
}
