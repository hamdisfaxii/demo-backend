# 📋 Guide Intégration SMTP - Production Ready

## ✅ Complété

### 1️⃣ Configuration SMTP Réelle (Gmail)

**Fichiers créés/modifiés:**

#### Backend - Java Spring Boot
- ✅ `src/main/resources/application.properties` - Configuration avec variables d'environnement
- ✅ `src/main/java/com/example/conges/controller/EmailTestController.java` - Endpoints de test

#### Backend - Node.js
- ✅ `test-email-config.js` - Script de test SMTP
- ✅ `package.json` - Scripts `test:email` et `test:smtp`

#### Configuration Sécurisée
- ✅ `.env.example` - Template variables d'environnement
- ✅ `.gitignore` - Ignore `.env` (secrets)

---

## 🚀 Prochaines Étapes

### 2️⃣ Configuration Gmail (Instructions utilisateur)

**À faire par l'utilisateur:**

1. **Accéder à Google Account**
   ```
   https://myaccount.google.com → Sécurité
   ```

2. **Activer 2FA (si pas déjà activé)**
   - Vérifier que l'authentification 2FA est activée

3. **Générer App Password**
   ```
   https://myaccount.google.com/apppasswords
   → Sélectionner: Mail + Autres (personnalisé)
   → Google génère un code 16 caractères
   ```

4. **Créer fichier .env**
   ```bash
   cp .env.example .env
   ```

5. **Remplir les credentials**
   ```env
   MAIL_USERNAME=votre-email@gmail.com
   MAIL_PASSWORD=abcdefghijklmnop  # Sans espaces
   MAIL_FROM_EMAIL=votre-email@gmail.com
   ```

---

## 🧪 Tester la Configuration

### Option A: Node.js (Rapide)
```bash
cd gestion-conges-backend
npm install dotenv
npm run test:email
```

**Résultat attendu:**
```
✅ Found: MAIL_HOST = smt***
✅ Found: MAIL_PORT = 587***
✅ Found: MAIL_USERNAME = vot***
✅ Found: MAIL_PASSWORD = abc***
✅ SMTP Connection Verified!
✅ Test email sent successfully!
```

### Option B: Spring Boot (Complet)

1. **Lancer le backend**
   ```bash
   mvn spring-boot:run
   ```

2. **Vérifier configuration**
   ```bash
   curl http://localhost:8080/api/test/smtp-config
   ```

   **Réponse:**
   ```json
   {
     "status": "✅ SUCCESS",
     "message": "SMTP Configuration is valid",
     "mailUsername": "v***",
     "fromName": "Gestion des Congés"
   }
   ```

3. **Envoyer email de test**
   ```bash
   curl -X POST http://localhost:8080/api/test/send-email \
     -H "Content-Type: application/json" \
     -d '{
       "to": "votre-email@gmail.com",
       "subject": "Test Email",
       "message": "Ceci est un email de test"
     }'
   ```

4. **Vérifier connexion SMTP**
   ```bash
   curl -X POST http://localhost:8080/api/test/smtp-verify
   ```

---

## 🔧 Endpoints API Disponibles

### 1. Vérifier la Configuration
```
GET /api/test/smtp-config
```

**Réponse:**
```json
{
  "status": "✅ SUCCESS",
  "mailHost": "smtp.gmail.com",
  "mailPort": 587,
  "mailUsername": "v***@gmail.com",
  "fromName": "Gestion des Congés"
}
```

### 2. Envoyer Email de Test
```
POST /api/test/send-email
Content-Type: application/json

{
  "to": "recipient@gmail.com",
  "subject": "Mon Sujet",
  "message": "Mon message"
}
```

**Réponse:**
```json
{
  "status": "✅ SUCCESS",
  "message": "Email sent successfully",
  "recipient": "recipient@gmail.com",
  "timestamp": 1703091234567
}
```

### 3. Vérifier Connexion SMTP
```
POST /api/test/smtp-verify
```

**Réponse OK:**
```json
{
  "status": "✅ VERIFIED",
  "message": "SMTP connection successful",
  "host": "smtp.gmail.com",
  "port": 587
}
```

**Réponse Erreur Auth:**
```json
{
  "status": "❌ AUTH_FAILED",
  "message": "SMTP Authentication failed - Check credentials",
  "hint": "Verify App Password is correct and 2FA is enabled on Gmail"
}
```

---

## 🐛 Dépannage

### Erreur: `535 5.7.8 Username and Password not accepted`
- **Cause:** Credentials incorrects
- **Solution:**
  - Vérifier le App Password (16 chars sans espaces)
  - Vérifier que 2FA est activé
  - Régénérer le App Password

### Erreur: `Connection timeout`
- **Cause:** Port ou host incorrect
- **Solution:**
  - Vérifier: Host = `smtp.gmail.com`, Port = `587`
  - Vérifier la connexion internet

### Erreur: `Mail not sent: 550 5.1.1`
- **Cause:** Email destinataire invalide
- **Solution:**
  - Vérifier l'adresse email de test

---

## 📊 Swagger Documentation

Une fois le backend lancé, accédez à:
```
http://localhost:8080/swagger-ui.html
```

Vous verrez tous les endpoints y compris:
- `/api/test/smtp-config` - Vérifier config
- `/api/test/send-email` - Envoyer email
- `/api/test/smtp-verify` - Vérifier connexion

---

## ✨ Prochaines Phases

### Phase 2: Tests Email
- [ ] Tester notifications automatiques
- [ ] Tester templates email
- [ ] Tester emails en lot

### Phase 3: Intégration Frontend
- [ ] Ajouter composants AI au formulaire
- [ ] Refonte CSS (Tailwind)
- [ ] Tester workflow complet

### Phase 4: Déploiement
- [ ] Build: `mvn clean install`
- [ ] Docker/Kubernetes
- [ ] Variables d'environnement prod
- [ ] Tests JUnit

---

## 📝 Checklist Configuration

- [ ] Google Account 2FA activé
- [ ] App Password généré (16 chars)
- [ ] `.env` file créé avec credentials
- [ ] `.env` dans `.gitignore`
- [ ] Test Node.js réussi (`npm run test:email`)
- [ ] Test Spring Boot réussi (`/api/test/smtp-verify`)
- [ ] Email de test reçu dans Gmail inbox
- [ ] Swagger documenté et accessible
