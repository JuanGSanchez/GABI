package ui.desktop;

import core.LibraryService;
import core.reports.ReportExporter;
import core.reports.ReportService;
import core.reports.ReportServiceImpl;
import ui.UiText;
import ui.assistant.AssistantPanel;
import ui.assistant.AssistantService;
import ui.info.WidgetInfo;

import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import java.awt.BorderLayout;

/**
 * The GABI Swing desktop shell (SPEC-18).
 *
 * <p>A single {@link JFrame} hosting one tab per catalogue entity — Books, Members,
 * Loans and Users-admin — each a thin {@link EntityPanel} over the shared
 * {@code core.LibraryService}. The menu bar mirrors the console structure and is the
 * mounting point for the centralized info popup (SPEC-01, Help ▸ Info) and the dockable
 * AI assistant panel (SPEC-02, Assistant menu).
 *
 * <p>All catalogue behavior lives in the core; this class only wires widgets to service
 * calls. The console adapter ({@code manager.LibMenu}) is retained in parallel.
 *
 * @author GABI SDD pipeline (SPEC-18 desktop UI)
 */
public class MainFrame extends JFrame {

    private final transient LibraryService service;
    private final transient UiText text;
    private final JTabbedPane tabs;

    private final BooksPanel booksPanel;
    private final MembersPanel membersPanel;
    private final LoansPanel loansPanel;
    private final UsersPanel usersPanel;

    private final transient AssistantService assistantService;
    private final transient ReportService reportService;
    private AssistantPanel assistantPanel;
    private JSplitPane splitPane;

    public MainFrame(LibraryService service, UiText text) {
        this(service, text, null);
    }

    public MainFrame(LibraryService service, UiText text, AssistantService assistantService) {
        super("GABI — " + text.getOr("program-name", "Library Manager"));
        this.service = service;
        this.text = text;
        this.assistantService = assistantService;
        this.reportService = new ReportServiceImpl(service);

        this.booksPanel = new BooksPanel(service, text);
        this.membersPanel = new MembersPanel(service, text);
        this.loansPanel = new LoansPanel(service, text);
        this.usersPanel = new UsersPanel(service, text);

        this.tabs = new JTabbedPane();
        tabs.addTab(text.getOr("program-properties-field-1-plural", "Books"), booksPanel);
        tabs.addTab(text.getOr("program-properties-field-2-plural", "Members"), membersPanel);
        tabs.addTab(text.getOr("program-properties-field-3-plural", "Loans"), loansPanel);
        tabs.addTab(text.getOr("program-properties-field-4-plural", "Users"), usersPanel);

        // SPEC-01: register tab-level info through the single centralized surface.
        WidgetInfo.register(booksPanel, "info.tab.books");
        WidgetInfo.register(membersPanel, "info.tab.members");
        WidgetInfo.register(loansPanel, "info.tab.loans");
        WidgetInfo.register(usersPanel, "info.tab.users");

        setJMenuBar(buildMenuBar());

        // SPEC-02: dock the assistant panel on the right when a service is supplied.
        if (assistantService != null) {
            this.assistantPanel = new AssistantPanel(assistantService);
            this.splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tabs, assistantPanel);
            splitPane.setResizeWeight(1.0);
            splitPane.setOneTouchExpandable(true);
            add(splitPane, BorderLayout.CENTER);
        } else {
            add(tabs, BorderLayout.CENTER);
        }

        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(assistantService != null ? 1240 : 900, 600);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu(text.getOr("program-intro-menu", "Menu"));
        JMenuItem exit = new JMenuItem(text.getOr("program-exit-2", "Exit"));
        exit.addActionListener(e -> dispose());
        WidgetInfo.register(exit, "info.menu.exit");
        fileMenu.add(exit);
        bar.add(fileMenu);

        // Reports menu — export catalogue/circulation reports as CSV (SPEC-21).
        JMenu reportsMenu = new JMenu("Reports");
        reportsMenu.setName("reports");
        for (String reportId : reportService.available()) {
            JMenuItem item = new JMenuItem("Export " + reportId + " (CSV)…");
            item.addActionListener(e -> exportReportCsv(reportId));
            reportsMenu.add(item);
        }
        bar.add(reportsMenu);

        // Assistant menu — toggles the dockable AI panel (SPEC-02). Closing it does not
        // affect any catalogue function; it can be reopened from here.
        if (assistantService != null) {
            JMenu aiMenu = new JMenu("Assistant");
            aiMenu.setName("assistant");
            JCheckBoxMenuItem toggle = new JCheckBoxMenuItem("Show assistant", true);
            toggle.setName("assistant-toggle");
            toggle.addActionListener(e -> setAssistantVisible(toggle.isSelected()));
            WidgetInfo.register(toggle, "info.assistant.provider");
            aiMenu.add(toggle);
            bar.add(aiMenu);
        }

        // Help menu — hosts the centralized widget-info affordance (SPEC-01).
        JMenu helpMenu = new JMenu(text.getOr("program-properties", "Help"));
        helpMenu.setName("help");
        WidgetInfo.register(helpMenu, "info.menu.help");
        bar.add(helpMenu);

        return bar;
    }

    /** Shows or hides the dockable assistant panel without disturbing the catalogue tabs. */
    public void setAssistantVisible(boolean visible) {
        if (assistantPanel == null || splitPane == null) {
            return;
        }
        assistantPanel.setVisible(visible);
        splitPane.setDividerLocation(visible ? 0.72 : 1.0);
        splitPane.revalidate();
    }

    /** The dockable assistant panel, or {@code null} when no assistant service is wired. */
    public AssistantPanel getAssistantPanel() {
        return assistantPanel;
    }

    /** Exports a report to a user-chosen CSV file (SPEC-21). */
    private void exportReportCsv(String reportId) {
        javax.swing.JFileChooser chooser = new javax.swing.JFileChooser();
        chooser.setSelectedFile(new java.io.File(reportId + ".csv"));
        if (chooser.showSaveDialog(this) != javax.swing.JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            String csv = ReportExporter.toCsv(reportService.byName(reportId));
            java.nio.file.Files.writeString(chooser.getSelectedFile().toPath(), csv);
        } catch (Exception ex) {
            javax.swing.JOptionPane.showMessageDialog(this, ex.getMessage(),
                    text.getOr("program-error-intro", "Error"), javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    /** The tabbed content pane (exposed for SPEC-01/SPEC-02 wiring and tests). */
    public JTabbedPane getTabs() {
        return tabs;
    }

    public BooksPanel getBooksPanel() {
        return booksPanel;
    }

    public MembersPanel getMembersPanel() {
        return membersPanel;
    }

    public LoansPanel getLoansPanel() {
        return loansPanel;
    }

    public UsersPanel getUsersPanel() {
        return usersPanel;
    }
}
