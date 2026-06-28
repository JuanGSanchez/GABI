package ui.info;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import ui.UiText;
import ui.desktop.BooksPanel;
import ui.desktop.FakeLibraryService;
import ui.desktop.MembersPanel;

import javax.swing.JButton;
import java.awt.GraphicsEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

/**
 * SPEC-01 acceptance gates:
 * <ul>
 *   <li>Static gate: {@code setToolTipText(} appears in NO source file except
 *       {@code WidgetInfo.java} — there is exactly one info surface.</li>
 *   <li>Coverage gate: every interactive widget on a panel carries a registered info
 *       key (tooltip + accessible description); a widget wired to a missing key would
 *       have thrown {@code UnknownInfoKeyException} at construction.</li>
 * </ul>
 */
class InfoCoverageGateTest {

    @Test
    @DisplayName("only WidgetInfo.java calls setToolTipText (no scattered inline tooltips)")
    void onlyHelperSetsTooltip() throws IOException {
        Path mainJava = Path.of("src", "main", "java");
        assertThat(mainJava).exists();
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> files = Files.walk(mainJava)) {
            files.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                try {
                    String src = Files.readString(p);
                    if (src.contains("setToolTipText(") && !p.getFileName().toString().equals("WidgetInfo.java")) {
                        offenders.add(p.toString());
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
        }
        assertThat(offenders)
                .as("setToolTipText must only be called by ui.info.WidgetInfo")
                .isEmpty();
    }

    @Test
    @DisplayName("every interactive toolbar widget has a registered info key (tooltip + accessible)")
    void everyWidgetHasInfo() {
        assumeFalse(GraphicsEnvironment.isHeadless(), "Swing panels require a display");
        WidgetInfo.setRegistry(InfoRegistry.of(Locale.ENGLISH));
        FakeLibraryService service = new FakeLibraryService();
        UiText text = UiText.english();

        for (String id : new String[]{"add", "list", "search", "remove"}) {
            JButton bookBtn = new BooksPanel(service, text).getComponentButton(id);
            assertThat(bookBtn.getToolTipText()).as("books " + id + " tooltip").isNotBlank();
            assertThat(bookBtn.getAccessibleContext().getAccessibleDescription())
                    .as("books " + id + " accessible").isNotBlank();

            JButton memberBtn = new MembersPanel(service, text).getComponentButton(id);
            assertThat(memberBtn.getToolTipText()).as("members " + id + " tooltip").isNotBlank();
        }
    }
}
