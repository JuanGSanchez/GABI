package access.rest.dto;

import tables.Loan;

import java.time.LocalDate;

/**
 * Data-transfer object for {@link Loan}.
 *
 * @param id       unique loan identifier
 * @param memberId the borrowing member's ID
 * @param bookId   the borrowed book's ID
 * @param dateLoan date the loan was created (ISO-8601)
 *
 * @author JuanGS (workstream c — access layer)
 */
public record LoanDto(int id, int memberId, int bookId, LocalDate dateLoan) {

    /** Maps a domain {@link Loan} to its DTO representation. */
    public static LoanDto from(Loan l) {
        return new LoanDto(l.getID(), l.getIdMember(), l.getIdBook(), l.getDateLoan());
    }
}
