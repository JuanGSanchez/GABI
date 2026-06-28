import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot bootstrap entry point for GABI.
 *
 * <p>Introduces the Spring context needed by:
 * <ul>
 *   <li>Spring AI embedding + chat model beans (RAG pipeline, workstream b).</li>
 *   <li>Spring-managed {@code DataSource} (replaces per-call credentials, D-3/D-7 fix).</li>
 *   <li>The MCP/REST access layer (workstream c, Spring AI MCP + WebMVC).</li>
 * </ul>
 *
 * <p><b>CLI preservation:</b> the existing library-manager CLI ({@code manager.LibMenu})
 * is driven by {@link GabiCliRunner}, a {@code CommandLineRunner} bean registered in
 * this context. When the Spring context starts, it immediately hands off to
 * {@code LibMenu}'s interactive loop, which runs until the user exits. This keeps
 * 100% backward compatibility: {@code java -jar gabi.jar admin 1234} still works
 * exactly as before.
 *
 * <p><b>Alternative invocation modes:</b>
 * <ul>
 *   <li>Default (no profile): CLI with local Ollama RAG (no API key needed).</li>
 *   <li>{@code -Dspring.ai.ollama.enabled=false}: CLI only, RAG disabled (no-op stub).</li>
 *   <li>Workstream (c) will add a server profile that starts MCP + REST without CLI.</li>
 * </ul>
 *
 * @author JuanGS (workstream b — Spring Boot bootstrapping)
 */
@SpringBootApplication(scanBasePackages = {"manager", "sql", "tables", "utils", "core", "rag", "access", "ui"})
public class GabiApplication {

    public static void main(String[] args) {
        SpringApplication.run(GabiApplication.class, args);
    }
}
