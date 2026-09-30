package com.sonrisa.alerting.app.architecture.fixture;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

/** Fixture: a controller that exposes an entity directly and inside a generic type (forbidden). */
@RestController
public class ControllerExposingEntity {

    public ResponseEntity<List<FixtureEntity>> find() {
        return ResponseEntity.ok(List.of());
    }

    public void save(FixtureEntity entity) {
    }
}
