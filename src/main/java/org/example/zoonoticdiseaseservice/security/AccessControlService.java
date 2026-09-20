package org.example.zoonoticdiseaseservice.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AccessControlService {

    public void checkHazardAccess(
            String userHazard,
            String requestedHazard) {

        if (userHazard == null ||
                requestedHazard == null ||
                !userHazard.equalsIgnoreCase(requestedHazard)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied for this hazard"
            );
        }
    }

    public void checkWardAccess(
            String userWard,
            String requestedWard) {

        if (userWard == null ||
                requestedWard == null ||
                !userWard.equalsIgnoreCase(requestedWard)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "Access denied for this ward"
            );
        }
    }

    public void checkNationalWriteAccess(
            String role) {

        if ("NATIONAL".equalsIgnoreCase(role)) {

            throw new ResponseStatusException(
                    HttpStatus.FORBIDDEN,
                    "National users have read-only access"
            );
        }
    }
}
