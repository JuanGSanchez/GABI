package ui.desktop;

import core.LibraryService;
import tables.Member;
import ui.UiText;

import java.util.List;

/**
 * Desktop adapter for the Members register. Mirrors the console {@code MemberMenu}
 * over the shared {@code core.LibraryService}.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class MembersPanel extends EntityPanel<Member> {

    public MembersPanel(LibraryService service, UiText text) {
        super(service, text);
        refresh();
    }

    @Override
    protected List<EntityTableModel.Column<Member>> columns() {
        return List.of(
                new EntityTableModel.Column<>(text.getOr("program-member-properties-1", "ID"), Member::getID),
                new EntityTableModel.Column<>(text.getOr("program-member-properties-2", "name"), Member::getName),
                new EntityTableModel.Column<>(text.getOr("program-member-properties-3", "surname"), Member::getSurname));
    }

    @Override
    protected List<Member> loadRows() {
        return service.listMembers();
    }

    @Override
    protected String menuPrefix() {
        return "program-member-menu-";
    }

    @Override
    protected String entityWord() {
        return text.getOr("program-properties-field-2-singular", "Member");
    }

    @Override
    protected String entityWordPlural() {
        return text.getOr("program-properties-field-2-plural", "Members");
    }

    @Override
    protected void onAdd() {
        String name = prompt(text.getOr("program-member-properties-2", "name"));
        if (name == null) {
            return;
        }
        String surname = prompt(text.getOr("program-member-properties-3", "surname"));
        if (surname == null) {
            return;
        }
        int nextId = service.countMembers()[1] + 1;
        service.addMember(nextId, name, surname);
    }

    @Override
    protected void onRemove() {
        Member selected = selectedRow();
        if (selected == null) {
            return;
        }
        service.deleteMember(selected.getID());
    }

    @Override
    protected List<Member> searchRows() {
        String field = prompt(text.getOr("program-general-criteria", "field (name/surname)"));
        if (field == null) {
            return null;
        }
        String fragment = prompt(text.getOr("program-general-search", "search"));
        if (fragment == null) {
            return null;
        }
        return service.searchMembers(field, fragment);
    }
}
