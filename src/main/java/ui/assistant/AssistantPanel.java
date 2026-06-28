package ui.assistant;

import ui.info.WidgetInfo;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;

/**
 * Dockable Swing AI assistant panel (SPEC-02).
 *
 * <p>A thin view over {@link AssistantService}: it sends the user's message and renders the
 * streamed reply incrementally on the Event Dispatch Thread. It owns no chat logic —
 * conversation memory, context injection, provider selection and graceful failure all live
 * in the service. Every interactive control registers its help through the centralized
 * SPEC-01 {@link WidgetInfo} surface.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public class AssistantPanel extends JPanel {

    private final transient AssistantService service;
    private final JTextArea transcript;
    private final JTextField input;
    private final JButton send;
    private final JButton clear;
    private final JCheckBox contextToggle;
    private final JLabel provider;

    public AssistantPanel(AssistantService service) {
        super(new BorderLayout(4, 4));
        this.service = service;
        setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        setPreferredSize(new Dimension(340, 0));

        this.transcript = new JTextArea();
        transcript.setEditable(false);
        transcript.setLineWrap(true);
        transcript.setWrapStyleWord(true);

        this.input = new JTextField();
        this.send = new JButton("Send");
        this.clear = new JButton("Clear");
        this.contextToggle = new JCheckBox("Use app context", service.isInjectContext());
        this.provider = new JLabel(providerLabel());

        send.addActionListener(e -> onSend());
        input.addActionListener(e -> onSend());
        clear.addActionListener(e -> onClear());
        contextToggle.addActionListener(e -> service.setInjectContext(contextToggle.isSelected()));

        registerInfo();

        add(buildHeader(), BorderLayout.NORTH);
        add(new JScrollPane(transcript), BorderLayout.CENTER);
        add(buildInputBar(), BorderLayout.SOUTH);
    }

    private JPanel buildHeader() {
        JPanel header = new JPanel(new BorderLayout());
        header.add(provider, BorderLayout.WEST);
        header.add(contextToggle, BorderLayout.EAST);
        return header;
    }

    private JPanel buildInputBar() {
        JPanel bar = new JPanel(new BorderLayout(4, 0));
        bar.add(input, BorderLayout.CENTER);
        JPanel buttons = new JPanel();
        buttons.add(send);
        buttons.add(clear);
        bar.add(buttons, BorderLayout.EAST);
        bar.add(Box.createVerticalStrut(2), BorderLayout.NORTH);
        return bar;
    }

    private void registerInfo() {
        WidgetInfo.register(input, "info.assistant.input");
        WidgetInfo.register(send, "info.assistant.send");
        WidgetInfo.register(clear, "info.assistant.clear");
        WidgetInfo.register(contextToggle, "info.assistant.context");
        WidgetInfo.register(provider, "info.assistant.provider");
    }

    private String providerLabel() {
        return "Provider: " + service.providerName();
    }

    private void onClear() {
        service.clear();
        transcript.setText("");
    }

    private void onSend() {
        String text = input.getText();
        if (text == null || text.isBlank()) {
            return;
        }
        input.setText("");
        append("\nYou: " + text + "\nAssistant: ");
        send.setEnabled(false);

        // Streamed chunks arrive on a reactive thread — marshal every update to the EDT.
        service.send(text, new ChatStreamHandler() {
            @Override
            public void onChunk(String chunk) {
                if (chunk != null) {
                    SwingUtilities.invokeLater(() -> append(chunk));
                }
            }

            @Override
            public void onComplete() {
                SwingUtilities.invokeLater(() -> {
                    append("\n");
                    send.setEnabled(true);
                });
            }

            @Override
            public void onError(Throwable error) {
                SwingUtilities.invokeLater(() -> {
                    append("\n[assistant unavailable: " + readable(error) + "]\n");
                    send.setEnabled(true);
                });
            }
        });
    }

    private static String readable(Throwable error) {
        String msg = error.getMessage();
        return (msg == null || msg.isBlank()) ? error.getClass().getSimpleName() : msg;
    }

    private void append(String s) {
        transcript.append(s);
        transcript.setCaretPosition(transcript.getDocument().getLength());
    }

    /** Refreshes the provider label after a runtime backend swap. */
    public void refreshProvider() {
        provider.setText(providerLabel());
    }
}
