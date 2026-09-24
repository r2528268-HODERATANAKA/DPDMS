package com.dpdms.zoonotic_disease_service;

import com.dpdms.zoonotic_disease_service.exception.ForbiddenOperationException;
import com.dpdms.zoonotic_disease_service.exception.ResourceNotFoundException;
import com.dpdms.zoonotic_disease_service.model.ZoonoticIncident;
import com.dpdms.zoonotic_disease_service.model.OutbreakStatus;
import com.dpdms.zoonotic_disease_service.model.IncidentStatus;
import com.dpdms.zoonotic_disease_service.model.Severity;
import com.dpdms.zoonotic_disease_service.repository.ZoonoticIncidentRepository;
import com.dpdms.zoonotic_disease_service.service.AlertNotifier;
import com.dpdms.zoonotic_disease_service.service.ZoonoticIncidentService;
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

// Unit tests for the Zoonotic disease service rules (no database needed - the repository is mocked).
// These cover the marking-guide behaviours: scoping (403), workflow, and audit fields.
@ExtendWith(MockitoExtension.class)
class ZoonoticIncidentServiceTest {

    @Mock
    private ZoonoticIncidentRepository repository;

    @Mock
    private AlertNotifier alertNotifier;

    @InjectMocks
    private ZoonoticIncidentService service;

    private ZoonoticIncident sample;

    @BeforeEach
    void setUp() {
        sample = sampleIncident();
    }

    // A complete, valid zoonotic incident used by most tests
    private ZoonoticIncident sampleIncident() {
        return ZoonoticIncident.builder()
                .ward("Mudzi").district("Mudzi").province("Mashonaland Central")
                .occurredAt(LocalDateTime.now())
                .reporter("T. Chirwa")
                .severity(Severity.HIGH)
                .latitude(-17.4).longitude(31.6)
                .diseaseName("Anthrax")
                .suspectedAnimalSpecies("Cattle")
                .humanCases(3).humanDeaths(0).animalsAffected(5)
                .outbreakStatus(OutbreakStatus.CONFIRMED)
                .build();
    }

    @Test
    void createForcesPendingStatusRegardlessOfInput() {
        sample.setStatus(IncidentStatus.APPROVED); // recorder tries to sneak an approved record in
        when(repository.save(any(ZoonoticIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticIncident saved = service.create(sample, "Mudzi", "zoonotic");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        verify(repository).save(sample);
    }

    @Test
    void createRejectsRecorderFromAnotherWard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Bindura", "zoonotic"));
        assertTrue(ex.getMessage().contains("Mudzi"));
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsRecorderAssignedToAnotherHazard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Mudzi", "flood"));
        assertTrue(ex.getMessage().contains("zoonotic"));
        verifyNoInteractions(repository);
    }

    @Test
    void updateResubmitsRecordAsPending() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(ZoonoticIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticIncident changes = sampleIncident();
        changes.setSeverity(Severity.CRITICAL);
        ZoonoticIncident saved = service.update(1L, changes, "Mudzi", "zoonotic");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertEquals(Severity.CRITICAL, saved.getSeverity());
    }

    @Test
    void updateBlockedWhenRecordAlreadyApproved() {
        sample.setStatus(IncidentStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.update(1L, sampleIncident(), "Mudzi", "zoonotic"));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteEnforcesWardScope() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.delete(1L, "Bindura", "zoonotic"));
        verify(repository, never()).delete(any(ZoonoticIncident.class));
    }

    @Test
    void approveSetsStatusReviewerAndTimeAndNotifiesAlertService() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(ZoonoticIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticIncident approved = service.approve(1L, "Mrs Moyo", "zoonotic");

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
        when(repository.save(any(ZoonoticIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticIncident rejected = service.reject(1L, "Mrs Moyo", "Duplicate report", "zoonotic");

        assertEquals(IncidentStatus.REJECTED, rejected.getStatus());
        assertEquals("Mrs Moyo", rejected.getReviewedBy());
        assertEquals("Duplicate report", rejected.getReviewNotes());
    }

    @Test
    void requestCorrectionsRecordsNotesAndStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(ZoonoticIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        ZoonoticIncident returned = service.requestCorrections(1L, "Mrs Moyo", "Check the figures", "zoonotic");

        assertEquals(IncidentStatus.CORRECTIONS_REQUESTED, returned.getStatus());
        assertEquals("Check the figures", returned.getReviewNotes());
    }

    @Test
    void findByIdReturnsIncident() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        ZoonoticIncident found = service.findById(1L);

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

        List<ZoonoticIncident> result = service.findAllApproved();

        assertEquals(1, result.size());
        verify(repository).findByStatus(IncidentStatus.APPROVED);
    }
}
