# 🎓 SmartPath – Educational Management Platform

---

## 📌 Overview

SmartPath est une application desktop développée avec JavaFX dans le cadre du module PIDEV (3ème année) à Esprit School of Engineering.

Elle permet de gérer efficacement un système éducatif : utilisateurs, cours, stages, quizz et suivi académique.

---

## ✨ Features

🔹 Admin

Gestion des utilisateurs
Gestion des filières
Gestion des stages
Gestion des quizz
Dashboard global

🔹 Professeur

Gestion des cours et leçons
Création de quizz
Suivi des étudiants

🔹 Étudiant

Accès aux cours
Participation aux quizz
Consultation des notes
Candidature aux stages

---

## 🛠️ Tech Stack
Java 17 (JDK)
JavaFX
MySQL
Maven
IntelliJ IDEA

---

## 🏗️ Architecture

Architecture MVC :

Model → données (User, Quiz, Cours…)
View → interfaces (FXML)
Controller → logique UI
Service → logique métier

---

## 🔐 Security
Hashage des mots de passe avec BCrypt
Authentification obligatoire
Gestion des rôles (Admin / Prof / Étudiant)

---

## ⚙️ Installation
git clone <repo-url>
cd SmartPath

Installer un **JDK 17** (pas seulement un JRE) et verifier que `JAVA_HOME` pointe vers le JDK.

mvn clean install

Configurer la base de données dans les fichiers .properties.

Configurer Groq AI dans un fichier `config.properties` à la racine du projet ou via variables d'environnement :

```properties
GROQ_API_KEY=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
GROQ_API_URL=https://api.groq.com/openai/v1/chat/completions
GROQ_MODEL=llama-3.3-70b-versatile
```

Les variantes en minuscules sont aussi acceptées, par exemple `groq.api.key`.

---

## ▶️ Run
mvn javafx:run

ou via IntelliJ → Run Main.java

---

## 📁 Project Structure
src/
 ├── controllers/
 ├── models/
 ├── services/
 ├── utils/
 └── resources/
     ├── fxml/
     └── images/

---

## 👥 Contributors
Zerzeri Yasmine
Benassi Zeineb

---

## 🎓 Academic Context
