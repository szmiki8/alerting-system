package com.sonrisa.alerting.app.architecture.fixture;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;

/** Fixture: a JPA entity. */
@Entity
public class FixtureEntity {

    @Id
    Long id;
}
