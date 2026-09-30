package com.sonrisa.alerting.archfixture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/**
 * Fixture: a JPA entity. Kept outside {@code com.sonrisa.alerting.app} so that Hibernate's entity scan (which
 * validates every entity against the Flyway schema) does not pick it up.
 */
@Entity
public class FixtureEntity {

    @Id
    public Long id;
}
