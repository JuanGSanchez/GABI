package ui.info;

import javax.swing.AbstractAction;
import javax.swing.JComponent;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.util.Locale;

/**
 * The single centralized widget-info surface for GABI's desktop UI (SPEC-01).
 *
 * <p>This is the Java/Swing analogue of the FF-Explorer centralized info pattern, and it
 * closes that reference's three gaps:
 * <ul>
 *   <li><b>No inline literals:</b> {@link #register(JComponent, String)} is the only path
 *       that calls {@code setToolTipText}; all text comes from the single {@link InfoRegistry}.
 *       A grep gate asserts no other source file calls {@code setToolTipText}.</li>
 *   <li><b>Accessibility:</b> the same registry entry is also set as the component's
 *       {@code AccessibleContext} accessible description, and an F1 key binding exposes the
 *       help to keyboard-only users.</li>
 *   <li><b>Coverage enforcement:</b> registering an unknown key throws (fail-fast), so a
 *       coverage test catches any widget wired to a missing entry.</li>
 * </ul>
 *
 * <p><b>Single configuration points:</b> {@link #installGlobalDefaults()} is the one place
 * that sets the {@link ToolTipManager#sharedInstance()} global delays AND the {@code UIManager}
 * {@code ToolTip.*} styling keys. No component sets per-instance dismiss timing or per-component
 * tooltip styling.
 *
 * @author GABI SDD pipeline (SPEC-01 centralized widget-info popup)
 */
public final class WidgetInfo {

    private static InfoRegistry registry = InfoRegistry.of(Locale.getDefault());
    private static boolean globalsInstalled = false;

    private WidgetInfo() {
    }

    /** Sets the active registry (call once at UI startup with the chosen locale). */
    public static synchronized void setRegistry(InfoRegistry r) {
        registry = r;
    }

    /** The active registry (for coverage tests). */
    public static synchronized InfoRegistry registry() {
        return registry;
    }

    /**
     * Installs the GLOBAL tooltip behavior and styling in exactly one place:
     * the shared {@link ToolTipManager} delays and the {@code UIManager ToolTip.*} keys.
     * Idempotent.
     */
    public static synchronized void installGlobalDefaults() {
        if (globalsInstalled) {
            return;
        }
        ToolTipManager tip = ToolTipManager.sharedInstance();
        tip.setInitialDelay(400);
        tip.setDismissDelay(12000);
        tip.setReshowDelay(200);
        tip.setEnabled(true);

        // The single theming point — look comes ONLY from these UIManager keys.
        UIManager.put("ToolTip.background", new Color(0x2B2B2B));
        UIManager.put("ToolTip.foreground", new Color(0xF0F0F0));
        UIManager.put("ToolTip.font", new Font(Font.SANS_SERIF, Font.PLAIN, 12));

        globalsInstalled = true;
    }

    /**
     * The ONLY registration path. Sets BOTH the tooltip text and the accessible description
     * from the same registry entry, and binds F1 (when focused) to surface the info for
     * keyboard users.
     *
     * @param component the interactive widget
     * @param key       the stable widget id (must exist in the registry)
     * @throws InfoRegistry.UnknownInfoKeyException if the key is not registered (fail-fast)
     */
    public static void register(JComponent component, String key) {
        String help = registry.text(key); // throws fail-fast on unknown key
        component.setToolTipText(help);    // the single setToolTipText sink in the codebase
        component.getAccessibleContext().setAccessibleDescription(help);
        bindHelpKey(component, help);
    }

    private static void bindHelpKey(JComponent component, String help) {
        component.getInputMap(JComponent.WHEN_FOCUSED)
                .put(KeyStroke.getKeyStroke("F1"), "gabi-show-info");
        component.getActionMap().put("gabi-show-info", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                // Programmatically surface the same help text via the shared tooltip manager.
                component.putClientProperty("gabi.lastInfo", help);
                ToolTipManager.sharedInstance().mouseMoved(
                        new java.awt.event.MouseEvent(component, 0, 0, 0, 1, 1, 0, false));
            }
        });
    }
}
