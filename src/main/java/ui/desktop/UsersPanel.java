package ui.desktop;

import core.LibraryService;
import tables.User;
import ui.UiText;

import java.util.List;

/**
 * Desktop adapter for DB-user administration. Mirrors the console {@code UserMenu}.
 *
 * <p><b>Privileged surface (SPEC-09):</b> the add/remove operations are admin-only.
 * This panel does <em>not</em> re-implement the authorization check — it delegates to
 * {@code core.LibraryService.addUser/deleteUser}, which routes through the single core
 * authorization gate. A non-admin caller is denied uniformly by the core, and the
 * resulting {@link core.LibraryException} surfaces here as an error dialog. The panel
 * never builds an identifier or SQL itself; identifier validation stays in the core.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class UsersPanel extends EntityPanel<User> {

    public UsersPanel(LibraryService service, UiText text) {
        super(service, text);
        refresh();
    }

    @Override
    protected List<EntityTableModel.Column<User>> columns() {
        return List.of(
                new EntityTableModel.Column<>(text.getOr("program-user-properties-1", "ID"), u -> u.getID()),
                new EntityTableModel.Column<>(text.getOr("program-user-properties-2", "name"), u -> u.getName()));
    }

    @Override
    protected List<User> loadRows() {
        return service.listUsers();
    }

    @Override
    protected String menuPrefix() {
        return "program-user-menu-";
    }

    @Override
    protected String entityWord() {
        return text.getOr("program-properties-field-4-singular", "User");
    }

    @Override
    protected String entityWordPlural() {
        return text.getOr("program-properties-field-4-plural", "Users");
    }

    @Override
    protected void onAdd() {
        String loginName = prompt(text.getOr("program-user-properties-2", "name"));
        if (loginName == null) {
            return;
        }
        char[] pw = promptMasked(text.getOr("program-user-properties-3", "password"));
        if (pw == null) {
            return;
        }
        try {
            int nextId = service.countUsers()[1] + 1;
            service.addUser(nextId, loginName, pw); // identifier validation + admin gate live in the core
        } finally {
            java.util.Arrays.fill(pw, '\0'); // never retain the credential in memory
        }
    }

    @Override
    protected void onRemove() {
        User selected = selectedRow();
        if (selected == null) {
            return;
        }
        service.deleteUser(selected.getID());
    }

    @Override
    protected List<User> searchRows() {
        // User search is not a console capability; the list view is the canonical view.
        return service.listUsers();
    }

    /**
     * Prompts via a Swing masked field so the value is never echoed and is collected as
     * {@code char[]} (caller zeroes it after use). Returns {@code null} on cancel/blank.
     */
    private char[] promptMasked(String message) {
        javax.swing.JPasswordField field = new javax.swing.JPasswordField();
        int ok = javax.swing.JOptionPane.showConfirmDialog(
                this, field, message, javax.swing.JOptionPane.OK_CANCEL_OPTION);
        if (ok != javax.swing.JOptionPane.OK_OPTION) {
            return null;
        }
        char[] entered = field.getPassword();
        return (entered.length == 0) ? null : entered;
    }
}
