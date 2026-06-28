package ui;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Locale;
import java.util.MissingResourceException;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-13 / SPEC-18: the desktop edge localizes through {@link UiText} over the shared
 * {@code statements*.properties} bundles, resolving every key in both locales with no
 * silent missing-key fallback.
 */
class UiTextTest {

    @Test
    @DisplayName("resolves a key in English and Spanish")
    void resolvesBothLocales() {
        assertThat(UiText.english().get("program-book-properties-2")).isNotBlank();
        assertThat(UiText.spanish().get("program-book-properties-2")).isNotBlank();
    }

    @Test
    @DisplayName("format applies String.format arguments to a pattern value")
    void formatAppliesArgs() {
        String formatted = UiText.english().format("program-general-total", "7");
        assertThat(formatted).contains("7");
    }

    @Test
    @DisplayName("getOr returns the fallback for an unknown key")
    void getOrFallsBack() {
        assertThat(UiText.english().getOr("no-such-key-xyz", "fallback")).isEqualTo("fallback");
    }

    @Test
    @DisplayName("get fails fast on an unknown key (no silent missing-key fallback)")
    void getThrowsOnUnknownKey() {
        assertThatThrownBy(() -> UiText.english().get("no-such-key-xyz"))
                .isInstanceOf(MissingResourceException.class);
    }

    @Test
    @DisplayName("of(null) defaults to the platform locale and still resolves")
    void ofNullDefaults() {
        UiText t = UiText.of(null);
        assertThat(t.locale()).isNotNull();
        assertThat(t.getOr("program-name", "x")).isNotNull();
    }

    @Test
    @DisplayName("both ES and EN resolve the core book menu keys used by the desktop")
    void bothLocalesResolveMenuKeys() {
        for (Locale loc : new Locale[]{Locale.ENGLISH, new Locale("es")}) {
            UiText t = UiText.of(loc);
            for (int i = 1; i <= 4; i++) {
                assertThat(t.get("program-book-menu-" + i)).isNotBlank();
            }
        }
    }
}
