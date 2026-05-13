# 📋 Audit des Menus par Utilisateur et Pays

## Utilisateurs et Pays Configurés

### Backend Mock (mock-backend.js)

| ID  | Utilisateur  | Email                        | Rôle     | Pays             | Département |
| --- | ------------ | ---------------------------- | -------- | ---------------- | ----------- |
| 1   | John Doe     | john@company.com             | EMPLOYEE | **TN** (Tunisie) | IT          |
| 2   | Sarah Martin | sarah@company.com            | EMPLOYEE | **FR** (France)  | RH          |
| 3   | RH Admin     | rh@company.com               | RH       | **TN** (Tunisie) | RH          |
| 4   | DRH Admin    | drh@company.com              | DRH      | **TN** (Tunisie) | RH          |
| 5   | Hamdi Sfaxi  | hamdihamdisfaxi123@gmail.com | EMPLOYEE | **TN** (Tunisie) | IT          |

---

## Menus Frontend par Rôle

### 1️⃣ Employés (EMPLOYEE)

**Menu affiché (depuis navbar.jsx)**

```
├── Accueil (/employee/dashboard)
├── Mon calendrier (/employee/calendar)
└── Mes demandes (/employee/historique)
```

**Sous-menus disponibles dans /employee/dashboard**

```
├── Créer une nouvelle demande
│   ├── Congé payé
│   ├── Congé maladie
│   ├── J'arrive en retard
│   ├── Permission courte durée (sortie courte)
│   ├── Congés sans solde
│   └── Congés exceptionnels
├── Mon solde
├── Mon calendrier
└── Mes demandes historique
```

### ✅ Employés TUNISIENS (TN) - Utilisateurs 1, 5

| Menu Item                     | Affichable | Notes                                       |
| ----------------------------- | ---------- | ------------------------------------------- |
| Accueil                       | ✅         | Dashboard employé TN                        |
| Mon calendrier                | ✅         | Horaires de travail TN (Normal/Été/Ramadan) |
| Mes demandes                  | ✅         | Historique filtré TN                        |
| Nouvelle demande (Congé payé) | ✅         | 30 jours/an TN                              |
| Nouvelle demande (Maladie)    | ✅         | 7 jours/an TN                               |
| J'arrive en retard            | ✅         | Débite "ARRIVE_AUTORISATION" (TN)           |
| Permission courte durée       | ⚠️         | Limitée à horaires normaux (TN non-RTT)     |
| Congés sans solde             | ✅         | Sans décompte                               |
| Congés exceptionnels          | ✅         | Configurable RH                             |

**Utilisateur 1: John Doe (TN)**

- Email: john@company.com
- Pays: Tunisie
- Manager: RH Admin (ID 3)

**Utilisateur 5: Hamdi Sfaxi (TN)**

- Email: hamdihamdisfaxi123@gmail.com
- Pays: Tunisie
- Manager: RH Admin (ID 3)

---

### ✅ Employés FRANÇAIS (FR) - Utilisateur 2

| Menu Item                     | Affichable | Notes                                     |
| ----------------------------- | ---------- | ----------------------------------------- |
| Accueil                       | ✅         | Dashboard employé FR                      |
| Mon calendrier                | ✅         | Horaires de travail FR (normal seulement) |
| Mes demandes                  | ✅         | Historique filtré FR                      |
| Nouvelle demande (Congé payé) | ✅         | 25 jours/an FR                            |
| Nouvelle demande (Maladie)    | ✅         | 7 jours/an FR (comme TN)                  |
| J'arrive en retard            | ✅         | Débite "SORTIE_COURTE" (FR)               |
| Permission courte durée (RTT) | ✅         | RTT France 2h/mois                        |
| Congés sans solde             | ✅         | Sans décompte                             |
| Congés exceptionnels          | ✅         | Configurable RH                           |

**Utilisateur 2: Sarah Martin (FR)**

- Email: sarah@company.com
- Pays: France
- Manager: RH Admin (ID 3)

---

## 2️⃣ RH / DRH (RH ou DRH)

**Menu affiché (depuis navbar.jsx)**

```
├── Accueil (/rh/dashboard)
├── Décisions RH (/rh/decisions)
├── Historique (/rh/requests)
├── Calendrier (/rh/calendar)
├── Paramètres (/rh/configuration)
├── Jours fériés (/rh/jours-feries)
└── Soldes (/rh/soldes)
```

### ✅ RH ADMIN (RH) - Utilisateur 3

| Menu Item              | Affichable | Notes                                 |
| ---------------------- | ---------- | ------------------------------------- |
| Accueil (Dashboard RH) | ✅         | Stats demandes toutes nationalités    |
| Décisions RH           | ✅         | Approbation/Rejet demandes            |
| Historique             | ✅         | Liste complète demandes               |
| Calendrier             | ✅         | Vue calendrier synth. par utilisateur |
| Paramètres             | ✅         | Config workflow, fériés, horaires     |
| Jours fériés           | ✅         | Gestion TN, FR, MA                    |
| Soldes                 | ✅         | Affiche soldes tous employés          |

