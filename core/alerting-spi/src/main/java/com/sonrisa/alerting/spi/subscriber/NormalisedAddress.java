package com.sonrisa.alerting.spi.subscriber;

/**
 * The canonical form of a subscriber address, as returned by {@link SubscriberType#normalise}. Equal
 * addresses have equal values, which makes duplicate detection (FR-05) possible.
 *
 * <p>{@link #toString()} hides the value, because the address may be a secret (NFR-04). Use
 * {@link #value()} explicitly where the plain address is really needed.
 *
 * @param value the normalised address; not blank
 */
public record NormalisedAddress(String value) {

    public NormalisedAddress {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("value must not be blank");
        }
    }

    @Override
    public String toString() {
        return "NormalisedAddress[***]";
    }
}
