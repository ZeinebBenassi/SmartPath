SmartPath – Plateforme de Gestion Éducative
🎓 Overview

SmartPath est une application desktop développée en JavaFX dans le cadre du module PIDEV – 3ème année ingénierie (2025–2026) à Esprit School of Engineering.

La plateforme aide à gérer l’environnement académique : utilisateurs, cours, stages, quizz et suivi des étudiants, avec une interface graphique simple et interactive.

🚀 Fonctionnalités
👨‍💼 Administrateur
Gestion des utilisateurs (CRUD)
Gestion des filières
Gestion des offres de stage
Gestion des quizz
Dashboard avec statistiques
👩‍🏫 Professeur
Gestion des cours et leçons
Création de quizz
Suivi des étudiants
Dashboard pédagogique
👨‍🎓 Étudiant
Consultation des cours
Passage des quizz
Consultation des notes
Candidature aux stages
Dashboard personnel
💻 Technologies
Langage : Java 17
Interface : JavaFX
Base de données : MySQL
Build tool : Maven
IDE : IntelliJ IDEA
🏗️ Architecture

L’application suit une architecture MVC (Model – View – Controller) :

Model : Entités (User, Cours, Quiz…)
View : Interfaces JavaFX (FXML)
Controller : Gestion des actions utilisateur
Service : Logique métier
🔐 Sécurité
Mots de passe chiffrés avec BCrypt
Authentification obligatoire
Gestion des rôles (Admin / Prof / Étudiant)
📋 Prérequis
Java JDK 17+
Maven
MySQL
IntelliJ IDEA (recommandé)
⚙️ Installation
git clone <repo-url>
cd SmartPath
mvn clean install

Configurer la base de données dans les fichiers de config.

▶️ Exécution
Avec Maven
mvn javafx:run
Avec IntelliJ
Lancer la classe Main.java
📁 Structure
SmartPath/
├── src/
│   ├── main/java/
│   │   ├── controllers/
│   │   ├── models/
│   │   ├── services/
│   │   └── utils/
│   ├── resources/
│   │   ├── fxml/
│   │   └── images/
├── pom.xml
└── README.md
👥 Entités principales
User (Admin, Prof, Étudiant)
Cours / Leçon
Filière
Quiz / Question
Relevé de notes
Offre / Candidature
👨‍💻 Contributors
Zerzeri Yasmine
Benassi Zeineb
🎓 Contexte académique

Projet réalisé à Esprit School of Engineering (Tunisie)
Module PIDEV – 3A | 2025–2026

🙏 Remerciements

Merci à l’équipe pédagogique d’Esprit pour l’encadrement et le support.
