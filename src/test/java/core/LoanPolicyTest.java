package core;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tables.Loan;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-19: due-date derivation and overdue testing are pure core logic.
 */
class LoanPolicyTest {

    @Test
    @DisplayName("due date is loan date plus the configured period")
    void dueDate() {
        LoanPolicy policy = new LoanPolicy(21);
        LocalDate loanDate = LocalDate.of(2026, 1, 1);
        assertThat(policy.dueDate(loanDate)).isEqualTo(LocalDate.of(2026, 1, 22));
        Loan loan = new Loan(1, 1, 1, loanDate);
        assertThat(policy.dueDate(loan)).isEqualTo(LocalDate.of(2026, 1, 22));
    }

    @Test
    @DisplayName("a loan is overdue only strictly after its due date")
    void overdueBoundary() {
        LoanPolicy policy = new LoanPolicy(7);
        LocalDate loanDate = LocalDate.of(2026, 1, 1);
        LocalDate due = policy.dueDate(loanDate); // 2026-01-08
        assertThat(policy.isOverdue(loanDate, due.minusDays(1))).isFalse();
        assertThat(policy.isOverdue(loanDate, due)).isFalse();          // due date itself is OK
        assertThat(policy.isOverdue(loanDate, due.plusDays(1))).isTrue();
        assertThat(policy.isOverdue(new Loan(1, 1, 1, loanDate), due.plusDays(1))).isTrue();
    }

    @Test
    @DisplayName("default policy is three weeks and a non-positive period is rejected")
    void defaultsAndValidation() {
        assertThat(LoanPolicy.defaultPolicy().loanPeriodDays()).isEqualTo(21);
        assertThatThrownBy(() -> new LoanPolicy(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new LoanPolicy(-5)).isInstanceOf(IllegalArgumentException.class);
    }
}
