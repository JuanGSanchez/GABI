package ui.desktop;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Generic, read-only {@link javax.swing.table.TableModel} that renders a list of
 * domain value-objects through a set of declared columns.
 *
 * <p>It is a pure view-adapter: it holds no business rule and never mutates the
 * catalogue — every change to the underlying data goes through {@code core.LibraryService}
 * and is reflected back by calling {@link #setRows(List)}. This keeps the SPEC-18
 * invariant that no business logic lives in the Swing layer.
 *
 * <p>{@code AbstractTableModel} can be instantiated and exercised without a display,
 * so this class is fully unit-testable in a headless environment.
 *
 * @param <T> the domain value-object type (Book, Member, Loan, User)
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class EntityTableModel<T> extends AbstractTableModel {

    /** A single column: a header label and a pure extractor from the row object. */
    public static final class Column<T> {
        private final String header;
        private final Function<T, Object> extractor;

        public Column(String header, Function<T, Object> extractor) {
            this.header = header;
            this.extractor = extractor;
        }

        public String header() {
            return header;
        }

        public Object valueOf(T row) {
            return extractor.apply(row);
        }
    }

    private final List<Column<T>> columns;
    private List<T> rows;

    public EntityTableModel(List<Column<T>> columns) {
        this.columns = List.copyOf(columns);
        this.rows = new ArrayList<>();
    }

    /** Replaces all rows and notifies listeners. */
    public void setRows(List<T> newRows) {
        this.rows = newRows == null ? new ArrayList<>() : new ArrayList<>(newRows);
        fireTableDataChanged();
    }

    /** Returns the row object at the given table row index. */
    public T getRow(int rowIndex) {
        return rows.get(rowIndex);
    }

    @Override
    public int getRowCount() {
        return rows.size();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).header();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        return columns.get(columnIndex).valueOf(rows.get(rowIndex));
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false; // read-only view; edits go through the core service
    }
}
