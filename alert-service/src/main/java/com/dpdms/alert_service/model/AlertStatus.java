package com.dpdms.alert_service.model;

// Result of one delivery attempt on one channel:
// SENT    - provider accepted the message
// FAILED  - provider returned an error (detail column holds the reason)
// SKIPPED - channel deliberately off (mock mode / not configured / no recipients)
public enum AlertStatus {
    SENT,
    FAILED,
    SKIPPED
}