**Utilisateur 3: RH Admin (RH)**

- Email: rh@company.com
- Pays: Tunisie
- Rôle: RH

---

### ✅ DRH ADMIN (DRH) - Utilisateur 4

| Menu Item              | Affichable | Notes                                   |
| ---------------------- | ---------- | --------------------------------------- |
| Accueil (Dashboard RH) | ✅         | Même que RH (full access)               |
| Décisions RH           | ✅         | Approbation/Rejet demandes (niveau DRH) |
| Historique             | ✅         | Liste complète demandes                 |
| Calendrier             | ✅         | Vue calendrier synth. par utilisateur   |
| Paramètres             | ✅         | Config workflow, fériés, horaires       |
| Jours fériés           | ✅         | Gestion TN, FR, MA                      |
| Soldes                 | ✅         | Affiche soldes tous employés            |

**Utilisateur 4: DRH Admin (DRH)**

- Email: drh@company.com
- Pays: Tunisie
- Rôle: DRH

---

## 📊 Résumé des Différences par Pays

### Tunisie (TN)

- **Congés payés**: 30 jours/an
- **Congé maladie**: 7 jours/an
- **Retard**: Débite "ARRIVE_AUTORISATION" (pas RTT)
- **Permission courte**: ⚠️ Limitée (2h max/mois)
- **Horaires**: Normal / Été / Ramadan
- **Emplois**: John Doe, Hamdi Sfaxi, RH Admin, DRH Admin

### France (FR)

- **Congés payés**: 25 jours/an
- **Congé maladie**: 7 jours/an (comme Tunisie)
- **Retard**: Débite "SORTIE_COURTE" (= RTT)
- **Permission courte (RTT)**: ✅ 2h/mois ou 1 jour
- **Horaires**: Normal seulement
- **Emplois**: Sarah Martin

### Maroc (MA)

- ⚠️ **Non configuré** en utilisateurs mock actuels
- Types: 22 jours/an congés payés, 7 jours maladie

---

## ⚙️ Logique Backend Implémentée

### Calcul Jours Selon Pays

```javascript
// France: Lun-Ven seulement, fériés excl.
// Tunisie: Lun-Ven sauf vendredi RTT + jours fériés TN
// Maroc: Lun-Ven sauf dimanche + jours fériés MA
```

### Quotas de Solde par Pays

**Tunisie**

- CONGES_PAYES: 30 jours
- CONGE_MALADIE: 7 jours
- CONGE_SANS_SOLDE: illimité
- SORTIE_COURTE: ⚠️ Limité (non-RTT)
- ARRIVE_AUTORISATION: débite sur SORTIE_COURTE

**France**

- CONGES_PAYES: 25 jours
- CONGE_MALADIE: ❌ Pas en mock
- CONGE_SANS_SOLDE: illimité
- SORTIE_COURTE (RTT): 2h/mois ou jours illimitée
- RETARD: débite sur SORTIE_COURTE

**Maroc**

- CONGES_PAYES: 22 jours
- CONGE_MALADIE: 7 jours
- Similar à TN

---

## 🔍 Points de Vérification

### ✅ À Tester

1. **Utilisateur 1 (John Doe - TN)**
   - [ ] Dashboard affiche "Congés payés: 30"
   - [ ] "J'arrive en retard" visible
   - [ ] Permission courte: message validation horaires TN
   - [ ] Calendrier: affiche Normal/Été/Ramadan

2. **Utilisateur 2 (Sarah Martin - FR)**
   - [ ] Dashboard affiche "Congés payés: 25"
   - [ ] "J'arrive en retard" visible (débite RTT)
   - [ ] Permission courte (RTT): 2h max
   - [ ] Calendrier: Normal seulement

3. **Utilisateur 3 (RH Admin - TN)**
   - [ ] Menu RH complet visible
   - [ ] Soldes page: affiche John (30 TN) + Sarah (25 FR) + Hamdi (30 TN)
   - [ ] Configuration: options tunisiennes

4. **Utilisateur 4 (DRH Admin - TN)**
   - [ ] Même menu que RH
   - [ ] Approbations dispo

---

## 📝 Notes

- Le menu est généré dynamiquement selon `user.role` (EMPLOYEE/RH/DRH)
- Le pays (`user.country`) affecte:
  - Quotas de jours
  - Formules calcul (lundi-vendredi vs autres)
  - Horaires d'ouverture
  - Règles de retard vs RTT
- Les menus ne sont **pas** cachés par pays (affichage pareil TN et FR)
- Les validations **backend** appliquent les règles pays
