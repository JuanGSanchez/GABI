package ui.desktop;

import core.LibraryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import ui.UiText;
import ui.info.InfoRegistry;
import ui.info.WidgetInfo;

import javax.swing.SwingUtilities;
import java.awt.GraphicsEnvironment;
import java.util.Locale;

/**
 * Launches the Swing desktop shell (SPEC-18) when the {@code desktop} profile is active:
 * <pre>java -jar gabi-1.0.0-exec.jar --spring.profiles.active=desktop</pre>
 *
 * <p>It is a sibling adapter to {@code GabiCliRunner} (console) and the {@code server}
 * profile (MCP + REST), all over the same {@code core.LibraryService}. The frame is built
 * and shown on the Event Dispatch Thread; the EDT then keeps the JVM alive while the
 * window is open.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
@Component
@Profile("desktop")
public class GabiDesktopRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GabiDesktopRunner.class);

    private final LibraryService service;

    @Value("${gabi.ui.locale:en}")
    private String localeTag;

    public GabiDesktopRunner(LibraryService service) {
        this.service = service;
    }

    @Override
    public void run(String... args) {
        if (GraphicsEnvironment.isHeadless()) {
            log.warn("Desktop profile active but the environment is headless — UI not started. "
                    + "Use the default CLI or the 'server' profile instead.");
            return;
        }
        Locale locale = Locale.forLanguageTag(localeTag);
        UiText text = UiText.of(locale);
        log.info("Launching GABI desktop UI (locale={})", localeTag);
        SwingUtilities.invokeLater(() -> {
            // SPEC-01: configure the single info surface once, before any widget registers.
            WidgetInfo.setRegistry(InfoRegistry.of(locale));
            WidgetInfo.installGlobalDefaults();
            MainFrame frame = new MainFrame(service, text);
            frame.setVisible(true);
        });
    }
}
