package ui.desktop;

import core.LibraryService;
import tables.Loan;
import ui.UiText;

import java.util.List;

/**
 * Desktop adapter for the Loans ledger. Mirrors the console {@code LoanMenu}
 * (new loan / list / search by member / return) over the shared core service.
 * The add and remove actions map to {@code createLoan} and {@code returnLoan}.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class LoansPanel extends EntityPanel<Loan> {

    public LoansPanel(LibraryService service, UiText text) {
        super(service, text);
        refresh();
    }

    @Override
    protected List<EntityTableModel.Column<Loan>> columns() {
        return List.of(
                new EntityTableModel.Column<>(text.getOr("program-loan-properties-1", "Loan ID"), Loan::getID),
                new EntityTableModel.Column<>(text.getOr("program-loan-properties-2", "Member ID"), Loan::getIdMember),
                new EntityTableModel.Column<>(text.getOr("program-loan-properties-3", "Book ID"), Loan::getIdBook),
                new EntityTableModel.Column<>(text.getOr("program-loan-properties-4", "loan date"), Loan::getDateLoan));
    }

    @Override
    protected List<Loan> loadRows() {
        return service.listLoans();
    }

    @Override
    protected String menuPrefix() {
        return "program-loan-menu-";
    }

    @Override
    protected String entityWord() {
        return text.getOr("program-properties-field-3-singular", "Loan");
    }

    @Override
    protected String entityWordPlural() {
        return text.getOr("program-properties-field-3-plural", "Loans");
    }

    @Override
    protected void onAdd() {
        Integer memberId = promptInt(text.getOr("program-loan-add-1", "member id"));
        if (memberId == null) {
            return;
        }
        Integer bookId = promptInt(text.getOr("program-loan-add-2", "book id"));
        if (bookId == null) {
            return;
        }
        int nextId = service.countLoans()[1] + 1;
        service.createLoan(nextId, memberId, bookId); // loan limit / lent-book rules in the core
    }

    @Override
    protected void onRemove() {
        Loan selected = selectedRow();
        if (selected == null) {
            return;
        }
        service.returnLoan(selected.getID());
    }

    @Override
    protected List<Loan> searchRows() {
        Integer memberId = promptInt(text.getOr("program-loan-properties-2", "Member ID"));
        if (memberId == null) {
            return null;
        }
        return service.listLoansByMember(memberId);
    }

    private Integer promptInt(String message) {
        String raw = prompt(message);
        if (raw == null) {
            return null;
        }
        try {
            return Integer.parseInt(raw.trim());
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
