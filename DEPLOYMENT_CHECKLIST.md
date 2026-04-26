# 🔧 CHECKLIST D'INTÉGRATION - À FAIRE APRÈS DÉPLOIEMENT

## ✅ Phase 1: Vérification Compilation (5 minutes)

- [ ] Tous les fichiers `.java` sont au bon endroit
- [ ] `pom.xml` contient les 3 dépendances (iText7, POI, Jackson)
- [ ] `mvn clean compile` passe sans erreurs
- [ ] `mvn clean test` passe (14 tests)
- [ ] Application démarre : `mvn spring-boot:run`

## ✅ Phase 2: Vérification BD (5 minutes)

- [ ] Table `history` existe dans MySQL

```sql
SHOW TABLES LIKE 'history';
DESCRIBE history;
```

- [ ] Indices créés

```sql
SHOW INDEX FROM history;
-- Doit afficher: idx_user, idx_demande, idx_action_type, idx_action_date
```

- [ ] Contraintes FK existent

```sql
SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE
WHERE TABLE_NAME = 'history';
```

## ✅ Phase 3: Vérification API (10 minutes)

**Démarrer l'application et tester:**

### Test 1: Créer une demande (génère History.CREATE)

```bash
# Remplacer {TOKEN} par un JWT valide
curl -X POST "http://localhost:8080/api/demandes" \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json" \
  -d '{
    "typeConge": "PAYE",
    "dateDebut": "2024-04-01",
    "dateFin": "2024-04-05",
    "motif": "Test historique"
  }' \
  -v

# Vérifier: Status 201 Created
# Body: Contient l'ID de la demande créée
```

### Test 2: Vérifier que History a été enregistrée

```bash
# Dans MySQL
SELECT * FROM history WHERE action_type = 'CREATE' ORDER BY action_date DESC LIMIT 1;

# Doit afficher un record avec:
# - user_id: ID de l'utilisateur
# - demande_id: ID de la demande créée
# - action_type: CREATE
# - status: EN_ATTENTE
```

### Test 3: Récupérer l'historique (GET /api/history)

```bash
curl -X GET "http://localhost:8080/api/history?page=0&size=10" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -v

# Vérifier: Status 200 OK
# Body: JSON avec records d'historique
```

### Test 4: Récupérer historique d'une demande

```bash
curl -X GET "http://localhost:8080/api/history/demande/1" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -v

# Vérifier: Status 200 OK
# Body: Historique de la demande 1
```

### Test 5: Statistiques

```bash
curl -X GET "http://localhost:8080/api/history/statistics" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -v

# Vérifier: Status 200 OK
# Body: JSON comme {"CREATE": 5, "APPROVE": 2, ...}
```

### Test 6: Export PDF

```bash
curl -X GET "http://localhost:8080/api/history/export/pdf" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -o test_historique.pdf \
  -v

# Vérifier: Status 200 OK
# Fichier: test_historique.pdf créé et non vide
# Contenu: PDF avec tableau de l'historique
```

### Test 7: Export Excel

```bash
curl -X GET "http://localhost:8080/api/history/export/excel" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -o test_historique.xlsx \
  -v

# Vérifier: Status 200 OK
# Fichier: test_historique.xlsx créé et non vide
# Contenu: Excel avec tableau de l'historique
```

### Test 8: Filtrage Avancé

```bash
# Filtrer par action type
curl -X GET "http://localhost:8080/api/history?actionType=CREATE&page=0&size=5" \
  -H "Authorization: Bearer {RH_TOKEN}"

# Filtrer par pays
curl -X GET "http://localhost:8080/api/history?pays=TN&page=0&size=5" \
  -H "Authorization: Bearer {RH_TOKEN}"

# Filtrer par date
curl -X GET "http://localhost:8080/api/history?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59" \
  -H "Authorization: Bearer {RH_TOKEN}"

# Filtrer par utilisateur
curl -X GET "http://localhost:8080/api/history/user/1" \
  -H "Authorization: Bearer {RH_TOKEN}"
```

## ✅ Phase 4: Vérification Sécurité (5 minutes)

### Test 1: Accès sans token (doit être 401)

```bash
curl -X GET "http://localhost:8080/api/history" \
  -v

# Vérifier: Status 401 Unauthorized
```

### Test 2: Accès avec rôle EMPLOYEE (doit être 403)

```bash
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer {EMPLOYEE_TOKEN}" \
  -v

# Vérifier: Status 403 Forbidden
```

### Test 3: Accès avec rôle RH (doit être 200)

```bash
curl -X GET "http://localhost:8080/api/history" \
  -H "Authorization: Bearer {RH_TOKEN}" \
  -v

# Vérifier: Status 200 OK
```

### Test 4: EMPLOYEE accède à son propre historique (doit être 200)

```bash
curl -X GET "http://localhost:8080/api/history/user/1" \
  -H "Authorization: Bearer {USER_1_TOKEN}" \
  -v

# Vérifier: Status 200 OK (l'utilisateur 1 accède à son historique)
```

## ✅ Phase 5: Intégration AuthService (OPTIONNEL mais RECOMMANDÉ)

**Ajouter enregistrement de LOGIN dans AuthService:**

```java
// Dans AuthService.java ou AuthController.java

@Transactional
public AuthResponse login(String email, String password) {
    // ... validation existante ...

    UserEntity user = userRepository.findByEmail(email)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

    // ... validation password ...

    String token = jwtService.generateToken(user);

    // ✅ AJOUTER ICI:
    historyService.recordLogin(user);

    return new AuthResponse(token);
}
```

