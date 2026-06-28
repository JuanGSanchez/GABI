package ui.desktop;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Book;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-18: the generic table model is a pure, read-only view adapter — fully
 * exercisable without a display.
 */
class EntityTableModelTest {

    private EntityTableModel<Book> model() {
        return new EntityTableModel<>(List.of(
                new EntityTableModel.Column<>("ID", Book::getID),
                new EntityTableModel.Column<>("Title", Book::getTitle),
                new EntityTableModel.Column<>("Status", b -> b.isLent() ? "lent" : "available")));
    }

    @Test
    @DisplayName("columns are declared with headers")
    void columns() {
        EntityTableModel<Book> m = model();
        assertThat(m.getColumnCount()).isEqualTo(3);
        assertThat(m.getColumnName(0)).isEqualTo("ID");
        assertThat(m.getColumnName(1)).isEqualTo("Title");
    }

    @Test
    @DisplayName("setRows populates and getValueAt extracts via the column functions")
    void rowsAndValues() {
        EntityTableModel<Book> m = model();
        m.setRows(List.of(new Book(1, "Dune", "Herbert"), new Book(2, "It", "King", true)));
        assertThat(m.getRowCount()).isEqualTo(2);
        assertThat(m.getValueAt(0, 0)).isEqualTo(1);
        assertThat(m.getValueAt(0, 1)).isEqualTo("Dune");
        assertThat(m.getValueAt(0, 2)).isEqualTo("available");
        assertThat(m.getValueAt(1, 2)).isEqualTo("lent");
        assertThat(m.getRow(1).getTitle()).isEqualTo("It");
    }

    @Test
    @DisplayName("setRows(null) clears to empty")
    void setRowsNull() {
        EntityTableModel<Book> m = model();
        m.setRows(List.of(new Book(1, "Dune", "Herbert")));
        m.setRows(null);
        assertThat(m.getRowCount()).isZero();
    }

    @Test
    @DisplayName("the model is read-only (edits go through the core service)")
    void readOnly() {
        assertThat(model().isCellEditable(0, 0)).isFalse();
    }
}
