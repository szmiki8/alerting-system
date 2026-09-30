/**
 * Small test plugins (test sources only) that follow the plugin module convention described in
 * {@link com.sonrisa.alerting.app.plugin}: auto-configurations listed in
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports} of the test
 * resources, each off unless its {@code enabled} property is set. They live outside
 * {@code com.sonrisa.alerting.app} so that component scanning never picks them up.
 */
package com.sonrisa.alerting.testplugin;
