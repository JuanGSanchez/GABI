package core;

import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Spring configuration that replaces the production DataSource with an
 * in-memory Apache Derby datasource for tests.
 *
 * <p>URL: {@code jdbc:derby:memory:gabiTest;create=true}
 * <br>Driver: {@code org.apache.derby.jdbc.EmbeddedDriver} (test-scope dep)
 *
 * <p>The schema (books / members / loans / users tables) is created by
 * {@link TestSchemaHelper} — call its static methods in {@code @BeforeAll}
 * or {@code @BeforeEach} hooks in each test class.
 */
@Configuration
public class InMemoryDerbyConfig {

    static final String TEST_DB_URL = "jdbc:derby:memory:gabiTest;create=true";

    @Bean
    @Primary
    public DataSource testDataSource() {
        return DataSourceBuilder.create()
                .driverClassName("org.apache.derby.jdbc.EmbeddedDriver")
                .url(TEST_DB_URL)
                .username("")
                .password("")
                .build();
    }
}
