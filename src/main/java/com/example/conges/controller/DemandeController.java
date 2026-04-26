package com.example.conges.controller;

import com.example.conges.dto.DemandeCongeRequest;
import com.example.conges.dto.DemandeCongeResponse;
import com.example.conges.dto.SoldeCongeResponse;
import com.example.conges.dto.StatistiquesRhResponse;
import com.example.conges.dto.ValiderDemandeRequest;
import com.example.conges.entity.UserEntity;
import com.example.conges.service.CongeService;
import javax.validation.Valid;
import java.util.List;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/demandes")
@RequiredArgsConstructor
public class DemandeController {

    private final CongeService congeService;

    @PostMapping
    public ResponseEntity<DemandeCongeResponse> creerDemande(
            @AuthenticationPrincipal UserEntity user,
            @Valid @RequestBody DemandeCongeRequest request
    ) {
        DemandeCongeResponse body = congeService.creerDemande(user.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PutMapping("/{id}/annuler")
    public ResponseEntity<DemandeCongeResponse> annulerDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity user
    ) {
        return ResponseEntity.ok(congeService.annulerDemande(id, user.getId()));
    }

    @GetMapping("/mes-demandes")
    public ResponseEntity<List<DemandeCongeResponse>> mesDemandes(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(congeService.getMesDemandes(user.getId()));
    }

    @GetMapping("/historique")
    public ResponseEntity<List<DemandeCongeResponse>> historique(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(congeService.getHistorique(user.getId()));
    }

    @GetMapping("/solde")
    public ResponseEntity<List<SoldeCongeResponse>> solde(@AuthenticationPrincipal UserEntity user) {
        return ResponseEntity.ok(congeService.calculerSolde(user.getId()));
    }

    @GetMapping("/en-attente")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<List<DemandeCongeResponse>> demandesEnAttente() {
        return ResponseEntity.ok(congeService.getAllDemandesEnAttente());
    }

    @PutMapping("/{id}/valider")
    @PreAuthorize("hasAnyRole('RH','MANAGER','ADMIN')")
    public ResponseEntity<DemandeCongeResponse> validerDemande(
            @PathVariable Long id,
            @AuthenticationPrincipal UserEntity rh,
            @Valid @RequestBody ValiderDemandeRequest request
    ) {
        boolean accepte = Boolean.TRUE.equals(request.getAccepte());
        return ResponseEntity.ok(congeService.validerDemande(
                id,
                rh.getId(),
                accepte,
                request.getCommentaire()
        ));
    }

    @GetMapping("/statistiques")
    @PreAuthorize("hasRole('RH')")
    public ResponseEntity<StatistiquesRhResponse> statistiques() {
        return ResponseEntity.ok(congeService.getStatistiquesRh());
    }
}
