# API Historique - Documentation Complète

## Vue d'ensemble

Le système d'historique enregistre automatiquement toutes les actions effectuées sur les demandes de congés. Chaque action est tracée avec:

- Utilisateur qui a effectué l'action
- Type d'action (CREATE, APPROVE, REJECT, etc.)
- Date et heure de l'action
- Description et détails
- Pays concerné
- Statut de la demande
- Adresse IP et User-Agent du client

## Endpoints

### 1. GET /api/history

Récupère l'historique avec filtres et pagination.

**Parameters:**

```
GET /api/history?page=0&size=20&userId=1&actionType=APPROVE&pays=TN&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

| Paramètre    | Type     | Requis | Description                                                   |
| ------------ | -------- | ------ | ------------------------------------------------------------- |
| `userId`     | Long     | Non    | Filtrer par utilisateur                                       |
| `demandeId`  | Long     | Non    | Filtrer par demande de congé                                  |
| `actionType` | String   | Non    | Type d'action (CREATE, APPROVE, REJECT, CANCEL, UPDATE, etc.) |
| `pays`       | String   | Non    | Filtrer par pays (TN, MA, FR)                                 |
| `startDate`  | DateTime | Non    | Date de début (ISO format)                                    |
| `endDate`    | DateTime | Non    | Date de fin (ISO format)                                      |
| `page`       | Integer  | Non    | Numéro de page (défaut: 0)                                    |
| `size`       | Integer  | Non    | Taille de page (défaut: 20)                                   |
| `sort`       | String   | Non    | Tri (défaut: actionDate,desc)                                 |

**Response (200 OK):**

```json
{
  "content": [
    {
      "id": 1,
      "user": {
        "id": 1,
        "nom": "Dupont",
        "prenom": "Jean",
        "email": "jean.dupont@example.com"
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
  ],
  "totalElements": 150,
  "totalPages": 8,
  "currentPage": 0
}
```

### 2. GET /api/history/user/{userId}

Récupère l'historique d'un utilisateur spécifique.

**Exemple:**

```
GET /api/history/user/1?page=0&size=10
```

**Response (200 OK):** Même format que /api/history

### 3. GET /api/history/demande/{demandeId}

Récupère l'historique d'une demande de congé spécifique.

**Exemple:**

```
GET /api/history/demande/5?page=0&size=10
```

**Response (200 OK):** Même format que /api/history

### 4. GET /api/history/statistics

Récupère les statistiques sur les actions enregistrées.

**Response (200 OK):**

```json
{
  "CREATE": 145,
  "SUBMIT": 142,
  "APPROVE": 120,
  "REJECT": 15,
  "CANCEL": 8,
  "UPDATE": 25,
  "DOCUMENT_SENT": 0,
  "EXPORTED": 5,
  "SYNCED_DOLIBARR": 0,
  "LOGIN": 2150,
  "LOGOUT": 2100,
  "REPORT_VIEWED": 45,
  "OTHER": 0
}
```

### 5. GET /api/history/export/pdf

Exporte l'historique au format PDF.

**Parameters:** Mêmes que /api/history pour le filtrage

**Exemple:**

```
GET /api/history/export/pdf?actionType=APPROVE&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

**Response (200 OK):** Fichier PDF

```
Content-Type: application/pdf
Content-Disposition: attachment; filename="historique_1704067200000.pdf"
```

### 6. GET /api/history/export/excel

Exporte l'historique au format Excel (.xlsx).

**Parameters:** Mêmes que /api/history pour le filtrage

**Exemple:**

```
GET /api/history/export/excel?pays=TN&startDate=2024-01-01T00:00:00
```

**Response (200 OK):** Fichier Excel

```
Content-Type: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet
Content-Disposition: attachment; filename="historique_1704067200000.xlsx"
```

### 7. GET /api/history/export/rh-report

Exporte un rapport RH spécifique au format Excel.

**Parameters:**

- `pays` (String, optionnel) - Filtrer par pays
- `startDate` (DateTime, optionnel) - Date de début
- `endDate` (DateTime, optionnel) - Date de fin

**Exemple:**

```
GET /api/history/export/rh-report?pays=TN&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59
```

**Response (200 OK):** Fichier Excel avec rapport RH formaté

## Types d'Actions (ActionType)

| Code              | Description                     |
| ----------------- | ------------------------------- |
| `CREATE`          | Création d'une demande de congé |
| `SUBMIT`          | Soumission d'une demande        |
| `APPROVE`         | Approbation par RH/Manager      |
| `REJECT`          | Rejet d'une demande             |
| `CANCEL`          | Annulation d'une demande        |
| `UPDATE`          | Modification d'une demande      |
| `DOCUMENT_SENT`   | Document envoyé                 |
| `EXPORTED`        | Export effectué (PDF/Excel)     |
| `SYNCED_DOLIBARR` | Synchronisation avec Dolibarr   |
| `LOGIN`           | Connexion utilisateur           |
| `LOGOUT`          | Déconnexion utilisateur         |
| `REPORT_VIEWED`   | Rapport consulté                |
| `OTHER`           | Autre action                    |

## Sécurité & Permissions

| Endpoint                               | Rôles requis                         |
| -------------------------------------- | ------------------------------------ |
| `GET /api/history`                     | RH, ADMIN                            |
| `GET /api/history/user/{userId}`       | RH, ADMIN, ou l'utilisateur lui-même |
| `GET /api/history/demande/{demandeId}` | RH, ADMIN, EMPLOYEE                  |
| `GET /api/history/statistics`          | RH, ADMIN                            |
| `GET /api/history/export/pdf`          | RH, ADMIN                            |
| `GET /api/history/export/excel`        | RH, ADMIN                            |
| `GET /api/history/export/rh-report`    | RH, ADMIN                            |

## Exemples de Requêtes cURL

### Récupérer l'historique des approbations du mois dernier

```bash
curl -X GET "http://localhost:8080/api/history?actionType=APPROVE&startDate=2024-02-01T00:00:00&endDate=2024-02-29T23:59:59" \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json"
```

### Exporter l'historique d'un utilisateur en PDF

```bash
curl -X GET "http://localhost:8080/api/history/export/pdf?userId=1" \
  -H "Authorization: Bearer {TOKEN}" \
  -o "historique_user_1.pdf"
```

### Exporter le rapport RH par pays

```bash
curl -X GET "http://localhost:8080/api/history/export/rh-report?pays=TN&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59" \
  -H "Authorization: Bearer {TOKEN}" \
  -o "rapport_rh_tunisie.xlsx"
```

### Récupérer les statistiques

```bash
curl -X GET "http://localhost:8080/api/history/statistics" \
  -H "Authorization: Bearer {TOKEN}" \
  -H "Content-Type: application/json"
```

## Enregistrement Automatique (AOP)

Les actions suivantes sont enregistrées automatiquement:

1. **Création d'une demande**

   ```
   POST /api/demandes → ActionType.CREATE
   ```

2. **Validation d'une demande**

   ```
   PUT /api/demandes/{id}/valider → ActionType.APPROVE/REJECT
   ```

3. **Annulation d'une demande**

   ```
   PUT /api/demandes/{id}/annuler → ActionType.CANCEL
   ```

4. **Connexion utilisateur**
   ```
   POST /api/auth/login → ActionType.LOGIN
   ```

## Filtrage Avancé

Vous pouvez combiner les filtres:

```
GET /api/history?userId=1&actionType=APPROVE&pays=TN&startDate=2024-01-01T00:00:00&endDate=2024-12-31T23:59:59&page=0&size=50
```

Cette requête récupère toutes les approbations de l'utilisateur 1 en Tunisie pour l'année 2024, avec pagination.

## Stockage des Détails (JSON)

Le champ `details` contient les informations détaillées en format JSON:

**Exemple CREATE:**

```json
{
  "typeConge": "PAYE",
  "dateDebut": "2024-03-01",
  "dateFin": "2024-03-05",
  "nombreJours": "4"
}
```

**Exemple APPROVE:**

```json
{
  "approver": "Jean Dupont",
  "previousStatus": "EN_ATTENTE",
  "newStatus": "ACCEPTE"
}
```

**Exemple REJECT:**

```json
{
  "reason": "Période déjà couverte",
  "previousStatus": "EN_ATTENTE",
  "newStatus": "REFUSE"
}
```

## Aide au Débogage

### Vérifier la table History

```sql
SELECT * FROM history ORDER BY action_date DESC LIMIT 10;
SELECT COUNT(*) FROM history;
```

### Compter les actions par type

```sql
SELECT action_type, COUNT(*) FROM history GROUP BY action_type;
```

### Historique d'un utilisateur

```sql
SELECT * FROM history WHERE user_id = 1 ORDER BY action_date DESC;
```

### Historique d'une demande

```sql
SELECT * FROM history WHERE demande_id = 5 ORDER BY action_date DESC;
```
