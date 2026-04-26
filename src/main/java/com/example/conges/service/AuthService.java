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
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final JwtService jwtService;
    private final DolibarrService dolibarrService;

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String email = request.getEmail() == null ? null : request.getEmail().trim();
        String password = request.getPassword();
        log.info("🔐 Tentative login (Dolibarr) email={}", email);

        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email/mot de passe obligatoires");
        }

        DolibarrEmployeeDto dolibarrUser = dolibarrService.authenticateUserViaApi(email, password);
        if (dolibarrUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Identifiants Dolibarr invalides");
        }

        // Upsert user local, mais UNIQUEMENT à partir d'un compte Dolibarr valide.
        UserEntity user = userRepository.findByDolibarrId(dolibarrUser.getId())
                .orElseGet(() -> userRepository.findByEmail(email).orElse(null));

        Role role = user != null ? user.getRole() : Role.EMPLOYE;
        String nom = dolibarrUser.getLastName();
        String prenom = dolibarrUser.getFirstName();

        if (user == null) {
            user = UserEntity.builder()
                    .dolibarrId(dolibarrUser.getId())
                    .email(email)
                    .nom(nom)
                    .prenom(prenom)
                    .role(role)
                    .pays(dolibarrUser.getCountryCode())
                    .build();
        } else {
            user.setDolibarrId(dolibarrUser.getId());
            user.setEmail(email);
            user.setNom(nom);
            user.setPrenom(prenom);
            user.setRole(role);
            if (dolibarrUser.getCountryCode() != null) {
                user.setPays(dolibarrUser.getCountryCode());
            }
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
}
