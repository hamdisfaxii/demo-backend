package com.example.conges.service;

import com.example.conges.dto.AuthResponse;
import com.example.conges.dto.LoginRequest;
import com.example.conges.dto.dolibarr.DolibarrEmployeeDto;
import com.example.conges.entity.Role;
import com.example.conges.entity.UserEntity;
import com.example.conges.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final DolibarrService dolibarrService;
    private final DolibarrBackgroundSyncFacade dolibarrBackgroundSyncFacade;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String password = request.getPassword();
        log.info("🔐 Tentative login email={}", email);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email/mot de passe obligatoires");
        }

        // Essayer d'abord avec Dolibarr si configuré
        DolibarrEmployeeDto dolibarrUser = dolibarrService.authenticateUserViaApi(email, password);
        if (dolibarrUser != null) {
            AuthResponse response = handleDolibarrLogin(dolibarrUser, email);
            scheduleFullDolibarrSyncAfterCommitIfConfigured();
            return response;
        }

        // Comptes : authentification et identité depuis Dolibarr uniquement (pas de compte test local).
        log.warn("⚠️ Authentification Dolibarr échouée pour email={}. Connexion refusée.", email);
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants invalides (Dolibarr).");
    }

    private AuthResponse handleDolibarrLogin(DolibarrEmployeeDto dolibarrUser, String email) {
        // Upsert user local, mais UNIQUEMENT à partir d'un compte Dolibarr valide.
        UserEntity user = userRepository.findByDolibarrId(dolibarrUser.getId())
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        Role role = resolveRole(user, dolibarrUser, email);
        String nom = dolibarrUser.getLastName();
        String prenom = dolibarrUser.getFirstName();
        String paysRH = dolibarrService.resolveSupportedHrCountryIso2(
                dolibarrUser.getId(), dolibarrUser);

        if (user == null) {
            user = UserEntity.builder()
                    .dolibarrId(dolibarrUser.getId())
                    .email(email)
                    .nom(nom)
                    .prenom(prenom)
                    .role(role)
                    .pays(paysRH)
                    .build();
        } else {
            user.setDolibarrId(dolibarrUser.getId());
            user.setEmail(email);
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setRole(role);
            user.setPays(paysRH);
        }
        user = userRepository.save(user);

        String token = jwtService.generateToken(user);
        log.info("✅ Connexion Dolibarr OK userId={}, email={}, role={}", user.getId(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .token(token)
                .type("Bearer")
                .user(AuthResponse.UserInfo.builder()
                        .id(user.getId())
                        .email(user.getEmail())
                        .nom(user.getNom())
                        .prenom(user.getPrenom())
                        .role(user.getRole())
                        .pays(user.getPays())
                        .build())
                .build();
    }

    private Role resolveRole(UserEntity existingUser, DolibarrEmployeeDto dolibarrUser, String email) {
        if (existingUser != null && existingUser.getRole() != null && existingUser.getRole() != Role.EMPLOYE) {
            // Si un rôle RH/ADMIN local a déjà été validé, on le conserve.
            return existingUser.getRole();
        }

        if (dolibarrUser != null && dolibarrUser.isAdminLike()) {
            return Role.ADMIN;
        }

        String login = dolibarrUser == null ? "" : String.valueOf(dolibarrUser.getLogin()).toLowerCase();
        String normalizedEmail = email == null ? "" : email.toLowerCase();
        if (login.contains("admin") || normalizedEmail.contains("admin")) {
            return Role.ADMIN;
        }

        return Role.EMPLOYE;
    }

    /**
     * Une fois la transaction login validée : resynchroniser employés / types / féries / allocations
     * depuis Dolibarr (cohérence avec donnée métier après chaque connexion).
     */
    private void scheduleFullDolibarrSyncAfterCommitIfConfigured() {
        if (!dolibarrService.isDolibarrConnectionConfigured()) {
            return;
        }
        Runnable run = () -> dolibarrBackgroundSyncFacade.triggerFullSyncAfterLogin();
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            run.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        run.run();
                    }
                });
    }
}
