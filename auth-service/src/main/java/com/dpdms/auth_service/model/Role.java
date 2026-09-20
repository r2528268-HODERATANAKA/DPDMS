package com.dpdms.auth_service.model;

// The three roles from the project brief. A user's role decides which claims their token
// carries and therefore what the hazard services will let them do:
//
//   WARD_RECORDER         - must have a CONCRETE ward + hazard (FR-SCOPE-01, no wildcards)
//   PROVINCIAL_SUPERVISOR - must have a hazard; reviews/approves records for that hazard
//   PROVINCIAL_ADMIN      - creates accounts and oversees the whole province
public enum Role {
    WARD_RECORDER,
    PROVINCIAL_SUPERVISOR,
    PROVINCIAL_ADMIN
}
