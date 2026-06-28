package ui.assistant;

/**
 * Supplies an app-state context snapshot to inject into the assistant prompt (SPEC-02).
 *
 * <p>Implementations summarize the current application state (e.g. catalogue counts, the
 * selected entity) as plain text. The contract is strict: a snapshot MUST NOT contain any
 * credential, API key, or other secret — it is appended to the model prompt, so leaking a
 * secret here would exfiltrate it to the provider.
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
@FunctionalInterface
public interface AppStateProvider {

    /** A secret-free, human-readable snapshot of current app state, or empty string. */
    String snapshot();

    /** A provider that contributes no context. */
    static AppStateProvider none() {
        return () -> "";
    }
}
