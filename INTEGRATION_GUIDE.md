# 🔗 Guide d'Intégration Complète - Système d'Historique

## Vue d'ensemble du Flux

```
┌─────────────────────┐
│  Frontend (React)   │
└──────────┬──────────┘
           │ POST /api/demandes
           ▼
┌─────────────────────┐
│ DemandeController   │ ────► creerDemande(userId, request)
└──────────┬──────────┘
           │
           ▼
┌──────────────────────────┐
│    CongeService          │
│  - verifier solde        │
│  - sauvegarder demande   │ ────► historyService.recordCreation()
│  - logguer action        │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│   HistoryService         │
│  - créer record History  │
│  - extraire IP/UA        │
│  - sérialiser détails    │
└──────────┬───────────────┘
           │
           ▼
┌──────────────────────────┐
│  HistoryRepository       │
│  - save() dans BD        │
└──────────────────────────┘
```

## Flux Détaillé pour Chaque Action

### 1️⃣ Création d'une Demande

**Requête HTTP:**

```http
POST /api/demandes
Authorization: Bearer {JWT_TOKEN}
Content-Type: application/json

{
  "typeConge": "PAYE",
  "dateDebut": "2024-03-01",
  "dateFin": "2024-03-05",
  "motif": "Repos annuel"
}
```

**Flux Backend:**

```java
// 1. DemandeController
@PostMapping
public ResponseEntity<DemandeCongeResponse> creerDemande(
        @AuthenticationPrincipal UserEntity user,
        @Valid @RequestBody DemandeCongeRequest request
) {
    // Extrait l'utilisateur du token JWT
    DemandeCongeResponse body = congeService.creerDemande(user.getId(), request);
    return ResponseEntity.status(HttpStatus.CREATED).body(body);
}

// 2. CongeService.creerDemande()
@Transactional
public DemandeCongeResponse creerDemande(Long userId, DemandeCongeRequest request) {
    // Vérifie que l'utilisateur existe
    UserEntity user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur introuvable"));

    // Valide les dates
    if (request.getDateFin().isBefore(request.getDateDebut())) {
        throw new IllegalArgumentException("...");
    }

    // Calcule les jours ouvrables
    int joursOuvrables = calculerJoursOuvrables(request.getDateDebut(), request.getDateFin());

    // Vérifie le solde disponible
    verifierSoldeDisponible(userId, request.getTypeConge(), joursOuvrables);

    // Crée la demande
    DemandeConge demande = DemandeConge.builder()
            .user(user)
            .typeConge(request.getTypeConge())
            .dateDebut(request.getDateDebut())
            .dateFin(request.getDateFin())
            .nombreJours(joursOuvrables)
            .motif(request.getMotif())
            .statut(StatutConge.EN_ATTENTE)
            .build();

    // Sauvegarde dans la BD
    DemandeConge saved = demandeCongeRepository.save(demande);

    log.info("Demande de congé créée id={} pour userId={}", saved.getId(), userId);

    // ✅ ENREGISTRE AUTOMATIQUEMENT DANS L'HISTORIQUE
    historyService.recordCreation(user, saved);

    return toResponse(saved);
}

// 3. HistoryService.recordCreation()
@Transactional
public void recordCreation(UserEntity user, DemandeConge demande) {
    String description = "Demande de congé créée";
    String details = buildDetails(
            "typeConge", demande.getTypeConge().toString(),
            "dateDebut", demande.getDateDebut().toString(),
            "dateFin", demande.getDateFin().toString(),
            "nombreJours", String.valueOf(demande.getNombreJours())
    );
    recordAction(user, demande, ActionType.CREATE, description, details);
}

// 4. HistoryService.recordAction()
@Transactional
public void recordAction(UserEntity user, DemandeConge demande, ActionType actionType,
                       String description, String details) {
    try {
        History history = History.builder()
                .user(user)                              // user_id = 1
                .demande(demande)                       // demande_id = 5
                .actionType(actionType)                 // "CREATE"
                .description(description)               // "Demande de congé créée"
                .details(details)                       // JSON des détails
                .pays(user.getPays())                   // "TN"
                .statut(demande.getStatut().toString()) // "EN_ATTENTE"
                .ipAddress(getClientIpAddress())        // "192.168.1.100"
                .userAgent(getUserAgent())              // "Mozilla/5.0..."
                .actionDate(LocalDateTime.now())        // 2024-03-15 10:30:00
                .build();

        // Sauvegarde dans BD
        historyRepository.save(history);

        log.info("Action enregistrée: {} pour l'utilisateur {}", actionType, user.getId());
    } catch (Exception e) {
        log.error("Erreur lors de l'enregistrement de l'historique", e);
        // Ne pas interrompre le flux métier
    }
}

// 5. HistoryRepository.save()
// Effectue l'INSERT dans la table history
// INSERT INTO history (user_id, demande_id, action_type, description, details, pays, statut, action_date, ip_address, user_agent)
// VALUES (1, 5, 'CREATE', 'Demande de congé créée', '{...}', 'TN', 'EN_ATTENTE', NOW(), '192.168.1.100', 'Mozilla/5.0...')
```

