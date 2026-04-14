# 🎓 SmartPath – Educational Management Platform

---

## 📌 Overview

SmartPath est une application desktop développée avec JavaFX dans le cadre du module PIDEV (3ème année) à Esprit School of Engineering.

Elle permet de gérer efficacement un système éducatif : utilisateurs, cours, stages, quizz et suivi académique.

**Module clé : feature/cours-et-quiz** - Gestion complète des matières, leçons (texte/PDF), quiz interactifs et questions.

---

## ✨ Features

🔹 **Admin**
- Gestion des utilisateurs
- Gestion des filières  
- Gestion des stages
- Gestion des quizz
- Dashboard global

🔹 **Professeur**
- Gestion des cours et leçons (texte + 📄 PDF drag & drop)
- Création de quizz avec questions multiples
- Suivi des étudiants

🔹 **Étudiant**
- Accès aux cours et leçons
- Participation aux quizz interactifs
- Consultation des notes
- Candidature aux stages

---

## 🛠️ Tech Stack
Java 17+ (Modules)
JavaFX 21
MySQL 8.0
Maven 3.9+
IntelliJ IDEA

text

---

## 🏗️ Architecture

**Architecture MVC renforcée :**
Model → POJOs (Matiere, Lecon, Quiz, Question)
View → FXML + CSS moderne
Controller → JavaFX Controllers
Service → JDBC avancé + détection schéma dynamique
Util → Session, Navigation, Sécurité

text

**Points forts techniques :**
- Détection automatique des colonnes BD (`prof_id`, `created_at`)
- Support polymorphe contenu (`__PDF__:` + chemin)
- Navigation dynamique (`ViewNavigator`)
- Contrôle d'accès granulaire par rôle

---

## 🔐 Security
- ✅ Hashage BCrypt des mots de passe
- ✅ Authentification obligatoire
- ✅ Rôles (ADMIN / PROF / ETUDIANT)
- ✅ Autorisations contextuelles

---

## ⚙️ Installation

```bash
# Cloner le projet (branche feature/cours-et-quiz)
git clone --branch feature/cours-et-quiz --single-branch https://github.com/ZeinebBenassi/SmartPath.git
cd SmartPath/SmartPath-Desktop

# Compiler
mvn clean compile javafx:compile

# Configurer DB (SmartPath-Desktop/src/main/resources/application.properties)
# jdbc:mysql://localhost:3306/smartpath | root | password
```

---

## ▶️ Run

```bash
# Via Maven
mvn javafx:run

# Via IntelliJ
Run → Main.java
```

---

📁 Project Structure
src/
├── controllers/
├── models/
├── services/
├── utils/
└── resources/
├── fxml/
└── images/

## 👥 Contributors

**Développeurs principaux :**

- **Zerzeri Yasmine** - Interface utilisateur, Design FXML
- **wiem Anabtaoui** - Architecture feature/cours-et-quiz, Services avancés, Drag & Drop PDF
- **Benassi Zeineb** - Infrastructure projet, Configuration Maven
-** iheb ferjeni**- gestion stage 

---





---

## 🚀 Prochaines étapes

- [ ] Sauvegarde des réponses aux quiz
- [ ] Correction automatique des quiz
- [ ] Pool de connexions HikariCP
- [ ] Historique des résultats étudiants
- [ ] Export PDF des résultats
