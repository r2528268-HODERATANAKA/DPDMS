package com.dpdms.drought_service;

import com.dpdms.drought_service.exception.ForbiddenOperationException;
import com.dpdms.drought_service.exception.ResourceNotFoundException;
import com.dpdms.drought_service.model.DroughtIncident;
import com.dpdms.drought_service.model.WaterSourceCondition;
import com.dpdms.drought_service.model.IncidentStatus;
import com.dpdms.drought_service.model.Severity;
import com.dpdms.drought_service.repository.DroughtIncidentRepository;
import com.dpdms.drought_service.service.AlertNotifier;
import com.dpdms.drought_service.service.DroughtIncidentService;
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

// Unit tests for the Drought service rules (no database needed - the repository is mocked).
// These cover the marking-guide behaviours: scoping (403), workflow, and audit fields.
@ExtendWith(MockitoExtension.class)
class DroughtIncidentServiceTest {

    @Mock
    private DroughtIncidentRepository repository;

    @Mock
    private AlertNotifier alertNotifier;

    @InjectMocks
    private DroughtIncidentService service;

    private DroughtIncident sample;

    @BeforeEach
    void setUp() {
        sample = sampleIncident();
    }

    // A complete, valid drought incident used by most tests
    private DroughtIncident sampleIncident() {
        return DroughtIncident.builder()
                .ward("Mudzi").district("Mudzi").province("Mashonaland Central")
                .occurredAt(LocalDateTime.now())
                .reporter("T. Chirwa")
                .severity(Severity.HIGH)
                .latitude(-17.4).longitude(31.6)
                .rainfallDeficitMm(62.5)
                .consecutiveDryDays(34)
                .affectedHouseholds(320)
                .livestockDeaths(18)
                .cropDamageHectares(120.0)
                .waterSourceCondition(WaterSourceCondition.CRITICAL)
                .build();
    }

    @Test
    void createForcesPendingStatusRegardlessOfInput() {
        sample.setStatus(IncidentStatus.APPROVED); // recorder tries to sneak an approved record in
        when(repository.save(any(DroughtIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident saved = service.create(sample, "Mudzi", "drought");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        verify(repository).save(sample);
    }

    @Test
    void createRejectsRecorderFromAnotherWard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Bindura", "drought"));
        assertTrue(ex.getMessage().contains("Mudzi"));
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsRecorderAssignedToAnotherHazard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Mudzi", "flood"));
        assertTrue(ex.getMessage().contains("drought"));
        verifyNoInteractions(repository);
    }

    @Test
    void updateResubmitsRecordAsPending() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(DroughtIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident changes = sampleIncident();
        changes.setSeverity(Severity.CRITICAL);
        DroughtIncident saved = service.update(1L, changes, "Mudzi", "drought");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertEquals(Severity.CRITICAL, saved.getSeverity());
    }

    @Test
    void updateBlockedWhenRecordAlreadyApproved() {
        sample.setStatus(IncidentStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.update(1L, sampleIncident(), "Mudzi", "drought"));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteEnforcesWardScope() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.delete(1L, "Bindura", "drought"));
        verify(repository, never()).delete(any(DroughtIncident.class));
    }

    @Test
    void approveSetsStatusReviewerAndTimeAndNotifiesAlertService() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(DroughtIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident approved = service.approve(1L, "Mrs Moyo", "drought");

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
        when(repository.save(any(DroughtIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident rejected = service.reject(1L, "Mrs Moyo", "Duplicate report", "drought");

        assertEquals(IncidentStatus.REJECTED, rejected.getStatus());
        assertEquals("Mrs Moyo", rejected.getReviewedBy());
        assertEquals("Duplicate report", rejected.getReviewNotes());
    }

    @Test
    void requestCorrectionsRecordsNotesAndStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(DroughtIncident.class))).thenAnswer(inv -> inv.getArgument(0));

        DroughtIncident returned = service.requestCorrections(1L, "Mrs Moyo", "Check the figures", "drought");

        assertEquals(IncidentStatus.CORRECTIONS_REQUESTED, returned.getStatus());
        assertEquals("Check the figures", returned.getReviewNotes());
    }

    @Test
    void findByIdReturnsIncident() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        DroughtIncident found = service.findById(1L);

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

        List<DroughtIncident> result = service.findAllApproved();

        assertEquals(1, result.size());
        verify(repository).findByStatus(IncidentStatus.APPROVED);
    }
}
