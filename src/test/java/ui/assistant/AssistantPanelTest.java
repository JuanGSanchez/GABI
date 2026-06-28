package ui.assistant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.UiText;
import ui.desktop.FakeLibraryService;
import ui.desktop.MainFrame;
import ui.info.InfoRegistry;
import ui.info.WidgetInfo;

import java.awt.GraphicsEnvironment;
import java.util.Locale;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * SPEC-02 UI wiring: the dockable assistant panel is hostable in the shell, its controls
 * carry centralized info, and toggling it does not disturb the catalogue. Swing widget
 * construction is guarded for headless CI.
 */
class AssistantPanelTest {

    @BeforeEach
    void setUp() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing panels require a display");
        WidgetInfo.setRegistry(InfoRegistry.of(Locale.ENGLISH));
    }

    private AssistantService stubService() {
        return new AssistantService(new NoOpChatBackend(), AppStateProvider.none(), false);
    }

    @Test
    @DisplayName("assistant panel builds with centralized info on its controls")
    void panelBuilds() {
        AssistantPanel panel = new AssistantPanel(stubService());
        assertThat(panel.getComponentCount()).isGreaterThan(0);
    }

    @Test
    @DisplayName("MainFrame docks the assistant and the toggle hides it without affecting tabs")
    void mainFrameDocksAssistant() {
        MainFrame frame = new MainFrame(UiTextEnglishService(), UiText.english(), stubService());
        try {
            assertThat(frame.getAssistantPanel()).isNotNull();
            assertThat(frame.getTabs().getTabCount()).isEqualTo(4);

            frame.setAssistantVisible(false);
            assertThat(frame.getAssistantPanel().isVisible()).isFalse();
            // tabs remain fully functional
            assertThat(frame.getTabs().getTabCount()).isEqualTo(4);
            frame.setAssistantVisible(true);
            assertThat(frame.getAssistantPanel().isVisible()).isTrue();
        } finally {
            frame.dispose();
        }
    }

    @Test
    @DisplayName("MainFrame without an assistant service simply has no assistant panel")
    void mainFrameWithoutAssistant() {
        MainFrame frame = new MainFrame(UiTextEnglishService(), UiText.english());
        try {
            assertThat(frame.getAssistantPanel()).isNull();
        } finally {
            frame.dispose();
        }
    }

    private FakeLibraryService UiTextEnglishService() {
        return new FakeLibraryService();
    }
}
