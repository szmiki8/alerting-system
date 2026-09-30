package com.sonrisa.alerting.app.persistence;

import com.sonrisa.alerting.app.persistence.crypto.CryptoConfiguration;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

/**
 * Data JPA slice on the configured database with the schema from Flyway: H2 in PostgreSQL mode from the
 * {@code test} profile, not a replacement embedded database. Add
 * {@code @ImportTestcontainers(PostgresTestcontainer.class)} to run the same tests on PostgreSQL. Each test runs
 * in a transaction that is rolled back, so tests leave the shared PostgreSQL database unchanged. The crypto beans
 * are imported because the subscriber entity listener needs them (BE-09).
 *
 * <p>Put this annotation on the class that <em>declares</em> the test methods (for example an abstract class
 * shared by an H2 and a PostgreSQL subclass): Spring resolves the class-level {@code @Transactional} from the
 * declaring class of a test method.
 */
@Documented
@Inherited
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ActiveProfiles("test")
@Import(CryptoConfiguration.class)
public @interface RepositoryTest {
}
