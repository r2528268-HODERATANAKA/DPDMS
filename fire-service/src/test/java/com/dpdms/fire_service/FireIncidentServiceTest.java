package com.dpdms.fire_service;

import com.dpdms.fire_service.exception.ForbiddenOperationException;
import com.dpdms.fire_service.exception.ResourceNotFoundException;
import com.dpdms.fire_service.model.FireIncident;
import com.dpdms.fire_service.model.FireStatus;
import com.dpdms.fire_service.model.IncidentStatus;
import com.dpdms.fire_service.model.Severity;
import com.dpdms.fire_service.repository.FireIncidentRepository;
import com.dpdms.fire_service.service.AlertNotifier;
import com.dpdms.fire_service.service.FireIncidentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

// Unit tests for the Fire service rules (no database needed - the repository is mocked).
// These cover the marking-guide behaviours: scoping (403), workflow, and audit fields.
@ExtendWith(MockitoExtension.class)
class FireIncidentServiceTest {

    @Mock
    private FireIncidentRepository repository;

    @Mock
    private AlertNotifier alertNotifier;

    @InjectMocks
    private FireIncidentService service;

    private FireIncident sample;

    @BeforeEach
    void setUp() {
        sample = sampleIncident();
    }

    // A complete, valid fire incident used by most tests
    private FireIncident sampleIncident() {
        return FireIncident.builder()
                .ward("Mudzi").district("Mudzi").province("Mashonaland Central")
                .occurredAt(LocalDateTime.now())
                .reporter("T. Chirwa")
                .severity(Severity.HIGH)
                .latitude(-17.4).longitude(31.6)
                .areaBurnedHectares(45.5)
                .suspectedCause("Land clearing fire that spread")
                .injuries(1).fatalities(0).structuresDestroyed(2)
                .fireStatus(FireStatus.ACTIVE)
                .build();
    }

    @Test
    void createForcesPendingStatusRegardlessOfInput() {
        sample.setStatus(IncidentStatus.APPROVED); // recorder tries to sneak an approved record in
        when(repository.save(any(FireIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        FireIncident saved = service.create(sample, "Mudzi", "fire");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        verify(repository).save(sample);
    }

    @Test
    void createRejectsRecorderFromAnotherWard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Bindura", "fire"));
        assertTrue(ex.getMessage().contains("Mudzi"));
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsRecorderAssignedToAnotherHazard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Mudzi", "flood"));
        assertTrue(ex.getMessage().contains("fire"));
        verifyNoInteractions(repository);
    }

    @Test
    void updateResubmitsRecordAsPending() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(FireIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        FireIncident changes = sampleIncident();
        changes.setSeverity(Severity.CRITICAL);
        FireIncident saved = service.update(1L, changes, "Mudzi", "fire");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertEquals(Severity.CRITICAL, saved.getSeverity());
    }

    @Test
    void updateBlockedWhenRecordAlreadyApproved() {
        sample.setStatus(IncidentStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.update(1L, sampleIncident(), "Mudzi", "fire"));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteEnforcesWardScope() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.delete(1L, "Bindura", "fire"));
        verify(repository, never()).delete(any(FireIncident.class));
    }

    @Test
    void approveSetsStatusReviewerAndTimeAndNotifiesAlertService() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(FireIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        FireIncident approved = service.approve(1L, "Mrs Moyo", "fire");

        assertEquals(IncidentStatus.APPROVED, approved.getStatus());
        assertEquals("Mrs Moyo", approved.getReviewedBy());
        assertNotNull(approved.getReviewedAt());
        verify(alertNotifier).notifyApproved(approved); // alert-service gets told about it
    }

    @Test
    void approveBySupervisorOfDifferentHazardRejectedAtTheDoor() {
        // The scope check runs BEFORE any database lookup - rejected at the door.
        assertThrows(ForbiddenOperationException.class,
                () -> service.approve(1L, "Mrs Moyo", "flood"));
        verifyNoInteractions(repository);
        verifyNoInteractions(alertNotifier);
    }

    @Test
    void rejectRecordsReviewerReasonAndTime() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(FireIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        FireIncident rejected = service.reject(1L, "Mrs Moyo", "Duplicate report", "fire");

        assertEquals(IncidentStatus.REJECTED, rejected.getStatus());
        assertEquals("Mrs Moyo", rejected.getReviewedBy());
        assertEquals("Duplicate report", rejected.getReviewNotes());
    }

    @Test
    void requestCorrectionsRecordsNotesAndStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(FireIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        FireIncident returned = service.requestCorrections(1L, "Mrs Moyo", "Check the figures", "fire");

        assertEquals(IncidentStatus.CORRECTIONS_REQUESTED, returned.getStatus());
        assertEquals("Check the figures", returned.getReviewNotes());
    }

    @Test
    void findByIdReturnsIncident() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        FireIncident found = service.findById(1L);

        assertEquals(sample, found);
    }

    @Test
    void findByIdUnknownIdThrowsNotFound() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.findById(99L));
    }

    @Test
    void findAllApprovedReturnsOnlyApprovedRecords() {
        when(repository.findByStatus(IncidentStatus.APPROVED)).thenReturn(List.of(sample));

        List<FireIncident> result = service.findAllApproved();

        assertEquals(1, result.size());
        verify(repository).findByStatus(IncidentStatus.APPROVED);
    }
}
