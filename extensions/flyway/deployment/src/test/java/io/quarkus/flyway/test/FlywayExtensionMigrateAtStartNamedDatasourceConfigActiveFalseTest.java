package io.quarkus.flyway.test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import jakarta.enterprise.inject.CreationException;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import io.quarkus.flyway.FlywayDataSource;
import io.quarkus.test.QuarkusUnitTest;

public class FlywayExtensionMigrateAtStartNamedDatasourceConfigActiveFalseTest {

    @RegisterExtension
    static final QuarkusUnitTest config = new QuarkusUnitTest()
            .withApplicationRoot((jar) -> jar
                    .addAsResource("db/migration/V1.0.0__Quarkus.sql"))
            .overrideConfigKey("quarkus.datasource.users.active", "false")
            .overrideConfigKey("quarkus.flyway.users.migrate-at-start", "true")
            // We need at least one build-time property for the datasource,
            // otherwise it's considered unconfigured at build time...
            .overrideConfigKey("quarkus.datasource.users.db-kind", "h2")
            // We need this otherwise the *default* datasource may impact this test
            .overrideConfigKey("quarkus.datasource.db-kind", "h2")
            .overrideConfigKey("quarkus.datasource.username", "sa")
            .overrideConfigKey("quarkus.datasource.password", "sa")
            .overrideConfigKey("quarkus.datasource.jdbc.url",
                    "jdbc:h2:tcp://localhost/mem:test-quarkus-migrate-at-start;DB_CLOSE_DELAY=-1");

    @Inject
    @FlywayDataSource("users")
    Instance<Flyway> flywayForNamedDatasource;

    @Test
    @DisplayName("If a named datasource is deactivated, even if migrate-at-start is enabled, the application should boot, but Flyway should be deactivated for that datasource")
    public void testBootSucceedsButFlywayDeactivated() {
        assertThatThrownBy(flywayForNamedDatasource::get)
                .isInstanceOf(CreationException.class)
                .cause()
                .hasMessageContainingAll("Unable to find datasource 'users' for Flyway",
                        "Datasource 'users' was deactivated through configuration properties.",
                        "To solve this, avoid accessing this datasource at runtime",
                        "Alternatively, activate the datasource by setting configuration property 'quarkus.datasource.\"users\".active'"
                                + " to 'true' and configure datasource 'users'",
                        "Refer to https://quarkus.io/guides/datasource for guidance.");
    }
}
