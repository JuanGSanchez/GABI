package core.reports;

import java.util.List;

/**
 * Builds catalogue/circulation reports from the core service (SPEC-21).
 *
 * <p>Reports cover the catalogue, active loans, overdue loans and loans-per-member. User
 * administration is intentionally NOT a report subject — the report surface never exposes
 * DB users or credentials.
 *
 * @author GABI SDD pipeline (SPEC-21 reports and data export)
 */
public interface ReportService {

    /** All books with availability status. */
    Report catalogue();

    /** All active loans with member/book ids and dates. */
    Report activeLoans();

    /** Loans past their due date, with the derived due date (SPEC-19). */
    Report overdueLoans();

    /** Per-member count of active loans. */
    Report loansPerMember();

    /** The stable ids of every available report. */
    List<String> available();

    /**
     * Builds a report by id.
     *
     * @throws IllegalArgumentException if the id is not a known report
     */
    Report byName(String name);
}
