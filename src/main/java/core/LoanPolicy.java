package core;

import tables.Loan;

import java.time.LocalDate;

/**
 * Loan-period policy (SPEC-19): turns a configurable loan period into a due date and an
 * overdue test. Pure, locale-free, side-effect-free domain logic — it lives in the core so
 * every adapter (console, desktop, REST, MCP, AI panel) computes due dates and overdue
 * status identically.
 *
 * <p>The due date is <em>derived</em> from the stored loan date plus the period, so overdue
 * tracking needs no schema change to the legacy loans table; the period is the single
 * configurable knob ({@code loan-period-days}, default 21).
 *
 * @param loanPeriodDays the number of days a member may keep a borrowed book
 * @author GABI SDD pipeline (SPEC-19 loan due dates / overdue tracking)
 */
public record LoanPolicy(int loanPeriodDays) {

    /** A sensible default of three weeks when nothing is configured. */
    public static final int DEFAULT_PERIOD_DAYS = 21;

    public LoanPolicy {
        if (loanPeriodDays <= 0) {
            throw new IllegalArgumentException("loanPeriodDays must be positive: " + loanPeriodDays);
        }
    }

    /** The default three-week policy. */
    public static LoanPolicy defaultPolicy() {
        return new LoanPolicy(DEFAULT_PERIOD_DAYS);
    }

    /** The due date for a loan made on {@code loanDate}. */
    public LocalDate dueDate(LocalDate loanDate) {
        return loanDate.plusDays(loanPeriodDays);
    }

    /** The due date for a loan. */
    public LocalDate dueDate(Loan loan) {
        return dueDate(loan.getDateLoan());
    }

    /** Whether a loan made on {@code loanDate} is overdue as of {@code today}. */
    public boolean isOverdue(LocalDate loanDate, LocalDate today) {
        return today.isAfter(dueDate(loanDate));
    }

    /** Whether the loan is overdue as of {@code today}. */
    public boolean isOverdue(Loan loan, LocalDate today) {
        return isOverdue(loan.getDateLoan(), today);
    }
}
