package core.search;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-20: pagination metadata math.
 */
class PageTest {

    @Test
    @DisplayName("totalPages rounds up and hasNext reflects remaining rows")
    void metadata() {
        Page<String> p0 = new Page<>(List.of("a", "b"), 0, 2, 5);
        assertThat(p0.totalPages()).isEqualTo(3);
        assertThat(p0.hasNext()).isTrue();

        Page<String> last = new Page<>(List.of("e"), 2, 2, 5);
        assertThat(last.hasNext()).isFalse();
    }

    @Test
    @DisplayName("empty result has zero pages; size<=0 is tolerated")
    void edges() {
        assertThat(new Page<>(List.of(), 0, 20, 0).totalPages()).isZero();
        assertThat(new Page<>(List.of("x"), 0, 0, 1).totalPages()).isEqualTo(1);
    }

    @Test
    @DisplayName("map preserves pagination metadata")
    void map() {
        Page<Integer> lengths = new Page<>(List.of("aa", "bbb"), 1, 2, 7).map(String::length);
        assertThat(lengths.content()).containsExactly(2, 3);
        assertThat(lengths.page()).isEqualTo(1);
        assertThat(lengths.totalElements()).isEqualTo(7);
    }
}
