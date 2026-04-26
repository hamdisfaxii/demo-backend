# 📋 Gestion des Congés - Système d'Historique Complet

## 🎯 Objective

**Implémenter un système d'historique automatisé et professionnel** qui enregistre TOUTES les actions effectuées sur les demandes de congés.

## ✅ Ce qui a été implémenté

### 1. **Entité History (JPA)**

- Fichier: `entity/History.java`
- Table: `history` avec indices pour performances
- Champs: user, demande, actionType, description, details (JSON), pays, statut, actionDate, ipAddress, userAgent
- Types d'actions : CREATE, APPROVE, REJECT, CANCEL, UPDATE, DOCUMENT_SENT, EXPORTED, SYNCED_DOLIBARR, LOGIN, LOGOUT, REPORT_VIEWED, OTHER

### 2. **Repository JPA**

- Fichier: `repository/HistoryRepository.java`
- Requêtes personnalisées :
  - `findByUserId()` - Historique par utilisateur
  - `findByDemandeId()` - Historique par demande
  - `findByActionType()` - Historique par type d'action
  - `searchHistory()` - Recherche avancée avec filtres multiples
  - `findByActionDateBetweenOrderByActionDateDesc()` - Export par période
  - `getUserHistoryByPeriod()` - Historique utilisateur sur une période

### 3. **Service HistoryService**

- Fichier: `service/HistoryService.java`
- Méthodes d'enregistrement :
  - `recordAction()` - Enregistrement générique
  - `recordCreation()` - Création de demande
  - `recordApproval()` - Approbation RH
  - `recordRejection()` - Rejet RH
  - `recordCancellation()` - Annulation employé
  - `recordUpdate()` - Modification
  - `recordDolibarrSync()` - Synchronisation Dolibarr
  - `recordExport()` - Export PDF/Excel
  - `recordLogin()` - Connexion utilisateur
- Méthodes de consultation :
  - `getHistory()` - Historique avec filtres et pagination
  - `getUserHistory()` - Historique d'un utilisateur
  - `getDemandeHistory()` - Historique d'une demande
  - `getHistoryForExport()` - Historique non-paginé pour export
  - `getActionStatistics()` - Statistiques par type d'action
- Utilitaires :
  - Extraction IP client
  - Extraction User-Agent
  - Construction JSON des détails

### 4. **Controllers REST**

- Fichier: `controller/HistoryController.java`
- Endpoints :

| Méthode | URL                                | Rôles               | Description                           |
| ------- | ---------------------------------- | ------------------- | ------------------------------------- |
| GET     | `/api/history`                     | RH, ADMIN           | Historique avec filtres et pagination |
| GET     | `/api/history/user/{userId}`       | RH, ADMIN, User     | Historique d'un utilisateur           |
| GET     | `/api/history/demande/{demandeId}` | RH, ADMIN, EMPLOYEE | Historique d'une demande              |
| GET     | `/api/history/statistics`          | RH, ADMIN           | Statistiques des actions              |
| GET     | `/api/history/export/pdf`          | RH, ADMIN           | Export PDF                            |
| GET     | `/api/history/export/excel`        | RH, ADMIN           | Export Excel                          |
| GET     | `/api/history/export/rh-report`    | RH, ADMIN           | Rapport RH complet                    |

### 5. **Services d'Export**

- **PdfExportService** (`service/export/PdfExportService.java`)
  - `generateHistoryReport()` - Rapport PDF de l'historique
  - `generateLeaveAttestation()` - Attestation de congés
  - Utilise **iText7** pour la génération de PDF

- **ExcelExportService** (`service/export/ExcelExportService.java`)
  - `generateHistoryExcel()` - Export tabulaire de l'historique
  - `generateRhReport()` - Rapport RH formaté
  - Utilise **Apache POI** pour la génération Excel

### 6. **DTOs**

- **HistoryResponse** (`dto/HistoryResponse.java`) - Pour les réponses API

