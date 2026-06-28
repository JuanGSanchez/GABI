package ui.info;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-01: exactly one info registry, locale-aware (ES/EN), every key resolves in both,
 * and an unknown key fails fast.
 */
class InfoRegistryTest {

    @Test
    @DisplayName("resolves a key in English and Spanish")
    void resolvesBothLocales() {
        assertThat(InfoRegistry.of(Locale.ENGLISH).text("info.toolbar.add")).isNotBlank();
        assertThat(InfoRegistry.of(new Locale("es")).text("info.toolbar.add")).isNotBlank();
    }

    @Test
    @DisplayName("unknown key throws (fail-fast)")
    void unknownKeyThrows() {
        assertThatThrownBy(() -> InfoRegistry.of(Locale.ENGLISH).text("info.nope"))
                .isInstanceOf(InfoRegistry.UnknownInfoKeyException.class)
                .hasMessageContaining("info.nope");
    }

    @Test
    @DisplayName("has() reflects key presence")
    void hasKey() {
        InfoRegistry r = InfoRegistry.of(Locale.ENGLISH);
        assertThat(r.has("info.toolbar.add")).isTrue();
        assertThat(r.has("info.nope")).isFalse();
    }

    @Test
    @DisplayName("every EN key resolves in ES and vice versa (locale parity)")
    void localeParity() {
        Set<String> en = keysOf(Locale.ENGLISH);
        Set<String> es = keysOf(new Locale("es"));
        InfoRegistry esReg = InfoRegistry.of(new Locale("es"));
        InfoRegistry enReg = InfoRegistry.of(Locale.ENGLISH);
        assertThat(en).isNotEmpty();
        for (String k : en) {
            assertThat(esReg.text(k)).as("ES must resolve EN key " + k).isNotBlank();
        }
        for (String k : es) {
            assertThat(enReg.text(k)).as("EN must resolve ES key " + k).isNotBlank();
        }
    }

    private Set<String> keysOf(Locale locale) {
        // read directly from the bundle variant to compare catalogues, bypassing fallback
        ResourceBundle b = ResourceBundle.getBundle(InfoRegistry.BUNDLE, locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        return b.keySet();
    }
}
