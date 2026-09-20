package com.dpdms.fire_service.model;

/**
 * Fifth mandatory fire indicator: whether the blaze is still burning or under control.
 * Reported at capture time and updated as the situation evolves.
 */
public enum FireStatus {
    ACTIVE,
    CONTAINED
}
