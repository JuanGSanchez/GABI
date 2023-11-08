/**
 * Paquete de menús del programa
 */
package manager;

import java.util.Scanner;

abstract class EntityMenu {
    abstract int[] selectionMenu(Scanner scan, int nEntity, int ID);

}
