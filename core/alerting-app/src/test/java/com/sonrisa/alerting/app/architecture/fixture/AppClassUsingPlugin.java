package com.sonrisa.alerting.app.architecture.fixture;

import com.sonrisa.alerting.source.archfixture.FakeSource;

/** Fixture: application code that uses a concrete plugin class (forbidden). */
public class AppClassUsingPlugin {

    final FakeSource source = new FakeSource();
}
