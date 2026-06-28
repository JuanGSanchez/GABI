package ui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Locale-aware text accessor for the GABI desktop UI.
 *
 * <p>This is the UI/adapter-edge localization point mandated by SPEC-13: the headless
 * {@code core} returns locale-free values and typed exceptions, and every adapter
 * (console, desktop, REST, MCP) localizes at its own edge. The desktop UI localizes
 * through this class, which reuses the existing {@code statements*.properties} i18n
 * infrastructure (ES/EN) shared with the console adapter — no second catalogue.
 *
 * <p>It is deliberately Swing-free and headless-safe so its resolution logic can be
 * unit-tested without a display.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI / SPEC-13 i18n separation)
 */
public final class UiText {

    /** Base name of the shared i18n bundle family ({@code statements*.properties}). */
    public static final String BUNDLE = "statements";

    private final ResourceBundle bundle;
    private final Locale locale;

    private UiText(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle(BUNDLE, locale);
    }

    /** Returns a {@code UiText} bound to the given locale. */
    public static UiText of(Locale locale) {
        return new UiText(locale == null ? Locale.getDefault() : locale);
    }

    /** Returns a {@code UiText} bound to English. */
    public static UiText english() {
        return of(Locale.ENGLISH);
    }

    /** Returns a {@code UiText} bound to Spanish. */
    public static UiText spanish() {
        return of(new Locale("es"));
    }

    /** The locale this accessor resolves against. */
    public Locale locale() {
        return locale;
    }

    /**
     * Resolves a bundle key. Unknown keys fail fast (no silent missing-key fallback),
     * matching the SPEC-13 acceptance criterion that every key resolves in both locales.
     *
     * @throws MissingResourceException if the key is absent from the bundle
     */
    public String get(String key) {
        return bundle.getString(key);
    }

    /**
     * Resolves a key whose value is a {@link String#format} pattern and applies the
     * given arguments. Console catalogue values use {@code %s}/{@code %d} placeholders.
     */
    public String format(String key, Object... args) {
        return String.format(get(key), args);
    }

    /** Resolves a key, returning {@code fallback} instead of throwing when absent. */
    public String getOr(String key, String fallback) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            return fallback;
        }
    }
}
