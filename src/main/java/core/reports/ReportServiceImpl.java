package core.reports;

import core.LibraryService;
import org.springframework.stereotype.Service;
import tables.Book;
import tables.Loan;
import tables.Member;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Default {@link ReportService} over {@code core.LibraryService} (SPEC-21).
 *
 * <p>Each builder is a pure projection of catalogue/circulation data into a {@link Report};
 * it holds no business rule and never reads credentials. Overdue rows use the core
 * {@code dueDate}/overdue policy (SPEC-19).
 *
 * @author GABI SDD pipeline (SPEC-21 reports and data export)
 */
@Service
public class ReportServiceImpl implements ReportService {

    private final transient LibraryService library;

    public ReportServiceImpl(LibraryService library) {
        this.library = library;
    }

    @Override
    public Report catalogue() {
        List<List<String>> rows = new ArrayList<>();
        for (Book b : library.listBooks()) {
            rows.add(List.of(
                    String.valueOf(b.getID()),
                    nz(b.getTitle()),
                    nz(b.getAuthor()),
                    b.isLent() ? "lent" : "available"));
        }
        return new Report("catalogue", List.of("id", "title", "author", "status"), rows);
    }

    @Override
    public Report activeLoans() {
        List<List<String>> rows = new ArrayList<>();
        for (Loan l : library.listLoans()) {
            rows.add(List.of(
                    String.valueOf(l.getID()),
                    String.valueOf(l.getIdMember()),
                    String.valueOf(l.getIdBook()),
                    String.valueOf(l.getDateLoan()),
                    String.valueOf(library.dueDate(l))));
        }
        return new Report("active-loans",
                List.of("loanId", "memberId", "bookId", "loanDate", "dueDate"), rows);
    }

    @Override
    public Report overdueLoans() {
        List<List<String>> rows = new ArrayList<>();
        for (Loan l : library.listOverdueLoans()) {
            rows.add(List.of(
                    String.valueOf(l.getID()),
                    String.valueOf(l.getIdMember()),
                    String.valueOf(l.getIdBook()),
                    String.valueOf(l.getDateLoan()),
                    String.valueOf(library.dueDate(l))));
        }
        return new Report("overdue-loans",
                List.of("loanId", "memberId", "bookId", "loanDate", "dueDate"), rows);
    }

    @Override
    public Report loansPerMember() {
        Map<Integer, Integer> counts = new LinkedHashMap<>();
        for (Member m : library.listMembers()) {
            counts.put(m.getID(), 0);
        }
        for (Loan l : library.listLoans()) {
            counts.merge(l.getIdMember(), 1, Integer::sum);
        }
        List<List<String>> rows = new ArrayList<>();
        counts.forEach((memberId, count) ->
                rows.add(List.of(String.valueOf(memberId), String.valueOf(count))));
        return new Report("loans-per-member", List.of("memberId", "activeLoans"), rows);
    }

    @Override
    public List<String> available() {
        return List.of("catalogue", "active-loans", "overdue-loans", "loans-per-member");
    }

    @Override
    public Report byName(String name) {
        return switch (name == null ? "" : name) {
            case "catalogue" -> catalogue();
            case "active-loans" -> activeLoans();
            case "overdue-loans" -> overdueLoans();
            case "loans-per-member" -> loansPerMember();
            default -> throw new IllegalArgumentException("Unknown report: " + name);
        };
    }

    private static String nz(String s) {
        return s == null ? "" : s;
    }
}
