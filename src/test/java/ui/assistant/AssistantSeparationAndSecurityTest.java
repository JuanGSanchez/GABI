package ui.assistant;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.*;

/**
 * SPEC-02 cross-cutting gates:
 * <ul>
 *   <li><b>Separation from MCP (SPEC-06):</b> the assistant path is ChatClient-only and
 *       references no MCP server / endpoint.</li>
 *   <li><b>No hardcoded secret (SPEC-08):</b> no API-key literal lives in the assistant
 *       sources; the context snapshot and provider label leak no secret.</li>
 *   <li><b>Graceful no-provider:</b> the NoOp backend reports not-ready and streams a clear
 *       configure-provider message instead of crashing.</li>
 * </ul>
 */
class AssistantSeparationAndSecurityTest {

    private static final Path ASSISTANT_SRC = Path.of("src", "main", "java", "ui", "assistant");

    private List<Path> assistantSources() throws IOException {
        try (Stream<Path> s = Files.walk(ASSISTANT_SRC)) {
            return s.filter(p -> p.toString().endsWith(".java")).toList();
        }
    }

    @Test
    @DisplayName("assistant sources reference ChatClient and never the MCP server/endpoint")
    void separationFromMcp() throws IOException {
        List<String> mcpRefs = new ArrayList<>();
        boolean usesChatClient = false;
        for (Path p : assistantSources()) {
            String src = Files.readString(p);
            if (src.contains("access.mcp") || src.contains("McpServer")
                    || src.contains("mcp.server") || src.contains("\"/mcp\"")) {
                mcpRefs.add(p.getFileName().toString());
            }
            if (src.contains("ChatClient")) {
                usesChatClient = true;
            }
        }
        assertThat(mcpRefs).as("assistant must not touch the MCP transport").isEmpty();
        assertThat(usesChatClient).as("assistant must use the outbound ChatClient path").isTrue();
    }

    @Test
    @DisplayName("no API-key literal appears in any assistant source")
    void noHardcodedSecret() throws IOException {
        // sk-.../key-shaped literals and inline api-key assignments to a non-placeholder
        Pattern keyLiteral = Pattern.compile("(?i)(sk-[A-Za-z0-9]{16,}|api[._-]?key\\s*[:=]\\s*\"(?!\\$\\{)[^\"]+\")");
        List<String> offenders = new ArrayList<>();
        for (Path p : assistantSources()) {
            if (keyLiteral.matcher(Files.readString(p)).find()) {
                offenders.add(p.getFileName().toString());
            }
        }
        assertThat(offenders).as("no committed API-key literal in assistant code").isEmpty();
    }

    @Test
    @DisplayName("provider label and properties expose no credential")
    void propertiesCarryNoSecret() {
        AssistantProperties props = new AssistantProperties("openai", "gpt-4o-mini", true, true);
        assertThat(props.label()).isEqualTo("openai:gpt-4o-mini");
        // AssistantProperties intentionally has no key/secret/password accessor
        boolean hasSecretAccessor = false;
        for (var m : AssistantProperties.class.getMethods()) {
            String n = m.getName().toLowerCase();
            if (n.contains("key") || n.contains("secret") || n.contains("password") || n.contains("token")) {
                hasSecretAccessor = true;
            }
        }
        assertThat(hasSecretAccessor).as("no credential accessor on AssistantProperties").isFalse();
    }

    @Test
    @DisplayName("NoOp backend is not ready and streams a clear configure-provider message")
    void noOpBackend() {
        NoOpChatBackend backend = new NoOpChatBackend();
        assertThat(backend.isReady()).isFalse();
        assertThat(backend.providerName()).isEqualTo("none");

        List<String> chunks = new ArrayList<>();
        boolean[] done = {false};
        backend.stream(List.of(ChatMessage.user("hi")), "", new ChatStreamHandler() {
            @Override public void onChunk(String c) { chunks.add(c); }
            @Override public void onComplete() { done[0] = true; }
            @Override public void onError(Throwable e) { }
        });
        assertThat(done[0]).isTrue();
        assertThat(String.join("", chunks).toLowerCase()).contains("provider");
    }

    @Test
    @DisplayName("a missing provider yields a usable assistant, not a crash")
    void missingProviderIsUsable() {
        AssistantService service = new AssistantService(
                new NoOpChatBackend(), AppStateProvider.none(), false);
        assertThat(service.isReady()).isFalse();
        List<String> chunks = new ArrayList<>();
        assertThatCode(() -> service.send("hi", new ChatStreamHandler() {
            @Override public void onChunk(String c) { chunks.add(c); }
            @Override public void onComplete() { }
            @Override public void onError(Throwable e) { }
        })).doesNotThrowAnyException();
        assertThat(chunks).isNotEmpty();
    }
}
