package com.sonrisa.alerting.app.architecture.fixture;

import com.sonrisa.alerting.archfixture.FixtureEntity;
import org.springframework.web.bind.annotation.RestController;

/** Fixture: a controller that maps the entity to a response type (allowed). */
@RestController
public class ControllerUsingDto {

    public record FixtureResponse(Long id) {
    }

    public FixtureResponse find() {
        FixtureEntity entity = new FixtureEntity();
        return new FixtureResponse(entity.id);
    }
}
