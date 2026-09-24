package com.dpdms.mining_accident_service;

import com.dpdms.mining_accident_service.exception.ForbiddenOperationException;
import com.dpdms.mining_accident_service.exception.ResourceNotFoundException;
import com.dpdms.mining_accident_service.model.MiningAccident;
import com.dpdms.mining_accident_service.model.AccidentType;
import com.dpdms.mining_accident_service.model.MineStatus;
import com.dpdms.mining_accident_service.model.IncidentStatus;
import com.dpdms.mining_accident_service.model.Severity;
import com.dpdms.mining_accident_service.repository.MiningAccidentRepository;
import com.dpdms.mining_accident_service.service.AlertNotifier;
import com.dpdms.mining_accident_service.service.MiningAccidentService;
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

// Unit tests for the Mining accident service rules (no database needed - the repository is mocked).
// These cover the marking-guide behaviours: scoping (403), workflow, and audit fields.
@ExtendWith(MockitoExtension.class)
class MiningAccidentServiceTest {

    @Mock
    private MiningAccidentRepository repository;

    @Mock
    private AlertNotifier alertNotifier;

    @InjectMocks
    private MiningAccidentService service;

    private MiningAccident sample;

    @BeforeEach
    void setUp() {
        sample = sampleIncident();
    }

    // A complete, valid mining incident used by most tests
    private MiningAccident sampleIncident() {
        return MiningAccident.builder()
                .ward("Mudzi").district("Mudzi").province("Mashonaland Central")
                .occurredAt(LocalDateTime.now())
                .reporter("T. Chirwa")
                .severity(Severity.HIGH)
                .latitude(-17.4).longitude(31.6)
                .mineName("Rushinga River Alluvial Site")
                .accidentType(AccidentType.ROCKFALL)
                .casualties(2).rescued(1)
                .mineOperationalStatus(MineStatus.SUSPENDED)
                .description("Pit wall collapsed on two artisanal diggers")
                .build();
    }

    @Test
    void createForcesPendingStatusRegardlessOfInput() {
        sample.setStatus(IncidentStatus.APPROVED); // recorder tries to sneak an approved record in
        when(repository.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));

        MiningAccident saved = service.create(sample, "Mudzi", "mining");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertNotNull(saved.getCreatedAt());
        verify(repository).save(sample);
    }

    @Test
    void createRejectsRecorderFromAnotherWard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Bindura", "mining"));
        assertTrue(ex.getMessage().contains("Mudzi"));
        verifyNoInteractions(repository);
    }

    @Test
    void createRejectsRecorderAssignedToAnotherHazard() {
        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> service.create(sample, "Mudzi", "flood"));
        assertTrue(ex.getMessage().contains("mining"));
        verifyNoInteractions(repository);
    }

    @Test
    void updateResubmitsRecordAsPending() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));

        MiningAccident changes = sampleIncident();
        changes.setSeverity(Severity.CRITICAL);
        MiningAccident saved = service.update(1L, changes, "Mudzi", "mining");

        assertEquals(IncidentStatus.PENDING, saved.getStatus());
        assertEquals(Severity.CRITICAL, saved.getSeverity());
    }

    @Test
    void updateBlockedWhenRecordAlreadyApproved() {
        sample.setStatus(IncidentStatus.APPROVED);
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.update(1L, sampleIncident(), "Mudzi", "mining"));
        verify(repository, never()).save(any());
    }

    @Test
    void deleteEnforcesWardScope() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        assertThrows(ForbiddenOperationException.class,
                () -> service.delete(1L, "Bindura", "mining"));
        verify(repository, never()).delete(any(MiningAccident.class));
    }

    @Test
    void approveSetsStatusReviewerAndTimeAndNotifiesAlertService() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));

        MiningAccident approved = service.approve(1L, "Mrs Moyo", "mining");

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
        when(repository.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));

        MiningAccident rejected = service.reject(1L, "Mrs Moyo", "Duplicate report", "mining");

        assertEquals(IncidentStatus.REJECTED, rejected.getStatus());
        assertEquals("Mrs Moyo", rejected.getReviewedBy());
        assertEquals("Duplicate report", rejected.getReviewNotes());
    }

    @Test
    void requestCorrectionsRecordsNotesAndStatus() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));
        when(repository.save(any(MiningAccident.class))).thenAnswer(inv -> inv.getArgument(0));

        MiningAccident returned = service.requestCorrections(1L, "Mrs Moyo", "Check the figures", "mining");

        assertEquals(IncidentStatus.CORRECTIONS_REQUESTED, returned.getStatus());
        assertEquals("Check the figures", returned.getReviewNotes());
    }

    @Test
    void findByIdReturnsIncident() {
        when(repository.findById(1L)).thenReturn(Optional.of(sample));

        MiningAccident found = service.findById(1L);

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

        List<MiningAccident> result = service.findAllApproved();

        assertEquals(1, result.size());
        verify(repository).findByStatus(IncidentStatus.APPROVED);
    }
}
