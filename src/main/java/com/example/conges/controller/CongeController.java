package com.example.conges.controller;

import com.example.conges.dto.DemandeCongeRequest;
import com.example.conges.dto.DemandeCongeResponse;
import com.example.conges.dto.config.WorkScheduleConfigResponse;
import com.example.conges.entity.Role;
import com.example.conges.entity.UserEntity;
import com.example.conges.service.CongeService;
import javax.validation.Valid;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller alias pour la compatibilité frontend.
 * Redirige les requêtes /api/conge/* vers le service CongeService.
 */
@RestController
@RequestMapping("/api/conge")
@RequiredArgsConstructor
public class CongeController {

    private final CongeService congeService;

    /**
     * GET /api/conge/solde
     * Soldes métier enrichis ({@code soldeCongesPayes}, RTT, maladie, etc.) plus {@code solde} (= CP uniquement).
     */
    @GetMapping("/solde")
    public ResponseEntity<Map<String, Object>> getSolde(@AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(congeService.getSoldeResponseMap(user.getId()));
    }

    /**
     * Horaires de travail du pays métier pour caler créneaux « courte durée » / RTT front.
     */
    @GetMapping("/meta/work-schedule")
    public ResponseEntity<WorkScheduleConfigResponse> getMyWorkSchedule(
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(congeService.getActiveWorkScheduleForUser(user.getId()));
    }

    /**
     * GET /api/conge/liste?annee=2024&statut=attente
     * Retourne la liste des demandes avec filtres optionnels.
     */
    @GetMapping("/liste")
    public ResponseEntity<Map<String, Object>> getListe(
            @AuthenticationPrincipal UserEntity user,
            @RequestParam(required = false) Integer annee,
            @RequestParam(required = false) String statut) {
        
        // Pour un employé, retourne ses demandes; pour RH, retourne toutes les demandes
        List<DemandeCongeResponse> demandes;
        
        // Vérifie si l'utilisateur est RH
        boolean isRh = user.getRole() == Role.RH || user.getRole() == Role.ADMIN;
        
        if (isRh) {
            demandes = congeService.getAllDemandesEnAttente();
        } else {
            demandes = congeService.getMesDemandesFiltrees(user.getId(), annee, statut);
        }
        
        Map<String, Object> response = new HashMap<>();
        response.put("demandes", demandes);
        response.put("data", demandes);
        
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/conge/{id}
     * Retourne le détail d'une demande par son ID.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DemandeCongeResponse> getDemandeById(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            return ResponseEntity.ok(congeService.getDemandeById(id, user.getId(), user.getRole()));
        } catch (javax.persistence.EntityNotFoundException ex) {
            return ResponseEntity.notFound().build();
        } catch (org.springframework.security.access.AccessDeniedException ex) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
    }

    /**
     * POST /api/conge
     * Crée une nouvelle demande de congé.
     */
    @PostMapping
    public ResponseEntity<DemandeCongeResponse> creerDemande(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DemandeCongeRequest request) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        DemandeCongeResponse response = congeService.creerDemande(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * PUT /api/conge/{id}/valider
     * Valide une demande de congé (RH seulement).
     */
    @PutMapping("/{id}/valider")
    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<DemandeCongeResponse> validerDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity rh,
            @RequestBody Map<String, Object> body) {
        
        boolean accepte = (Boolean) body.getOrDefault("accepte", false);
        String commentaire = (String) body.getOrDefault("commentaire", "");
        
        DemandeCongeResponse response = congeService.validerDemande(
                id,
                rh.getId(),
                accepte,
                commentaire
        );
        
        return ResponseEntity.ok(response);
    }

    /**
     * PUT /api/conge/{id}/annuler
     * Annule une demande de congé.
     */
    @PutMapping("/{id}/annuler")
    public ResponseEntity<DemandeCongeResponse> annulerDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user) {
        
        DemandeCongeResponse response = congeService.annulerDemande(id, user.getId());
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/conge/stats
     * Retourne les statistiques pour le dashboard RH.
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('RH') or hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> getStats() {
        List<DemandeCongeResponse> toutesLemandes = congeService.getAllDemandesEnAttente();
        
        long total = toutesLemandes.size();
        long pending = toutesLemandes.stream()
                .filter(d -> "EN_ATTENTE".equals(d.getStatut().toString()))
                .count();
        long approved = toutesLemandes.stream()
                .filter(d -> "ACCEPTE".equals(d.getStatut().toString()))
                .count();
        long rejected = toutesLemandes.stream()
                .filter(d -> "REFUSE".equals(d.getStatut().toString()))
                .count();
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", total);
        stats.put("pending", pending);
        stats.put("approved", approved);
        stats.put("rejected", rejected);
        
        return ResponseEntity.ok(stats);
    }
}
