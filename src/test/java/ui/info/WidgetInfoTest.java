package ui.info;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.KeyStroke;
import javax.swing.ToolTipManager;
import javax.swing.UIManager;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-01: {@link WidgetInfo} is the single registration path and the single global
 * configuration point. Plain Swing components are constructable headless, so these
 * assertions run unconditionally.
 */
class WidgetInfoTest {

    @BeforeEach
    void setUp() {
        WidgetInfo.setRegistry(InfoRegistry.of(Locale.ENGLISH));
    }

    @Test
    @DisplayName("register sets tooltip AND accessible description from the same registry entry")
    void registerSetsTooltipAndAccessible() {
        JButton b = new JButton();
        WidgetInfo.register(b, "info.toolbar.add");
        String expected = InfoRegistry.of(Locale.ENGLISH).text("info.toolbar.add");
        assertThat(b.getToolTipText()).isEqualTo(expected);
        assertThat(b.getAccessibleContext().getAccessibleDescription()).isEqualTo(expected);
    }

    @Test
    @DisplayName("register binds F1 for keyboard accessibility")
    void registerBindsF1() {
        JButton b = new JButton();
        WidgetInfo.register(b, "info.toolbar.list");
        Object action = b.getInputMap(javax.swing.JComponent.WHEN_FOCUSED)
                .get(KeyStroke.getKeyStroke("F1"));
        assertThat(action).isEqualTo("gabi-show-info");
        assertThat(b.getActionMap().get("gabi-show-info")).isNotNull();
    }

    @Test
    @DisplayName("register with an unknown key fails fast")
    void registerUnknownKeyThrows() {
        JButton b = new JButton();
        assertThatThrownBy(() -> WidgetInfo.register(b, "info.unknown.key"))
                .isInstanceOf(InfoRegistry.UnknownInfoKeyException.class);
    }

    @Test
    @DisplayName("installGlobalDefaults sets the shared ToolTipManager delays and UIManager ToolTip.* keys")
    void globalDefaults() {
        WidgetInfo.installGlobalDefaults();
        ToolTipManager tip = ToolTipManager.sharedInstance();
        assertThat(tip.getDismissDelay()).isEqualTo(12000);
        assertThat(tip.getInitialDelay()).isEqualTo(400);
        // Styling comes ONLY from these UIManager keys (the single theming point).
        assertThat(UIManager.get("ToolTip.background")).isNotNull();
        assertThat(UIManager.get("ToolTip.foreground")).isNotNull();
        assertThat(UIManager.get("ToolTip.font")).isNotNull();
    }
}