### 7. **Mapper**

- **HistoryMapper** (`mapper/HistoryMapper.java`) - Conversion Entity → DTO

### 8. **Intégration CongeService**

- Le service est **automatiquement appelé** lors de :
  - `creerDemande()` - Appelle `recordCreation()`
  - `annulerDemande()` - Appelle `recordCancellation()`
  - `validerDemande()` - Appelle `recordApproval()` ou `recordRejection()`

### 9. **Configuration**

- **AppConfiguration.java** - Configuration ObjectMapper pour JSON
- **pom.xml** - Dépendances :
  - `itext7-core:7.2.6` pour PDF
  - `poi-ooxml:5.2.4` pour Excel
  - `jackson-databind:2.15.2` pour JSON

### 10. **Tests**

- **HistoryServiceTest.java** - Tests unitaires du service
- **HistoryControllerTest.java** - Tests des endpoints

### 11. **Documentation**

- **docs/API_HISTORIQUE.md** - Documentation API complète avec exemples
- **SQL Migration** - `src/main/resources/db/migration/history-table.sql`

## 🔌 Integration Points

### Avec CongeService

```java
// Automatiquement enregistré lors de création
DemandeConge saved = demandeCongeRepository.save(demande);
historyService.recordCreation(user, saved);
```

### Avec AuthService (à faire)

```java
// À ajouter lors de la connexion
historyService.recordLogin(user);
```

### Avec DolibarrService (à faire)

```java
// À ajouter lors de la synchronisation
historyService.recordDolibarrSync(user, demande, "SUCCESS");
```

## 🗄️ Schéma BD

```sql
CREATE TABLE history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    demande_id BIGINT,
    action_type VARCHAR(50) NOT NULL,
    description VARCHAR(500),
    details LONGTEXT,  -- JSON
    pays VARCHAR(50),
    statut VARCHAR(50),
    action_date DATETIME NOT NULL,
    ip_address VARCHAR(45),
    user_agent VARCHAR(500),

    CONSTRAINT fk_history_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_history_demande FOREIGN KEY (demande_id) REFERENCES demandes_conge(id),

    INDEX idx_user (user_id),
    INDEX idx_demande (demande_id),
    INDEX idx_action_type (action_type),
    INDEX idx_action_date (action_date)
);
```

## 📊 Exemple d'Historique

```json
{
  "id": 1,
  "user": {
    "id": 1,
    "nom": "Dupont",
    "prenom": "Jean",
    "email": "jean@example.com"
  },
  "demandeId": 5,
  "actionType": "CREATE",
  "description": "Demande de congé créée",
  "details": "{\"typeConge\":\"PAYE\",\"dateDebut\":\"2024-03-01\",\"dateFin\":\"2024-03-05\",\"nombreJours\":\"4\"}",
  "pays": "TN",
  "statut": "EN_ATTENTE",
  "actionDate": "01/03/2024 10:30:00",
  "ipAddress": "192.168.1.100",
  "userAgent": "Mozilla/5.0..."
}
```

## 🚀 Utilisation

### 1. Récupérer l'historique

```bash
curl -X GET "http://localhost:8080/api/history?page=0&size=20" \
  -H "Authorization: Bearer TOKEN"
```

### 2. Filtrer par utilisateur

```bash
curl -X GET "http://localhost:8080/api/history/user/1?page=0&size=10" \
  -H "Authorization: Bearer TOKEN"
```

### 3. Exporter en PDF

```bash
curl -X GET "http://localhost:8080/api/history/export/pdf?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59" \
  -H "Authorization: Bearer TOKEN" \
  -o "historique.pdf"
```

### 4. Exporter en Excel

```bash
curl -X GET "http://localhost:8080/api/history/export/excel?pays=TN" \
  -H "Authorization: Bearer TOKEN" \
  -o "historique.xlsx"
```

