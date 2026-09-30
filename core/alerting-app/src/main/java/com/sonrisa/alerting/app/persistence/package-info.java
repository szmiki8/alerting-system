/**
 * Persistence (architecture Section 8, ADR-04): JPA entities and Spring Data repositories over a schema that
 * only Flyway migrations change ({@code src/main/resources/db/migration}). Hibernate validates the schema at
 * start-up. H2 in-memory (PostgreSQL mode) by default, PostgreSQL in the {@code postgres} profile.
 */
package com.sonrisa.alerting.app.persistence;