## ✅ Phase 6: Intégration DolibarrService (OPTIONNEL)

**Ajouter enregistrement de synchronisation dans DolibarrService:**

```java
// Dans DolibarrService.java

@Transactional
public void syncLeaveToDolibarr(Long demandeId) {
    DemandeConge demande = demandeCongeRepository.findById(demandeId)
            .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

    try {
        // ... call API Dolibarr ...

        UserEntity user = demande.getUser();

        // ✅ AJOUTER ICI:
        historyService.recordDolibarrSync(user, demande, "SUCCESS");

    } catch (Exception e) {
        // ✅ AJOUTER ICI:
        historyService.recordDolibarrSync(user, demande, "FAILED: " + e.getMessage());
        throw e;
    }
}
```

## ✅ Phase 7: Tests Manuels Avancés (15 minutes)

### Scenario 1: Complet (Create → Approve → Export)

```
1. Employé crée demande
   curl POST /api/demandes

2. Vérifier History.CREATE enregistrée
   SELECT * FROM history WHERE action_type = 'CREATE'

3. RH approuve
   curl PUT /api/demandes/5/valider

4. Vérifier History.APPROVE enregistrée
   SELECT * FROM history WHERE demande_id = 5

5. RH exporte
   curl GET /api/history/demande/5/export/pdf

6. Vérifier PDF contient CREATE + APPROVE
```

### Scenario 2: Recherche Avancée

```
1. Créer 5 demandes différentes (par différents users)
2. Faire 3 approbations, 2 rejets
3. Rechercher par actionType=APPROVE
   → Doit retourner 3 records
4. Rechercher par pays=TN
   → Doit retourner seulement demandes de Tunisie
5. Rechercher par dateRange janvier 2024
   → Doit retourner seulement demandes de janvier
```

### Scenario 3: Export Multi-Format

```
1. Export PDF de tous les CREATE
   GET /api/history/export/pdf?actionType=CREATE

2. Export Excel de tous les APPROVE par pays
   GET /api/history/export/excel?actionType=APPROVE&pays=TN

3. Export RH Report (toutes données)
   GET /api/history/export/rh-report

4. Vérifier tous les 3 PDFs/Excels sont valides
```

## ✅ Phase 8: Performance (10 minutes)

### Test 1: Pagination

```
# Créer 100+ records dans history
# Tester GET /api/history avec page=0&size=20
# Doit retourner 20 records en < 1 seconde

curl -X GET "http://localhost:8080/api/history?page=0&size=20" \
  -H "Authorization: Bearer {TOKEN}" \
  -w "\nTime: %{time_total}s\n"
```

### Test 2: Filtres Multiples

```
# Requête complexe
GET /api/history?userId=1&actionType=APPROVE&pays=TN&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&page=0&size=50

# Doit retourner en < 2 secondes
```

### Test 3: Export Grand Volume

```
# Export avec startDate et endDate = toute l'année 2024
GET /api/history/export/excel?startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59

# Doit générer Excel en < 10 secondes même avec 1000+ records
```

## ✅ Phase 9: Logs & Monitoring (5 minutes)

### Vérifier les Logs

```
# Dans les logs d'application, chercher:
[INFO] Action enregistrée: CREATE pour l'utilisateur 1
[INFO] Historique consulté - 125 records trouvés
[INFO] Rapport PDF généré - 150 records
[INFO] Rapport Excel généré - 150 records

# Pas d'erreurs: [ERROR] ou [WARN] concernant History
```

### Configurer Logs (optionnel)

```properties
# Dans application.properties

# Log HistoryService
logging.level.com.example.conges.service.HistoryService=INFO

# Log HistoryController
logging.level.com.example.conges.controller.HistoryController=INFO

# Log SQL queries (pour debug)
logging.level.org.hibernate.SQL=DEBUG
spring.jpa.properties.hibernate.format_sql=true
```

## ✅ Phase 10: Documentation & Training (10 minutes)

- [ ] Lire QUICK_START.md (5 min)
- [ ] Lire API_HISTORIQUE.md (20 min)
- [ ] Montrer aux utilisateurs RH comment:
  - [ ] Consulter l'historique
  - [ ] Filtrer par critères
  - [ ] Exporter en PDF
  - [ ] Exporter en Excel
  - [ ] Voir les statistiques

## ✅ Post-Implémentation: Checklists

### Avant Production

- [ ] Tous les tests passent (`mvn test`)
- [ ] Application déconstruit correctement
- [ ] BD backup en place
- [ ] Monitoring en place (logs)
- [ ] Documentation accessible

### En Production

- [ ] Table `history` remplie (records augmentent)
- [ ] Performances acceptables (< 2s par requête)
- [ ] Aucune erreur dans les logs
- [ ] Utilisateurs RH content de l'historique

## 🆘 Dépannage

| Symptôme                          | Cause Probable           | Solution                                                 |
| --------------------------------- | ------------------------ | -------------------------------------------------------- |
| Erreur 404 sur `/api/history`     | Route non enregistrée    | Controller scanné par Spring? Vérifier `@RestController` |
| Erreur 500 sur POST /api/demandes | HistoryService échoue    | Vérifier qu'elle est injectée dans CongeService          |
| Historique vide                   | History pas enregistrée  | Déboguer CongeService.creerDemande()                     |
| Export PDF vide                   | Aucune donnée à exporter | Vérifier data en BD avec SQL                             |
| Performance lente                 | Pas d'indices            | Exécuter le script history-table.sql                     |

---

**Une fois toutes les phases complétées ✅, le système est PRODUCTION READY!**
