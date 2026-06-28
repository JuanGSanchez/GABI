package access.mcp;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Registers the {@link LibraryMcpTools} bean as a Spring AI MCP
 * {@link ToolCallbackProvider}.
 *
 * <p>The Spring AI MCP server starter (spring-ai-starter-mcp-server-webmvc) picks up
 * all {@code ToolCallbackProvider} beans in the context and exposes them via the
 * configured transport (Streamable-HTTP at {@code /mcp} and optionally STDIO).
 *
 * <p>Using {@link MethodToolCallbackProvider} is equivalent to the annotation-scanner
 * auto-config path; this explicit bean ensures the tools are registered even if
 * {@code spring.ai.mcp.server.annotation-scanner.enabled} is {@code false} in some
 * profiles.
 *
 * @author JuanGS (workstream c — access layer)
 */
@Configuration
public class McpToolsConfig {

    /**
     * Exposes all {@code @Tool}-annotated methods on {@link LibraryMcpTools} to the
     * MCP server at the Streamable-HTTP endpoint ({@code /mcp}).
     */
    @Bean
    public ToolCallbackProvider libraryToolCallbackProvider(LibraryMcpTools tools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(tools)
                .build();
    }
}
