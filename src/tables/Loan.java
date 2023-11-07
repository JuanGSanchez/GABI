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
public class Loan implements Comparable<Loan>, Serializable {
    /**
     * ID del préstamo
     */
    private final int idLoan;
    /**
     * ID del socio
     */
    private final int idMember;
    /**
     * ID del libro
     */
    private final int idBook;
    /**
     * Fecha del préstamo
     */
    private final LocalDate dateLoan;
    /**
     * Libro asociado al préstamo
     */
    private final Book book;
    /**
     * Socio asociado al préstamo
     */
    private final Member member;

    /**
     * Constructor completo de la clase,
     * designado para creación de entrada
     *
     * @param idLoan   ID del préstamo
     * @param idMember ID del socio
     * @param idBook   ID del libro
     */
    public Loan(int idLoan, int idMember, int idBook) {
        this(idLoan, idMember, idBook, LocalDate.now());
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada
     *
     * @param idLoan   ID del préstamo
     * @param idMember ID del socio
     * @param idBook   ID del libro
     * @param dateLoan fecha del préstamo
     */
    public Loan(int idLoan, int idMember, int idBook, LocalDate dateLoan) {
        this(idLoan, idMember, idBook, dateLoan, null, null);
    }

    /**
     * Constructor completo de la clase,
     * designado para recolección de entrada con más detalle
     *
     * @param idLoan   ID del préstamo
     * @param idMember ID del socio
     * @param idBook   ID del libro
     * @param dateLoan fecha del préstamo
     * @param member   Socio asociado al préstamo
     * @param book     Libro asociado al préstamo
     */
    public Loan(int idLoan, int idMember, int idBook, LocalDate dateLoan, Member member, Book book) {
        this.idLoan = idLoan;
        this.idMember = idMember;
        this.idBook = idBook;
        this.dateLoan = dateLoan;
        this.member = member;
        this.book = book;
    }

    /**
     * Método getter del ID del préstamo
     *
     * @return ID del préstamo
     */
    public final int getIdLoan() {
        return idLoan;
    }

    /**
     * Método getter del ID del socio
     *
     * @return ID del socio
     */
    public final int getIdMember() {
        return idMember;
    }

    /**
     * Método getter del ID del libro
     *
     * @return ID del libro
     */
    public final int getIdBook() {
        return idBook;
    }

    /**
     * Método getter de la fecha del préstamo
     *
     * @return Fecha del préstamo
     */
    public final LocalDate getDateLoan() {
        return dateLoan;
    }

    /**
     * Método getter de la variable socio
     *
     * @return Socio asociado al préstamo
     */
    public Member getMember() {
        return member;
    }

    /**
     * Método getter de la variable libro
     *
     * @return Libro asociado al préstamo
     */
    public Book getBook() {
        return book;
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

        Loan loan = (Loan) o;

        if (idMember != loan.idMember) return false;
        if (idBook != loan.idBook) return false;
        return dateLoan.equals(loan.dateLoan);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las ID de socio y libro,
     * así como la fecha
     */
    @Override
    public int hashCode() {
        int result = idMember;
        result = 31 * result + idBook;
        result = 31 * result + dateLoan.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Información del préstamo
     */
    @Override
    public String toString() {
        return "%s " + idLoan + ": %s " + idBook + " %s " + idMember + " %s " + dateLoan.format(DateTimeFormatter.ISO_LOCAL_DATE);
    }

    /**
     * Método sobreescrito de ordenación de objetos Prestamo según
     * su identificación numérica ID
     *
     * @param otherLoan El otro objeto Prestamo al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Loan otherLoan) {
        return this.idLoan - otherLoan.getIdLoan();
    }
}
