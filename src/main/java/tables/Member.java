/**
 * Paquete de clases de las tablas de datos
 */
package tables;

import java.io.Serial;
import java.util.ArrayList;
import java.util.List;

/**
 * Clase de la tabla de datos de socios,
 * personas registradas que solicitan los libros
 *
 * @author JuanGS
 * @version 1.0
 */
public final class Member extends Entity implements Comparable<Member> {
    /**
     * Identificador de clase para serialización
     */
    @Serial
    private static final long serialVersionUID = 2311070003L;
    /**
     * Nombre del socio
     */
    private final String name;
    /**
     * Apellidos del socio
     */
    private final String surname;
    /**
     * Lista de libros prestados
     */
    private final List<Book> listBook;

    /**
     * Constructor completo de la clase
     * designado para creación de entrada
     *
     * @param ID      ID del socio
     * @param name    Nombre del socio
     * @param surname Apellidos del socio
     */
    public Member(int ID, String name, String surname) {
        this(ID, name, surname, null);
    }

    /**
     * Constructor completo de la clase
     * designado para recolección de entrada
     *
     * @param ID       ID del socio
     * @param name     Nombre del socio
     * @param surname  Apellidos del socio
     * @param listBook Lista de libros prestados
     */
    public Member(int ID, String name, String surname, List<Book> listBook) {
        super(ID);
        this.name = name;
        this.surname = surname;
        this.listBook = listBook;
    }

    /**
     * Método getter de la variable nombre
     *
     * @return Nombre del socio
     */
    public String getName() {
        return name;
    }

    /**
     * Método getter de la variable apellidos
     *
     * @return Apellidos del socio
     */
    public String getSurname() {
        return surname;
    }

    /**
     * Método getter de la variable libroList
     *
     * @return Lista de libros prestados
     */
    public List<Book> getListBook() {
        return listBook == null ? null : new ArrayList<>(listBook);
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

        Member member = (Member) o;

        if (!name.equalsIgnoreCase(member.name)) return false;
        return surname.equalsIgnoreCase(member.surname);
    }

    /**
     * Método sobreescrito de generación del código hash
     *
     * @return Código hash en base a las variables nombre
     * y apellidos del socio
     */
    @Override
    public int hashCode() {
        int result = name.hashCode();
        result = 31 * result + surname.hashCode();
        return result;
    }

    /**
     * Método sobreescrito de verbalización del objeto instanciado
     *
     * @return Descripción del socio
     */
    @Override
    public String toString() {
        return super.toString() + String.format("%-20s", name) + "  " + String.format("%-40s", surname);
    }

    /**
     * Método sobreescrito de ordenación de objetos Socio según
     * su identificación numérica ID
     *
     * @param otherMember El otro objeto Libro al que ser comparado
     * @return Entero de referencia para el proceso de ordenación
     */
    @Override
    public int compareTo(Member otherMember) {
        return this.getID() - otherMember.getID();
    }
}
