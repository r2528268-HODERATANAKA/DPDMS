package com.dpdms.fire_service.model;

/**
 * Contract implemented by every hazard-specific severity scale.
 *
 * OOP concept: POLYMORPHISM (interface). The alert/report engines rank incidents through
 * this interface without knowing which hazard's scale they are ranking.
 */
public interface SeverityScale {

    /** @return 1 (lowest) .. 5 (highest) position on the scale. */
    int rank();

    /** @return true when this severity must trigger immediate alert fan-out. */
    boolean triggersImmediateAlert();
}
