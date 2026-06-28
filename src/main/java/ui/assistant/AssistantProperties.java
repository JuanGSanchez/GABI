package ui.assistant;

/**
 * Runtime-selectable AI assistant settings (SPEC-02).
 *
 * <p>Holds the provider id, model name, and enabled flag — all chosen at <b>runtime</b>
 * (settings UI / external config / environment), never hardcoded. It deliberately holds
 * <b>no credential</b>: API keys are sourced by the underlying Spring AI {@code ChatClient}
 * from {@code ${OPENAI_API_KEY}} / {@code ${ANTHROPIC_API_KEY}} environment placeholders,
 * so no key literal ever lives in source or in this object (honoring the no-plaintext-
 * credential invariant / SPEC-08).
 *
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public final class AssistantProperties {

    private String provider;
    private String model;
    private boolean enabled;
    private boolean injectContext;

    public AssistantProperties() {
        this("ollama", "llama3.2", true, true);
    }

    public AssistantProperties(String provider, String model, boolean enabled, boolean injectContext) {
        this.provider = provider;
        this.model = model;
        this.enabled = enabled;
        this.injectContext = injectContext;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isInjectContext() {
        return injectContext;
    }

    public void setInjectContext(boolean injectContext) {
        this.injectContext = injectContext;
    }

    /** A short provider:model label for the UI; never includes any credential. */
    public String label() {
        return provider + ":" + model;
    }
}
