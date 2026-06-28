package ui.desktop;

import core.LibraryService;
import ui.UiText;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
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

    public MainFrame(LibraryService service, UiText text) {
        super("GABI — " + text.getOr("program-name", "Library Manager"));
        this.service = service;
        this.text = text;

        this.booksPanel = new BooksPanel(service, text);
        this.membersPanel = new MembersPanel(service, text);
        this.loansPanel = new LoansPanel(service, text);
        this.usersPanel = new UsersPanel(service, text);

        this.tabs = new JTabbedPane();
        tabs.addTab(text.getOr("program-properties-field-1-plural", "Books"), booksPanel);
        tabs.addTab(text.getOr("program-properties-field-2-plural", "Members"), membersPanel);
        tabs.addTab(text.getOr("program-properties-field-3-plural", "Loans"), loansPanel);
        tabs.addTab(text.getOr("program-properties-field-4-plural", "Users"), usersPanel);

        setJMenuBar(buildMenuBar());
        add(tabs, BorderLayout.CENTER);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setSize(900, 600);
        setLocationRelativeTo(null);
    }

    private JMenuBar buildMenuBar() {
        JMenuBar bar = new JMenuBar();

        JMenu fileMenu = new JMenu(text.getOr("program-intro-menu", "Menu"));
        JMenuItem exit = new JMenuItem(text.getOr("program-exit-2", "Exit"));
        exit.addActionListener(e -> dispose());
        fileMenu.add(exit);
        bar.add(fileMenu);

        // Help menu — hosts the centralized widget-info affordance (SPEC-01 wires it here).
        JMenu helpMenu = new JMenu(text.getOr("program-properties", "Help"));
        helpMenu.setName("help");
        bar.add(helpMenu);

        return bar;
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
