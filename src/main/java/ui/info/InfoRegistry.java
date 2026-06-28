package ui.info;

import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.Set;

/**
 * The single widget-info registry for GABI's desktop UI (SPEC-01).
 *
 * <p>Backed by ONE {@code info*.properties} bundle family (locale-aware ES/EN), integrated
 * with the same i18n infrastructure as the rest of the UI. There is exactly one registry:
 * every interactive widget's help text — used for both its tooltip and its
 * {@code AccessibleContext} accessible description — is resolved here by a stable widget id.
 *
 * <p><b>Fail-fast:</b> {@link #text(String)} throws {@link UnknownInfoKeyException} for an
 * unregistered key, so a widget wired to a non-existent key fails loudly in a test rather
 * than silently rendering blank help.
 *
 * <p>This class is Swing-free and headless-safe so its resolution and coverage logic is
 * fully unit-testable.
 *
 * @author GABI SDD pipeline (SPEC-01 centralized widget-info popup)
 */
public final class InfoRegistry {

    /** Base name of the single info-registry bundle family. */
    public static final String BUNDLE = "info";

    /** Thrown when a widget is registered against a key absent from the registry. */
    public static final class UnknownInfoKeyException extends RuntimeException {
        public UnknownInfoKeyException(String key, Locale locale) {
            super("No widget-info entry for key '" + key + "' in locale '" + locale + "'. "
                    + "Add it to info.properties (and every locale variant).");
        }
    }

    private final ResourceBundle bundle;
    private final Locale locale;

    private InfoRegistry(Locale locale) {
        this.locale = locale;
        this.bundle = ResourceBundle.getBundle(BUNDLE, locale);
    }

    /** Returns a registry bound to the given locale (defaulting to the platform locale). */
    public static InfoRegistry of(Locale locale) {
        return new InfoRegistry(locale == null ? Locale.getDefault() : locale);
    }

    /** The locale this registry resolves against. */
    public Locale locale() {
        return locale;
    }

    /** True when the key has a registered entry. */
    public boolean has(String key) {
        return bundle.containsKey(key);
    }

    /**
     * Resolves the help text for a widget id.
     *
     * @throws UnknownInfoKeyException if the key is not registered (fail-fast)
     */
    public String text(String key) {
        try {
            return bundle.getString(key);
        } catch (MissingResourceException e) {
            throw new UnknownInfoKeyException(key, locale);
        }
    }

    /** All registered keys, for coverage tests that assert every widget id resolves. */
    public Set<String> keys() {
        Set<String> all = new HashSet<>();
        for (Enumeration<String> e = bundle.getKeys(); e.hasMoreElements(); ) {
            all.add(e.nextElement());
        }
        return Collections.unmodifiableSet(all);
    }
}
