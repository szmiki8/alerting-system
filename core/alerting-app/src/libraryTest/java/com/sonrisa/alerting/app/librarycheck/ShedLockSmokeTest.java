package com.sonrisa.alerting.app.librarycheck;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import net.javacrumbs.shedlock.core.LockConfiguration;
import net.javacrumbs.shedlock.core.SimpleLock;
import net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/** ShedLock 7 with the Spring JDBC provider grants a lock once and blocks a second holder. */
@SpringBootTest(classes = SmokeApplication.class)
class ShedLockSmokeTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void lockIsExclusiveUntilReleased() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS shedlock(name VARCHAR(64) NOT NULL, lock_until TIMESTAMP NOT NULL,"
                + " locked_at TIMESTAMP NOT NULL, locked_by VARCHAR(255) NOT NULL, PRIMARY KEY (name))");
        var provider = new JdbcTemplateLockProvider(JdbcTemplateLockProvider.Configuration.builder()
                .withJdbcTemplate(jdbcTemplate)
                .usingDbTime()
                .build());
        var config = new LockConfiguration(Instant.now(), "collection-run", Duration.ofMinutes(1), Duration.ZERO);

        Optional<SimpleLock> first = provider.lock(config);
        Optional<SimpleLock> second = provider.lock(config);

        assertThat(first).isPresent();
        assertThat(second).isEmpty();
        first.get().unlock();
        assertThat(provider.lock(config)).isPresent();
    }
}
