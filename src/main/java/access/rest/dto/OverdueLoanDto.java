package access.rest.dto;

import tables.Loan;

import java.time.LocalDate;

/**
 * Data-transfer object for an overdue {@link Loan} (SPEC-19): the loan plus its derived
 * due date and overdue flag. Read-only projection — carries no credential or write capability.
 *
 * @param id       unique loan identifier
 * @param memberId the borrowing member's ID
 * @param bookId   the borrowed book's ID
 * @param dateLoan date the loan was created (ISO-8601)
 * @param dueDate  the derived due date ({@code dateLoan + loan period})
 * @param overdue  whether the loan is past its due date
 *
 * @author GABI SDD pipeline (SPEC-19 loan due dates / overdue tracking)
 */
public record OverdueLoanDto(int id, int memberId, int bookId,
                             LocalDate dateLoan, LocalDate dueDate, boolean overdue) {

    /** Maps a domain {@link Loan} and its computed due date to the DTO. */
    public static OverdueLoanDto from(Loan l, LocalDate dueDate) {
        return new OverdueLoanDto(l.getID(), l.getIdMember(), l.getIdBook(),
                l.getDateLoan(), dueDate, true);
    }
}
