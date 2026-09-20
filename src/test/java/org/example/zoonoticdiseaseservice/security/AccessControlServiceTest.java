package org.example.zoonoticdiseaseservice.security;

import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AccessControlServiceTest {

    private final AccessControlService accessControlService =
            new AccessControlService();

    @Test
    void shouldAllowMatchingHazard() {
        assertDoesNotThrow(() ->
                accessControlService.checkHazardAccess(
                        "ZOONOTIC_DISEASE",
                        "ZOONOTIC_DISEASE"
                )
        );
    }

    @Test
    void shouldReturn403ForDifferentHazard() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessControlService.checkHazardAccess(
                        "ZOONOTIC_DISEASE",
                        "FLOOD"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldAllowMatchingWard() {
        assertDoesNotThrow(() ->
                accessControlService.checkWardAccess(
                        "Ward 5",
                        "Ward 5"
                )
        );
    }

    @Test
    void shouldReturn403ForDifferentWard() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessControlService.checkWardAccess(
                        "Ward 5",
                        "Ward 10"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldReturn403ForNationalWriteAccess() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> accessControlService.checkNationalWriteAccess(
                        "NATIONAL"
                )
        );

        assertEquals(403, exception.getStatusCode().value());
    }

    @Test
    void shouldAllowNonNationalWriteAccess() {
        assertDoesNotThrow(() ->
                accessControlService.checkNationalWriteAccess(
                        "WARD_RECORDER"
                )
        );
    }
}
