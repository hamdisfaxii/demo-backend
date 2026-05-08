package com.example.conges.service;

import com.example.conges.entity.DemandeConge;
import com.example.conges.entity.History;
import com.example.conges.entity.History.ActionType;
import com.example.conges.entity.StatutConge;
import com.example.conges.entity.TypeConge;
import com.example.conges.entity.UserEntity;
import com.example.conges.entity.Role;
import com.example.conges.repository.HistoryRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests unitaires pour HistoryService
 */
@ExtendWith(MockitoExtension.class)
class HistoryServiceTest {

    @Mock
    private HistoryRepository historyRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private HistoryService historyService;

    private UserEntity mockUser;
    private DemandeConge mockDemande;

    @BeforeEach
    void setUp() {
        mockUser = UserEntity.builder()
                .id(1L)
                .email("test@example.com")
                .nom("Dupont")
                .prenom("Jean")
                .role(Role.EMPLOYE)
                .pays("TN")
                .build();

        mockDemande = DemandeConge.builder()
                .id(1L)
                .user(mockUser)
                .typeConge(TypeConge.PAYE)
                .dateDebut(LocalDate.of(2024, 3, 1))
                .dateFin(LocalDate.of(2024, 3, 5))
                .nombreJours(4)
                .statut(StatutConge.EN_ATTENTE)
                .motif("Repos annuel")
                .dateSoumission(LocalDateTime.now())
                .build();
    }

    @Test
    void testRecordCreation() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> {
                    History h = invocation.getArgument(0);
                    h.setId(1L);
                    return h;
                });

        // Act
        historyService.recordCreation(mockUser, mockDemande);

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    void testRecordApproval() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        historyService.recordApproval(mockUser, mockDemande, "Dupont Jean");

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    void testRecordRejection() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        historyService.recordRejection(mockUser, mockDemande, "Raison du rejet");

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    void testRecordCancellation() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        historyService.recordCancellation(mockUser, mockDemande, "Pas de raison");

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    void testGetUserHistory() {
        // Arrangé
        List<History> mockHistory = List.of(
                History.builder()
                        .id(1L)
                        .userId(mockUser.getId())
                        .userNom(mockUser.getNom())
                        .userPrenom(mockUser.getPrenom())
                        .userEmail(mockUser.getEmail())
                        .demandeId(mockDemande.getId())
                        .actionType(ActionType.CREATE)
                        .description("Demande créée")
                        .actionDate(LocalDateTime.now())
                        .build()
        );

        Pageable pageable = PageRequest.of(0, 10);
        when(historyRepository.findByUserId(1L, pageable))
                .thenReturn(new PageImpl<>(mockHistory, pageable, 1));

        // Act
        var result = historyService.getUserHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getActionType()).isEqualTo(ActionType.CREATE);
    }

    @Test
    void testGetDemandeHistory() {
        // Arrangé
        List<History> mockHistory = List.of(
                History.builder()
                        .id(1L)
                        .userId(mockUser.getId())
                        .userNom(mockUser.getNom())
                        .userPrenom(mockUser.getPrenom())
                        .userEmail(mockUser.getEmail())
                        .demandeId(mockDemande.getId())
                        .actionType(ActionType.CREATE)
                        .actionDate(LocalDateTime.now())
                        .build(),
                History.builder()
                        .id(2L)
                        .userId(mockUser.getId())
                        .userNom(mockUser.getNom())
                        .userPrenom(mockUser.getPrenom())
                        .userEmail(mockUser.getEmail())
                        .demandeId(mockDemande.getId())
                        .actionType(ActionType.APPROVE)
                        .actionDate(LocalDateTime.now().plusHours(1))
                        .build()
        );

        Pageable pageable = PageRequest.of(0, 10);
        when(historyRepository.findByDemandeId(1L, pageable))
                .thenReturn(new PageImpl<>(mockHistory, pageable, 2));

        // Act
        var result = historyService.getDemandeHistory(1L, pageable);

        // Assert
        assertThat(result).isNotNull();
        assertThat(result.getContent()).hasSize(2);
        assertThat(result.getTotalElements()).isEqualTo(2);
    }

    @Test
    void testRecordLogin() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> {
                    History h = invocation.getArgument(0);
                    h.setId(1L);
                    return h;
                });

        // Act
        historyService.recordLogin(mockUser);

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }

    @Test
    void testRecordExport() {
        // Arrangé
        when(historyRepository.save(any(History.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        historyService.recordExport(mockUser, mockDemande, "PDF", "demande_1_2024-03-15.pdf");

        // Assert
        verify(historyRepository, times(1)).save(any(History.class));
    }
}
