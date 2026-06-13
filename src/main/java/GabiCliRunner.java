import manager.LibMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * Bridges the Spring Boot lifecycle and the existing {@code manager.LibMenu} CLI.
 *
 * <p>After the Spring context finishes starting (all beans constructed, DataSource
 * wired, RAG beans initialised), this runner is called with the original program
 * arguments and delegates straight to {@code LibMenu.main(args)}.
 *
 * <p>This is the minimal-invasiveness approach: {@code LibMenu} is left untouched
 * except that it now operates inside a live Spring context.  The DAOs still use
 * their singleton pattern and per-call connections for the CLI path; the NEW
 * {@link core.LibraryServiceImpl} uses the Spring-managed {@code DataSource} for
 * the headless / MCP / RAG path.
 *
 * <p>CLI invocation is unchanged:
 * <pre>
 *   java -jar gabi.jar admin 1234
 * </pre>
 *
 * @author JuanGS (workstream b — CLI preservation)
 */
@Component
public class GabiCliRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GabiCliRunner.class);

    @Override
    public void run(String... args) {
        log.info("GABI Spring context ready — launching CLI");
        LibMenu.main(args);
    }
}
