package core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

/**
 * Configures the Spring-managed {@link DataSource} for the headless library core.
 *
 * <p>This DataSource is injected into {@link LibraryServiceImpl}. It replaces the
 * per-call {@code DriverManager.getConnection(url, user, password)} pattern of the
 * legacy singleton DAOs (D-3/D-7 fix). Connection credentials come from environment
 * variables ({@code DB_USER} / {@code DB_PASSWORD}) set via
 * {@code application.yml} Spring EL placeholders — never committed plaintext.
 *
 * <p>The {@code gabi.db.url} property defaults to the Derby network URL so the
 * legacy CLI continues to work against the same network Derby server as before.
 * Workstream (e) tests override this to {@code jdbc:derby:memory:gabiTest;create=true}
 * via the test {@code DataSource} bean (keeping the network server out of CI).
 *
 * @author JuanGS (workstream b — DataSource extraction)
 */
@Configuration
public class GabiDataSourceConfig {

    private static final Logger log = LoggerFactory.getLogger(GabiDataSourceConfig.class);

    /** Derby network-client driver class name. */
    private static final String DERBY_CLIENT_DRIVER = "org.apache.derby.jdbc.ClientDriver";

    @Value("${gabi.db.url:jdbc:derby://localhost:1527/LibraryDB}")
    private String dbUrl;

    @Value("${gabi.db.user:}")
    private String dbUser;

    @Value("${gabi.db.password:}")
    private String dbPassword;

    /**
     * Creates the Spring-managed DataSource.
     *
     * <p>If {@code DB_USER} / {@code DB_PASSWORD} env vars are set, they override
     * the {@code gabi.db.user} / {@code gabi.db.password} values from YAML (this
     * double-override is handled by {@code utils.Utils.readProperties()} for the
     * legacy CLI path; here we do the same for the Spring path by preferring the
     * env var if available).
     */
    @Bean
    public DataSource gabiDataSource() {
        String user = envOrFallback("DB_USER", dbUser);
        String pass = envOrFallback("DB_PASSWORD", dbPassword);

        if (user == null || user.isBlank()) {
            log.warn("GabiDataSource: DB_USER is not set — DataSource connections will use no credentials. " +
                     "Set the DB_USER environment variable for a live Derby server.");
        }

        return DataSourceBuilder.create()
                .driverClassName(DERBY_CLIENT_DRIVER)
                .url(dbUrl)
                .username(user)
                .password(pass)
                .build();
    }

    private String envOrFallback(String envVar, String fallback) {
        String val = System.getenv(envVar);
        return (val != null && !val.isBlank()) ? val : fallback;
    }
}
