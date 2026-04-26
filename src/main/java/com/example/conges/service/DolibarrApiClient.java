package com.example.conges.service;

import java.time.Duration;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

/**
 * Client HTTP Dolibarr (API REST).
 *
 * - Utilise DOLIBARR_URL comme base (ex: http://localhost/dolibarr/htdocs/api/index.php)
 * - Ajoute systématiquement le header DOLAPIKEY
 * - Gère proprement les erreurs (401/403/404/5xx)
 *
 * Important sécurité: le DOLAPIKEY reste côté backend uniquement.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DolibarrApiClient {

    private final WebClient webClient = WebClient.builder()
            .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();

    @Value("${dolibarr.url}")
    private String baseUrl;

    @Value("${dolibarr.api-key:}")
    private String apiKey;

    public Map<String, Object> getStatus() {
        return getJson("/status");
    }

    public Object getUsers() {
        return getJsonObject("/users");
    }

    public Object getThirdParties() {
        return getJsonObject("/thirdparties");
    }

    public Object getInvoices() {
        return getJsonObject("/invoices");
    }

    public Map<String, Object> getJson(String path) {
        Object obj = getJsonObject(path);
        if (obj instanceof Map) {
            //noinspection unchecked
            return (Map<String, Object>) obj;
        }
        throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Réponse Dolibarr inattendue (JSON non-objet)");
    }

    public Object getJsonObject(String path) {
        String url = buildUrl(path);
        String key = (apiKey == null) ? "" : apiKey.trim();
        if (key.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Dolibarr API key manquante (DOLIBARR_API_KEY)");
        }

        try {
            return webClient.get()
                    .uri(url)
                    .header("DOLAPIKEY", key)
                    .retrieve()
                    .bodyToMono(Object.class)
                    .timeout(Duration.ofSeconds(15))
                    .block();
        } catch (WebClientResponseException e) {
            throw mapDolibarrHttpError(e, url);
        } catch (Exception e) {
            log.error("Erreur appel Dolibarr GET {}: {}", url, e.getMessage());
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Erreur connexion Dolibarr: " + e.getMessage());
        }
    }

    private String buildUrl(String path) {
        String base = (baseUrl == null) ? "" : baseUrl.trim();
        if (base.isBlank()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Dolibarr base URL manquante (DOLIBARR_URL)");
        }
        // base doit être seulement la base Dolibarr (index.php inclus si nécessaire), on ajoute juste le endpoint.
        String normalizedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = (path == null) ? "" : path.trim();
        if (!normalizedPath.startsWith("/")) normalizedPath = "/" + normalizedPath;
        return normalizedBase + normalizedPath;
    }

    private ResponseStatusException mapDolibarrHttpError(WebClientResponseException e, String url) {
        HttpStatus status = HttpStatus.resolve(e.getRawStatusCode());
        String body = e.getResponseBodyAsString();
        log.warn("Dolibarr HTTP {} on {} body={}", e.getRawStatusCode(), url, body);

        if (status == null) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Erreur Dolibarr: HTTP " + e.getRawStatusCode());
        }

        if (status == HttpStatus.UNAUTHORIZED) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Dolibarr 401 (API key invalide/manquante)");
        }
        if (status == HttpStatus.FORBIDDEN) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Dolibarr 403 (accès interdit)");
        }
        if (status == HttpStatus.NOT_FOUND) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Dolibarr 404 (endpoint introuvable): " + url);
        }

        if (status.is5xxServerError()) {
            return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                    "Dolibarr erreur serveur: " + status);
        }

        return new ResponseStatusException(HttpStatus.BAD_GATEWAY,
                "Dolibarr erreur: " + status + (body == null || body.isBlank() ? "" : (" - " + body)));
    }
}