**Résultat BD (history table):**

```
| id | user_id | demande_id | action_type | description            | details                                    | pays | statut     | action_date         | ip_address    |
|----|---------|------------|-------------|------------------------|-------------------------------------------|------|------------|---------------------|---------------|
| 1  | 1       | 5          | CREATE      | Demande de congé créée | {"typeConge":"PAYE","nombreJours":"4"...} | TN   | EN_ATTENTE | 2024-03-15 10:30:00 | 192.168.1.100 |
```

---

### 2️⃣ Validation (Approbation ou Rejet)

**Requête HTTP:**

```http
PUT /api/demandes/5/valider
Authorization: Bearer {JWT_RH_TOKEN}
Content-Type: application/json

{
  "accepte": true,
  "commentaire": "Approuvé"
}
```

**Flux Backend:**

```java
// 1. DemandeController
@PutMapping("/{id}/valider")
@PreAuthorize("hasRole('RH')")
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

// 2. CongeService.validerDemande()
@Transactional
public DemandeCongeResponse validerDemande(
        Long demandeId,
        Long rhId,
        boolean accepte,
        String commentaire
) {
    UserEntity rh = userRepository.findById(rhId)
            .orElseThrow(() -> new EntityNotFoundException("Utilisateur RH introuvable"));

    if (rh.getRole() != Role.RH) {
        throw new AccessDeniedException("Seul un utilisateur RH peut valider");
    }

    DemandeConge demande = demandeCongeRepository.findById(demandeId)
            .orElseThrow(() -> new EntityNotFoundException("Demande introuvable"));

    if (demande.getStatut() != StatutConge.EN_ATTENTE) {
        throw new IllegalStateException("Seules les demandes en attente peuvent être validées");
    }

    // Change le statut
    demande.setStatut(accepte ? StatutConge.ACCEPTE : StatutConge.REFUSE);
    demande.setDateTraitement(LocalDateTime.now());
    demande.setCommentaireRh(commentaire);

    DemandeConge saved = demandeCongeRepository.save(demande);

    log.info("Demande id={} {} par rhId={}", demandeId, accepte ? "acceptée" : "refusée", rhId);

    // ✅ ENREGISTRE LA VALIDATION DANS L'HISTORIQUE
    UserEntity employe = demande.getUser();
    if (accepte) {
        historyService.recordApproval(employe, saved, rh.getPrenom() + " " + rh.getNom());
    } else {
        historyService.recordRejection(employe, saved, commentaire);
    }

    return toResponse(saved);
}

// 3. HistoryService.recordApproval() OU recordRejection()
@Transactional
public void recordApproval(UserEntity user, DemandeConge demande, String approverName) {
    String description = "Demande approuvée par " + approverName;
    String details = buildDetails(
            "approver", approverName,
            "previousStatus", "EN_ATTENTE",
            "newStatus", demande.getStatut().toString()
    );
    recordAction(user, demande, ActionType.APPROVE, description, details);
}

// 4. HistoryRepository.save()
// INSERT INTO history (...) VALUES (..., 'APPROVE', 'Demande approuvée par Dupont Jean', ...)
```

**Résultat BD (history table):**

```
| id | user_id | demande_id | action_type | description                    | details                                                   | statut |
|----|---------|------------|-------------|--------------------------------|-----------------------------------------------------------|--------|
| 1  | 1       | 5          | CREATE      | Demande de congé créée         | {"typeConge":"PAYE"...}                                   | EN_ATTENTE |
| 2  | 1       | 5          | APPROVE     | Demande approuvée par Dupont Jean | {"approver":"Dupont Jean","previousStatus":"EN_ATTENTE","newStatus":"ACCEPTE"} | ACCEPTE |
```

---

### 3️⃣ Consultation de l'Historique

**Requête HTTP:**

```http
GET /api/history?demandeId=5&page=0&size=10
Authorization: Bearer {JWT_RH_TOKEN}
```

**Flux Backend:**

