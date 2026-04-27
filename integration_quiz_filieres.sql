-- ═══════════════════════════════════════════════════════════════
--  SmartPath — Script SQL d'intégration Quiz + Filières
--  À exécuter dans phpMyAdmin ou MySQL Workbench sur la base "smartpath"
--  Ce script ajoute les tables quiz et filiere au projet SmartPath-main
-- ═══════════════════════════════════════════════════════════════

USE smartpath;

-- ═══════════════════════════════════════════════════════════════
--  TABLE : filiere (gestion des filières admin)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS filiere (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    nom         VARCHAR(200) NOT NULL,
    categorie   VARCHAR(100),
    niveau      VARCHAR(100),
    description TEXT,
    debouches   TEXT,
    competences TEXT,
    icon        VARCHAR(10),
    image       VARCHAR(500)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
--  TABLE : question (questions du quiz de personnalité)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS question (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    text      VARCHAR(500) NOT NULL,
    category  VARCHAR(100) NOT NULL,
    ordre     INT          NOT NULL DEFAULT 1,
    is_active TINYINT(1)   NOT NULL DEFAULT 1
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
--  TABLE : answer (réponses aux questions, ManyToOne question)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS answer (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    text        VARCHAR(500) NOT NULL,
    points      INT          NOT NULL DEFAULT 5,
    trait       VARCHAR(100) NOT NULL,
    question_id INT          NOT NULL,
    CONSTRAINT fk_answer_question
        FOREIGN KEY (question_id) REFERENCES question(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
--  TABLE : quiz_result (résultats quiz par étudiant)
-- ═══════════════════════════════════════════════════════════════
CREATE TABLE IF NOT EXISTS quiz_result (
    id              INT AUTO_INCREMENT PRIMARY KEY,
    etudiant_id     INT  NOT NULL DEFAULT 1,
    responses       TEXT,
    scores          TEXT,
    recommendations TEXT,
    profile_type    VARCHAR(100),
    created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ═══════════════════════════════════════════════════════════════
--  DONNÉES : 12 Questions de personnalité (traits informatiques)
-- ═══════════════════════════════════════════════════════════════
INSERT IGNORE INTO question (id, text, category, ordre, is_active) VALUES
(1,  'Quel type de problème aimez-vous résoudre ?',                        'analytique',    1, 1),
(2,  'Comment préférez-vous apprendre de nouvelles technologies ?',         'pratique',      2, 1),
(3,  'Quel aspect du développement vous attire le plus ?',                  'creatif',       3, 1),
(4,  'Comment abordez-vous un projet complexe ?',                           'algorithmique', 4, 1),
(5,  'Quelle est votre approche face aux données massives ?',               'donnees',       5, 1),
(6,  'Comment réagissez-vous face à une panne système critique ?',          'systemes',      6, 1),
(7,  'Quel environnement de travail vous convient le mieux ?',              'pratique',      7, 1),
(8,  'Quelle compétence souhaitez-vous développer en priorité ?',           'technique',     8, 1),
(9,  'Comment sécurisez-vous vos applications ?',                           'securite',      9, 1),
(10, 'Quelle technologie vous passionne le plus ?',                         'algorithmique', 10, 1),
(11, 'Comment optimisez-vous les performances d''une application ?',        'technique',     11, 1),
(12, 'Quel est votre rapport aux mathématiques et à la modélisation ?',     'mathematique',  12, 1);

-- Réponses Q1
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('Analyser et interpréter des jeux de données',       8, 'analytique',    1),
('Concevoir des algorithmes efficaces',               8, 'algorithmique', 1),
('Créer des interfaces visuelles intuitives',          7, 'creatif',       1),
('Configurer et sécuriser des serveurs',              7, 'systemes',      1);

-- Réponses Q2
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('En pratiquant directement sur des projets réels',   8, 'pratique',      2),
('En lisant des articles et de la documentation',     7, 'analytique',    2),
('En suivant des tutoriels vidéo et des MOOCs',       6, 'creatif',       2),
('En participant à des hackathons',                   8, 'algorithmique', 2);

-- Réponses Q3
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('La conception d''expériences utilisateur (UX)',     9, 'creatif',       3),
('L''optimisation des requêtes et bases de données',  8, 'donnees',       3),
('La mise en place d''architectures cloud',           8, 'systemes',      3),
('Le développement d''API et de microservices',       7, 'pratique',      3);

-- Réponses Q4
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('Je décompose le problème en sous-problèmes',        9, 'algorithmique', 4),
('Je cherche des solutions existantes à adapter',     7, 'pratique',      4),
('Je prototype rapidement pour tester mes idées',     8, 'creatif',       4),
('J''analyse les données disponibles en premier',     8, 'analytique',    4);

-- Réponses Q5
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('J''utilise des outils de Big Data (Spark, Hadoop)', 9, 'donnees',       5),
('Je les visualise avec des graphiques interactifs',  8, 'analytique',    5),
('Je les stocke dans des bases relationnelles',       7, 'technique',     5),
('Je les exploite pour entraîner des modèles ML',     9, 'mathematique',  5);

-- Réponses Q6
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('Je diagnostique méthodiquement les logs',           8, 'systemes',      6),
('Je cherche rapidement la cause dans le code',       8, 'pratique',      6),
('J''analyse les métriques de performance',           7, 'analytique',    6),
('Je vérifie la sécurité et les pare-feu',            9, 'securite',      6);

-- Réponses Q7
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('En équipe, avec des revues de code régulières',     7, 'pratique',      7),
('En solo, avec une forte autonomie',                 8, 'algorithmique', 7),
('Dans un environnement créatif et agile',            8, 'creatif',       7),
('Dans un contexte structuré avec des processus',     7, 'systemes',      7);

-- Réponses Q8
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('La maîtrise des réseaux et protocoles',             9, 'reseaux',       8),
('La sécurité offensive et défensive',                9, 'securite',      8),
('Le machine learning et l''IA',                      9, 'mathematique',  8),
('Le développement mobile et web',                    8, 'pratique',      8);

-- Réponses Q9
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('J''applique les meilleures pratiques OWASP',        9, 'securite',      9),
('J''utilise des tests automatisés et du CI/CD',      8, 'technique',     9),
('Je chiffre les données sensibles',                  8, 'securite',      9),
('Je documente et audite régulièrement le code',      7, 'analytique',    9);

-- Réponses Q10
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('L''intelligence artificielle et le deep learning',  9, 'algorithmique', 10),
('Les technologies blockchain',                       7, 'technique',     10),
('L''IoT et les systèmes embarqués',                  8, 'systemes',      10),
('Le cloud computing et DevOps',                      8, 'reseaux',       10);

-- Réponses Q11
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('En profilant le code et en éliminant les goulots',  9, 'technique',     11),
('En optimisant les requêtes SQL et les index',       8, 'donnees',       11),
('En utilisant un CDN et du caching',                 7, 'reseaux',       11),
('En refactorisant l''architecture applicative',      8, 'algorithmique', 11);

-- Réponses Q12
INSERT IGNORE INTO answer (text, points, trait, question_id) VALUES
('J''adore les statistiques et l''algèbre linéaire',  9, 'mathematique',  12),
('Je les utilise quand c''est nécessaire',            6, 'pratique',      12),
('Je préfère la logique algorithmique pure',          8, 'algorithmique', 12),
('Je les applique à la modélisation de données',      9, 'donnees',       12);

-- ═══════════════════════════════════════════════════════════════
--  DONNÉES : Filières de démonstration
-- ═══════════════════════════════════════════════════════════════
INSERT IGNORE INTO filiere (nom, categorie, niveau, description, debouches, competences, icon) VALUES
('Génie Logiciel',            'informatique', 'Licence',
 'Formation complète en développement logiciel et architecture.',
 'Développeur, Architecte logiciel, Tech Lead',
 'Java, Python, Spring Boot, Git, SQL', '💻'),

('Data Science & IA',         'informatique', 'Master',
 'Analyse de données, machine learning et intelligence artificielle.',
 'Data Scientist, ML Engineer, BI Analyst',
 'Python, TensorFlow, Pandas, SQL, Spark', '📊'),

('Cybersécurité',             'informatique', 'Master',
 'Sécurité des systèmes, réseaux et applications.',
 'Expert Cybersécurité, Pentester, RSSI',
 'Ethical Hacking, OWASP, Firewall, Kali Linux', '🔒'),

('Réseaux & Télécommunications', 'informatique', 'Licence',
 'Infrastructure réseau, cloud et télécommunications.',
 'Administrateur Réseau, Ingénieur Cloud, DevOps',
 'Cisco, TCP/IP, AWS, Azure, Docker', '🌐'),

('Intelligence Artificielle', 'informatique', 'Master',
 'Modèles IA, deep learning et vision par ordinateur.',
 'Ingénieur IA, Chercheur, Data Engineer',
 'Python, PyTorch, TensorFlow, NLP, Computer Vision', '🤖'),

('Développement Web Full Stack', 'informatique', 'Licence',
 'Création d''applications web modernes, front et back-end.',
 'Développeur Full Stack, Freelance, CTO',
 'React, Node.js, Spring Boot, MySQL, Docker', '🌍');

SELECT CONCAT(
    '✅ Intégration terminée ! ',
    (SELECT COUNT(*) FROM question), ' questions | ',
    (SELECT COUNT(*) FROM answer), ' réponses | ',
    (SELECT COUNT(*) FROM filiere), ' filières'
) AS resultat;
