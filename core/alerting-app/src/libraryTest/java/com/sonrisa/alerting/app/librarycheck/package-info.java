/**
 * BE-02 smoke tests: each third-party library that later tasks adopt is resolved from the
 * version catalog and exercised once on the Java 17 toolchain together with Spring Boot 4.1.
 * These tests use their own classpath (the {@code libraryTest} suite), not the application's.
 */
package com.sonrisa.alerting.app.librarycheck;