### 5. Rapport RH

```bash
curl -X GET "http://localhost:8080/api/history/export/rh-report?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59" \
  -H "Authorization: Bearer TOKEN" \
  -o "rapport_rh.xlsx"
```

### 6. Statistiques

```bash
curl -X GET "http://localhost:8080/api/history/statistics" \
  -H "Authorization: Bearer TOKEN"
```

## 🔒 Sécurité

- **Authentification JWT** requise pour tous les endpoints
- **Autorisations par rôle** :
  - RH et ADMIN : accès complet
  - EMPLOYEE : accès à son propre historique
- **Audit trail** complet automatique
- **Masquage des données sensibles** en export

## 📈 Performances

- **Indices BD** sur : user_id, demande_id, action_type, action_date
- **Pagination** par défaut (20 records)
- **Requêtes optimisées** avec LEFT JOINs
- **Lazy loading** sur les associations

## 🎁 Fonctionnalités Bonus

1. ✅ **Export PDF** - Rapport formaté avec en-têtes et tableaux
2. ✅ **Export Excel** - Avec mise en forme (couleurs, borders, freeze pane)
3. ✅ **Statistiques** - Comptage par type d'action
4. ✅ **Filtrage avancé** - Multi-critères (user, demande, action, pays, dates)
5. ✅ **IP/User-Agent** - Traçabilité complète
6. ✅ **JSON details** - Infos détaillées sérializées
7. ✅ **Pagination** - Gestion des larges datasets

## 📋 Checklist des Améliorations

- [x] Entité History avec tous les champs
- [x] Repository JPA avec requêtes complètes
- [x] Service d'enregistrement automatique
- [x] Controllers REST avec pagination
- [x] Export PDF avec formatage
- [x] Export Excel avec styles
- [x] DTO et Mapper
- [x] Intégration CongeService
- [x] Tests unitaires
- [x] Tests endpoints
- [x] Configuration JSON
- [x] Documentation API
- [x] Indices BD
- [ ] AOP pour auto-enregistrement (optionnel)
- [ ] Notifications (optionnel)
- [ ] Graphiques (optionnel)

## 📚 Fichiers Créés/Modifiés

### Créés

- `entity/History.java`
- `repository/HistoryRepository.java`
- `service/HistoryService.java`
- `service/export/PdfExportService.java`
- `service/export/ExcelExportService.java`
- `controller/HistoryController.java`
- `dto/HistoryResponse.java`
- `mapper/HistoryMapper.java`
- `config/AppConfiguration.java`
- `service/HistoryServiceTest.java`
- `controller/HistoryControllerTest.java`
- `docs/API_HISTORIQUE.md`
- `src/main/resources/db/migration/history-table.sql`

### Modifiés

- `pom.xml` - Ajout dépendances iText7 et POI
- `service/CongeService.java` - Ajout appels HistoryService

## 🔧 Prochaines Étapes Recommandées

1. **Intégrer AuthService** - Ajouter enregistrement LOGIN/LOGOUT
2. **Ajouter notifications** - Email aux RH lors d'approbations
3. **Dashboard RH** - Vue graphique des historiques
4. **AOP interceptors** - Enregistrement automatique sans appels manuels
5. **Archivage** - Nettoyer les vieux records automatiquement
6. **Audit externe** - Intégration avec système de contrôle d'accès

## 💡 Notes

- **Thread-safe** : Annotations `@Transactional` garantissent la sérialité
- **Gestion d'erreurs** : Les exceptions sont loggées sans interruption du flux
- **Extensibilité** : Facile d'ajouter de nouveaux ActionTypes
- **Performance** : Requêtes optimisées et paginées
- **Conformité** : Conforme GDPR (traçabilité des actions utilisateurs)

---

**Version:** 1.0  
**Date:** 2024-03-15  
**Auteur:** Équipe Développement  
**Status:** ✅ Complet et Prêt à Utiliser
