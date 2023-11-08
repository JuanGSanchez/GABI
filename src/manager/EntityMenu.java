/**
 * Paquete de menús del programa
 */
package manager;

import java.util.Scanner;

/**
 * Clase base general para los submenús del programa
 */
abstract class EntityMenu {
    /**
     * Método del menú principal del gestor, desde el cual se acceden
     * a las acciones disponibles
     *
     * @param scan Entrada de datos por teclado
     * @param nEntity Número de entidades guardados dentro de la base de datos
     * @param ID Máxima ID de la entidad dentro de la base de datos
     * @return Valores actualizados de nEntity y ID
     */
    abstract int[] selectionMenu(Scanner scan, int nEntity, int ID);

}
