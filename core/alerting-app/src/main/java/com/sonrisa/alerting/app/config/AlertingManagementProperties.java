package com.sonrisa.alerting.app.config;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;

/**
 * Management group (Section 13.5, ADR-16): the operator credential for the Actuator endpoints on the
 * management port. The port and the exposed endpoints use Spring Boot's own {@code management.*} keys.
 *
 * <p>The password is a secret: set it only through {@code ALERTING_MANAGEMENT_OPERATOR_PASSWORD} or the
 * secret store. The deployed profiles ({@code demo}, {@code postgres}, {@code aws}) set
 * {@code operator-password-required}, so they refuse to start without it. In {@code local} and {@code test}
 * it is optional; without it, only the health probes are reachable. {@link #toString()} never prints it.
 *
 * @param operatorUsername user name of the operator, default {@code operator}
 * @param operatorPassword password of the operator
 * @param operatorPasswordRequired whether start-up fails when no password is set, default {@code false}
 */
@Validated
@ConfigurationProperties("alerting.management")
public record AlertingManagementProperties(
        @DefaultValue("operator") @NotBlank String operatorUsername,
        String operatorPassword,
        @DefaultValue("false") boolean operatorPasswordRequired) {

    /** Whether an operator password is configured. */
    public boolean hasOperatorPassword() {
        return operatorPassword != null && !operatorPassword.isBlank();
    }

    /**
     * Validation rule: a profile that requires the operator password must have one. The failure report
     * names this rule, never the password value.
     */
    @AssertTrue(message = "must be set through ALERTING_MANAGEMENT_OPERATOR_PASSWORD in this profile")
    public boolean isOperatorPasswordPresentWhenRequired() {
        return !operatorPasswordRequired || hasOperatorPassword();
    }

    @Override
    public String toString() {
        return "AlertingManagementProperties[operatorUsername=" + operatorUsername
                + ", operatorPassword=" + Secrets.mask(operatorPassword)
                + ", operatorPasswordRequired=" + operatorPasswordRequired + "]";
    }
}
