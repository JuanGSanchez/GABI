package ui.assistant;

/**
 * One turn in an assistant conversation (SPEC-02). Locale-free and provider-agnostic.
 *
 * @param role    who produced the message
 * @param content the message text
 * @author GABI SDD pipeline (SPEC-02 AI assistant panel)
 */
public record ChatMessage(Role role, String content) {

    /** Conversation roles, mapped by each backend to its provider's message types. */
    public enum Role { USER, ASSISTANT, SYSTEM }

    public static ChatMessage user(String content) {
        return new ChatMessage(Role.USER, content);
    }

    public static ChatMessage assistant(String content) {
        return new ChatMessage(Role.ASSISTANT, content);
    }

    public static ChatMessage system(String content) {
        return new ChatMessage(Role.SYSTEM, content);
    }
}