```java
// 1. HistoryController.getHistory()
@GetMapping
@PreAuthorize("hasAnyRole('RH', 'ADMIN')")
public ResponseEntity<Page<History>> getHistory(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long demandeId,
        @RequestParam(required = false) ActionType actionType,
        @RequestParam(required = false) String pays,
        @RequestParam(required = false) LocalDateTime startDate,
        @RequestParam(required = false) LocalDateTime endDate,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(defaultValue = "actionDate,desc") String[] sort) {

    Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "actionDate"));

    // Appelle le service
    Page<History> result = historyService.getHistory(userId, demandeId, actionType, pays, startDate, endDate, pageable);

    log.info("Historique consulté - {} records trouvés", result.getTotalElements());
    return ResponseEntity.ok(result);
}

// 2. HistoryService.getHistory()
@Transactional(readOnly = true)
public Page<History> getHistory(Long userId, Long demandeId, ActionType actionType,
                                String pays, LocalDateTime startDate, LocalDateTime endDate,
                                Pageable pageable) {
    // Utilise la requête JPA personnalisée du repository
    return historyRepository.searchHistory(userId, demandeId, actionType, pays, startDate, endDate, pageable);
}

// 3. HistoryRepository.searchHistory()
// Requête JPA :
// SELECT h FROM History h
// WHERE (:demandeId IS NULL OR h.demande.id = :demandeId)
// AND (:actionType IS NULL OR h.actionType = :actionType)
// AND ... (autres filtres)
// ORDER BY h.actionDate DESC
// LIMIT 10
```

**Réponse HTTP (200 OK):**

```json
{
  "content": [
    {
      "id": 2,
      "user": {
        "id": 1,
        "nom": "Dupont",
        "prenom": "Jean",
        "email": "jean@example.com"
      },
      "demandeId": 5,
      "actionType": "APPROVE",
      "description": "Demande approuvée par Dupont Jean",
      "details": "{\"approver\":\"Dupont Jean\",\"previousStatus\":\"EN_ATTENTE\",\"newStatus\":\"ACCEPTE\"}",
      "pays": "TN",
      "statut": "ACCEPTE",
      "actionDate": "15/03/2024 11:00:00",
      "ipAddress": "192.168.1.101",
      "userAgent": "Mozilla/5.0..."
    },
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
      "actionDate": "15/03/2024 10:30:00",
      "ipAddress": "192.168.1.100",
      "userAgent": "Mozilla/5.0..."
    }
  ],
  "totalElements": 2,
  "totalPages": 1,
  "currentPage": 0,
  "pageSize": 10
}
```

---

### 4️⃣ Export en PDF

**Requête HTTP:**

```http
GET /api/history/export/pdf?demandeId=5&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
Authorization: Bearer {JWT_RH_TOKEN}
```

**Flux Backend:**

```java
// 1. HistoryController.exportHistoryPdf()
@GetMapping("/export/pdf")
@PreAuthorize("hasAnyRole('RH', 'ADMIN')")
public ResponseEntity<byte[]> exportHistoryPdf(
        @RequestParam(required = false) Long userId,
        @RequestParam(required = false) Long demandeId,
        // ... autres paramètres
) {
    try {
        // Récupère les records sans pagination
        List<History> historyList = historyService.getHistoryForExport(
                userId, demandeId, actionType, pays, startDate, endDate
        );

        // Génère le PDF
        byte[] pdfBytes = pdfExportService.generateHistoryReport(
                historyList,
                "Rapport Historique - Gestion des Congés"
        );

        log.info("Rapport PDF généré - {} records", historyList.size());

        // Retourne le fichier PDF en téléchargement
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"historique_1704067200000.pdf\"")
                .header(HttpHeaders.CONTENT_TYPE, "application/pdf")
                .body(pdfBytes);

    } catch (IOException e) {
        log.error("Erreur lors de la génération du PDF", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
    }
}

// 2. HistoryService.getHistoryForExport()
@Transactional(readOnly = true)
public List<History> getHistoryForExport(Long userId, Long demandeId, ...) {
    // Requête spécialisée pour grands volumes (pas de pagination)
    return historyRepository.findByActionDateBetweenOrderByActionDateDesc(startDate, endDate);
}

// 3. PdfExportService.generateHistoryReport()
public byte[] generateHistoryReport(List<History> historyList, String title) throws IOException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    PdfWriter writer = new PdfWriter(baos);
    PdfDocument pdfDoc = new PdfDocument(writer);
    Document document = new Document(pdfDoc);

    // Configuration des polices (iText7)
    PdfFont titleFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

    // Ajoute le titre
    Paragraph titleParagraph = new Paragraph(title)
            .setFont(titleFont)
            .setFontSize(16)
            .setTextAlignment(TextAlignment.CENTER);
    document.add(titleParagraph);

    // Ajoute un tableau avec les données
    Table table = new Table(new float[]{1, 1.5f, 1.5f, 2f, 1.5f, 1.5f});

    // En-têtes
    table.addHeaderCell("Date");
    table.addHeaderCell("Utilisateur");
    table.addHeaderCell("Action");
    table.addHeaderCell("Description");
    table.addHeaderCell("Demande ID");
    table.addHeaderCell("Statut");

    // Remplir les données
    for (History history : historyList) {
        table.addCell(history.getActionDate().format(...));
        table.addCell(history.getUser().getPrenom() + " " + history.getUser().getNom());
        table.addCell(history.getActionType().toString());
        table.addCell(history.getDescription());
        table.addCell(history.getDemande() != null ? history.getDemande().getId().toString() : "-");
        table.addCell(history.getStatut());
    }

    document.add(table);
    document.close();

    return baos.toByteArray();
}
```

