# 🧪 Guide Test - Menus par Utilisateur

## 🔑 Identifiants de Connexion

Tous les utilisateurs utilisent le **mot de passe**: `password123`

### 1. John Doe (Employé Tunisie)

```
Email: john@company.com
Pays: Tunisie (TN)
Rôle: EMPLOYEE
```

**Étapes test**:

1. Se connecter avec `john@company.com` / `password123`
2. Vérifier le menu employé affiche 3 items: Accueil | Mon calendrier | Mes demandes
3. Aller à "Accueil" (Dashboard employé)
   - [ ] Solde affichage: "Congés payés: 30 jours" (TN)
   - [ ] Section "Nouvelle demande" avec options: Congé payé, Maladie, Retard, etc.
4. Tester créer une demande congé payé 3 jours
   - [ ] Calcul: doit être 3 jours ouvrables (lun-ven TN)
   - [ ] Validation solde: doit passer (30-3=27)
5. Aller à "Mon calendrier"
   - [ ] Affiche horaires Tunisie (Normal / Été / Ramadan)
6. Aller à "Mes demandes"
   - [ ] Affiche l'historique

---

### 2. Sarah Martin (Employée France)

```
Email: sarah@company.com
Pays: France (FR)
Rôle: EMPLOYEE
```

**Étapes test**:

1. Se connecter avec `sarah@company.com` / `password123`
2. Vérifier le menu employé (identique à John)
3. Aller à "Accueil"
   - [ ] Solde affichage: "Congés payés: 25 jours" (FR, pas 30!)
   - [ ] Solde affichage: "Congé maladie: 7 jours" (FR, maintenant actif!)
   - [ ] Section "Nouvelle demande"
4. Créer une demande congé payé de 5 jours
   - [ ] Calcul: 5 jours ouvrables (lun-ven France)
   - [ ] Validation: doit passer (25-5=20)
5. Tester "Permission courte durée" (RTT France)
   - [ ] Sélectionner une date
   - [ ] Entrer heure début: 09:00, heure fin: 11:00 (2 heures = 1 RTT)
   - [ ] Validation: doit passer
6. Aller à "Mon calendrier"
   - [ ] Affiche horaires France (Normal seulement, pas Ramadan!)
7. Aller à "Mes demandes"
   - [ ] Historique

---

### 3. Hamdi Sfaxi (Employé Tunisie)

```
Email: hamdihamdisfaxi123@gmail.com
Pays: Tunisie (TN)
Rôle: EMPLOYEE
```

**Étapes test**:

1. Se connecter avec `hamdihamdisfaxi123@gmail.com` / `password123`
2. Vérifier identique à John Doe (même pays TN)
3. Tests identiques à John Doe
4. Vérifier que les soldes sont indépendants (ne partage pas avec John)

---

### 4. RH Admin (Administrateur RH)

```
Email: rh@company.com
Pays: Tunisie (TN)
Rôle: RH
```

**Étapes test**:

1. Se connecter avec `rh@company.com` / `password123`
2. Vérifier menu RH complet:
   - [ ] Accueil (Dashboard RH)
   - [ ] Décisions RH
   - [ ] Historique
   - [ ] Calendrier
   - [ ] Paramètres
   - [ ] Jours fériés
   - [ ] Soldes

3. Aller à "Accueil" (RH Dashboard)
   - [ ] Affiche stats: demandes en attente, approuvées, refusées
   - [ ] Liste des demandes en cours

4. Aller à "Décisions RH"
   - [ ] Affiche les demandes en attente
   - [ ] Peut approuver/refuser les demandes de John, Sarah, Hamdi

5. Aller à "Soldes"
   - [ ] Affiche tous les employés:
     - John Doe: 30 jours (TN)
     - Sarah Martin: 25 jours (FR)
     - Hamdi Sfaxi: 30 jours (TN)
   - [ ] Peut éditer les soldes

6. Aller à "Jours fériés"
   - [ ] Affiche fériés Tunisie, France, Maroc (si configuré)

7. Aller à "Paramètres"
   - [ ] Options de configuration workflow

---

### 5. DRH Admin (Administrateur DRH)

```
Email: drh@company.com
Pays: Tunisie (TN)
Rôle: DRH
```

**Étapes test**:

1. Se connecter avec `drh@company.com` / `password123`
2. Vérifier menu **identique à RH** (même 7 items)
3. Tests identiques à RH Admin
4. Vérifier les permissions d'approbation (niveau DRH)

---

## ✅ Checklist de Vérification

### Menu Employé (John & Sarah)

- [ ] Affichage "Accueil", "Mon calendrier", "Mes demandes"
- [ ] Pas d'accès aux menus RH (Décisions, Configuration, etc.)
- [ ] Navbar affiche le prénom/email de l'utilisateur
- [ ] Navbar affiche le pays et département

### Différences TN vs FR (Employé)

#### Tunisie (John & Hamdi)

- [ ] Solde: 30 jours congés payés
- [ ] Solde: 7 jours maladie
- [ ] Calendrier: 3 options (Normal, Été, Ramadan)
- [ ] "J'arrive en retard": débite autorisation arrivée (pas RTT)
- [ ] "Permission courte": limitée (non-RTT)

#### France (Sarah)

- [ ] Solde: 25 jours congés payés
- [ ] Solde: 7 jours maladie (maintenant actif!)
- [ ] Calendrier: 1 option (Normal seulement)
- [ ] "J'arrive en retard": débite RTT
- [ ] "Permission courte (RTT)": 2h max par mois

### Menu RH/DRH (RH Admin & DRH Admin)

- [ ] 7 items visibles: Accueil, Décisions, Historique, Calendrier, Paramètres, Jours fériés, Soldes
- [ ] Dashboard RH affiche stats
- [ ] Soldes page: 5 utilisateurs (John TN, Sarah FR, Hamdi TN, RH Admin TN, DRH Admin TN)
- [ ] Jours fériés: peut gérer TN, FR, MA
- [ ] Peut approuver/refuser demandes

---

## 📋 Tableau Résumé des Menus

| Fonction               | John (TN)    | Sarah (FR)  | RH Admin   | DRH Admin  |
| ---------------------- | ------------ | ----------- | ---------- | ---------- |
| Accueil                | ✅           | ✅          | ✅         | ✅         |
| Mon calendrier         | ✅ (3 modes) | ✅ (1 mode) | ✅         | ✅         |
| Mes demandes           | ✅           | ✅          | —          | —          |
| Décisions RH           | ❌           | ❌          | ✅         | ✅         |
| Historique             | ❌           | ❌          | ✅         | ✅         |
| Paramètres             | ❌           | ❌          | ✅         | ✅         |
| Jours fériés           | ❌           | ❌          | ✅         | ✅         |
| Soldes                 | ❌           | ❌          | ✅         | ✅         |
| **Solde Congés Payés** | 30 j.        | 25 j.       | 30 j. (TN) | 30 j. (TN) |
| **Pays**               | TN           | FR          | TN         | TN         |

---

## 🐛 Bugs à Vérifier

- [ ] Soldes incorrects (ne correspond pas au pays)
- [ ] Menus visibles pour mauvais rôle
- [ ] Calcul jours incorrect selon pays (TN vs FR)
- [ ] Permission courte autorisée hors horaires
- [ ] RTT France: limite 2h/mois non respectée
- [ ] Pays affichés incorrectement en navbar

---

## 💾 Export & Sauvegarde des Données de Test

Les données mock sont sauvegardées dans:

```
c:\Users\Asus\Desktop\gestion-conges-backend-main\mock-persist.json
```

Pour réinitialiser les données, supprimez ce fichier et redémarrez le backend.
