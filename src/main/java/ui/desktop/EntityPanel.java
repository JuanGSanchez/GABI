package ui.desktop;

import core.LibraryException;
import core.LibraryService;
import ui.UiText;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import java.awt.BorderLayout;
import java.util.List;

/**
 * Base class for the desktop entity panels (Books / Members / Loans / Users).
 *
 * <p>Each panel is a <em>thin adapter</em> over {@code core.LibraryService}: it renders
 * rows in a {@link EntityTableModel}, exposes the same operations as the console menus
 * (add / delete / search / list / total), and maps {@link LibraryException} subtypes to
 * user-facing dialogs at this edge. It carries <b>no business rule</b> — every rule
 * (duplicate checks, loan limits, lent-book guard, admin gating) stays in the core.
 *
 * <p>The widget tree is built with lightweight Swing components only, so the panel can
 * be constructed in a headless environment for unit tests; modal dialogs are only ever
 * raised by interactive button actions, never during construction.
 *
 * @param <T> the domain value-object type
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public abstract class EntityPanel<T> extends JPanel {

    protected final transient LibraryService service;
    protected final transient UiText text;
    protected final EntityTableModel<T> model;
    protected final JTable table;
    protected final JToolBar toolbar;
    protected final JLabel status;

    protected EntityPanel(LibraryService service, UiText text) {
        super(new BorderLayout(4, 4));
        this.service = service;
        this.text = text;
        this.model = new EntityTableModel<>(columns());
        this.table = new JTable(model);
        this.toolbar = new JToolBar();
        this.toolbar.setFloatable(false);
        this.status = new JLabel(" ");

        buildToolbar();
        add(toolbar, BorderLayout.NORTH);
        add(new JScrollPane(table), BorderLayout.CENTER);
        add(status, BorderLayout.SOUTH);
    }

    /** Column definitions for this entity's table. */
    protected abstract List<EntityTableModel.Column<T>> columns();

    /** Loads the current rows from the core service (no caching, always live). */
    protected abstract List<T> loadRows();

    /** i18n key prefix for this entity's menu labels (e.g. {@code "program-book-menu-"}). */
    protected abstract String menuPrefix();

    /** Hook for the add action. */
    protected abstract void onAdd();

    /** Hook for the delete action. */
    protected abstract void onRemove();

    /**
     * Hook for the search action: prompts the user and returns the matching rows,
     * or {@code null} if the user cancelled. Never mutates the catalogue.
     */
    protected abstract List<T> searchRows();

    /** Singular display word for this entity. */
    protected abstract String entityWord();

    /** Plural display word for this entity. */
    protected abstract String entityWordPlural();

    /** Reloads the table from the live catalogue. */
    public void refresh() {
        try {
            List<T> rows = loadRows();
            model.setRows(rows);
            status.setText(text.format("program-general-total", String.valueOf(rows.size())));
        } catch (LibraryException ex) {
            model.setRows(List.of());
            status.setText(ex.getMessage());
        }
    }

    /** The currently selected row, or {@code null} if none. */
    protected T selectedRow() {
        int viewRow = table.getSelectedRow();
        if (viewRow < 0) {
            return null;
        }
        return model.getRow(table.convertRowIndexToModel(viewRow));
    }

    /** Builds a localized menu-label key for slot {@code n} without inline SQL-like concatenation. */
    private String menuKey(int n) {
        return menuPrefix() + n;
    }

    private void buildToolbar() {
        toolbar.add(newButton("add", text.format(menuKey(1), entityWord())));
        getComponentButton("add").addActionListener(e -> guarded(this::onAdd));
        toolbar.add(newButton("list", text.format(menuKey(2), entityWordPlural())));
        getComponentButton("list").addActionListener(e -> refresh());
        toolbar.add(newButton("search", text.format(menuKey(3), entityWord())));
        getComponentButton("search").addActionListener(e -> runSearch());
        toolbar.add(newButton("remove", text.format(menuKey(4), entityWord())));
        getComponentButton("remove").addActionListener(e -> guarded(this::onRemove));
    }

    /** Factory for a named action button; the name is the SPEC-01 info key suffix. */
    protected JButton newButton(String id, String label) {
        JButton b = new JButton(label);
        b.setName(id);
        toolbarButtons.put(id, b);
        return b;
    }

    private final java.util.Map<String, JButton> toolbarButtons = new java.util.HashMap<>();

    /** Returns a toolbar button by its registered id (for wiring + info registration). */
    public JButton getComponentButton(String id) {
        return toolbarButtons.get(id);
    }

    /** Runs the search hook and shows the matching rows (a filtered view, not a full reload). */
    protected void runSearch() {
        try {
            List<T> results = searchRows();
            if (results == null) {
                return; // cancelled
            }
            model.setRows(results);
            status.setText(text.format("program-general-total", String.valueOf(results.size())));
        } catch (LibraryException ex) {
            showError(ex);
        }
    }

    /** Runs an action, converting any {@link LibraryException} to an error dialog. */
    protected void guarded(Runnable action) {
        try {
            action.run();
            refresh();
        } catch (LibraryException ex) {
            showError(ex);
        }
    }

    /** Shows a typed-error dialog (no stack trace, no secret). */
    protected void showError(LibraryException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(),
                text.getOr("program-error-intro", "Error"), JOptionPane.ERROR_MESSAGE);
    }

    /** Prompts for a non-blank string; returns {@code null} if cancelled. */
    protected String prompt(String message) {
        String v = JOptionPane.showInputDialog(this, message);
        return (v == null || v.isBlank()) ? null : v.trim();
    }
}