**Réponse HTTP (200 OK):**

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="historique_1704067200000.pdf"
Content-Length: 15234

[Données binaires du fichier PDF]
```

**Fichier PDF généré:**

- Titre : "RAPPORT HISTORIQUE - GESTION DES CONGÉS"
- Tableau avec colonnes : Date, Utilisateur, Action, Description, Demande ID, Statut
- Footer : "Rapport confidentiel - Système de Gestion des Congés"

---

### 5️⃣ Export en Excel

**Requête HTTP:**

```http
GET /api/history/export/excel?pays=TN&actionType=APPROVE
Authorization: Bearer {JWT_RH_TOKEN}
```

**Flux Backend (Similaire au PDF):**

```java
// ExcelExportService.generateHistoryExcel()
public byte[] generateHistoryExcel(List<History> historyList, String sheetName) throws IOException {
    XSSFWorkbook workbook = new XSSFWorkbook();
    XSSFSheet sheet = workbook.createSheet(sheetName);

    // Styles
    XSSFCellStyle headerStyle = createHeaderStyle(workbook);
    XSSFCellStyle cellStyle = createCellStyle(workbook);

    // Ligne d'en-têtes
    XSSFRow headerRow = sheet.createRow(0);
    int colIndex = 0;
    for (String header : new String[]{"Date", "Utilisateur", "Email", "Action", ...}) {
        XSSFCell cell = headerRow.createCell(colIndex++);
        cell.setCellValue(header);
        cell.setCellStyle(headerStyle);
    }

    // Remplissage des données
    int rowNum = 1;
    for (History history : historyList) {
        XSSFRow row = sheet.createRow(rowNum++);
        row.createCell(0).setCellValue(history.getActionDate().toString());
        row.createCell(1).setCellValue(history.getUser().getPrenom() + " " + history.getUser().getNom());
        // ... autres colonnes
    }

    // Autosize des colonnes
    for (int i = 0; i < headers.length; i++) {
        sheet.autoSizeColumn(i);
    }

    // Gel la première ligne
    sheet.createFreezePane(0, 1);

    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    workbook.write(baos);
    workbook.close();

    return baos.toByteArray();
}
```

**Réponse HTTP (200 OK):**

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="historique_1704067200000.xlsx"
Content-Length: 25432

[Données binaires du fichier Excel]
```

---

## 📊 Résumé Complet du Flux

```
1. Utilisateur crée une demande
   → POST /api/demandes
   → CongeService.creerDemande()
   → DemandeConge save()
   → HistoryService.recordCreation() ✅
   → History save()

2. RH valide la demande
   → PUT /api/demandes/5/valider
   → CongeService.validerDemande()
   → DemandeConge update()
   → HistoryService.recordApproval() ✅
   → History save()

3. RH consulte l'historique
   → GET /api/history?demandeId=5
   → HistoryController.getHistory()
   → HistoryService.getHistory()
   → HistoryRepository.searchHistory()
   → Page<History> retourné

4. RH exporte en PDF
   → GET /api/history/export/pdf
   → HistoryService.getHistoryForExport()
   → PdfExportService.generateHistoryReport()
   → byte[] PDF retourné

5. RH exporte en Excel
   → GET /api/history/export/excel
   → HistoryService.getHistoryForExport()
   → ExcelExportService.generateHistoryExcel()
   → byte[] Excel retourné
```

---

## 🎯 Points Clés

✅ **Enregistrement Automatique** - HistoryService appelé automatiquement  
✅ **Pas d'Rupture du Flux** - Erreurs loggées, pas de throw  
✅ **Pagination** - Gestion des grands datasets  
✅ **Filtres Multi-Critères** - Recherche avancée  
✅ **Export Professionnel** - PDF et Excel formatés  
✅ **Sécurité** - Authentification + Autorisation par rôle  
✅ **Traçabilité Complète** - IP, User-Agent, timestamps  
✅ **Performances** - Indices BD, requêtes optimisées

---

**Cette intégration garantit que TOUTES les actions sont tracées et disponibles pour audit, reporting et analyse.**
