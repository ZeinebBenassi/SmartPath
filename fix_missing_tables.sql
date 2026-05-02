-- ============================================================
-- SmartPath — Fix Missing Tables and Columns
-- ============================================================
--#
#
USE smartpath;

-- 1. Create lecon table
CREATE TABLE IF NOT EXISTS `lecon` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `titre`       VARCHAR(255) NOT NULL,
    `description` TEXT,
    `contenu`     LONGTEXT,
    `fichier`     VARCHAR(500),
    `duree`       INT DEFAULT 0,
    `matiere_id`  INT,
    `prof_id`     INT,
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`matiere_id`) REFERENCES `matiere`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`prof_id`)   REFERENCES `prof`(`id`)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 2. Create test table
CREATE TABLE IF NOT EXISTS `test` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `titre`       VARCHAR(255) NOT NULL,
    `contenu`     TEXT,
    `date_test`   DATETIME,
    `duree`       INT DEFAULT 0,
    `matiere_id`  INT,
    `prof_id`     INT,
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`matiere_id`) REFERENCES `matiere`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`prof_id`)   REFERENCES `prof`(`id`)    ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 3. Update matiere table with missing columns from entity
-- We use a procedure to safely add columns if they don't exist
DELIMITER //
CREATE PROCEDURE AddMatiereColumns()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='coefficient') THEN
        ALTER TABLE `matiere` ADD COLUMN `coefficient` DOUBLE DEFAULT 1.0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='domaine') THEN
        ALTER TABLE `matiere` ADD COLUMN `domaine` VARCHAR(255);
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='note_min_requise') THEN
        ALTER TABLE `matiere` ADD COLUMN `note_min_requise` DOUBLE DEFAULT 0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='note_max') THEN
        ALTER TABLE `matiere` ADD COLUMN `note_max` DOUBLE DEFAULT 20.0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='rating') THEN
        ALTER TABLE `matiere` ADD COLUMN `rating` DOUBLE DEFAULT 0.0;
    END IF;
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='matiere' AND COLUMN_NAME='nb_avis') THEN
        ALTER TABLE `matiere` ADD COLUMN `nb_avis` INT DEFAULT 0;
    END IF;
END //
DELIMITER ;
CALL AddMatiereColumns();
DROP PROCEDURE AddMatiereColumns;

-- 4. Create offre table
CREATE TABLE IF NOT EXISTS `offre` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `titre`       VARCHAR(255) NOT NULL,
    `entreprise`  VARCHAR(255),
    `description` TEXT,
    `type`        VARCHAR(100),
    `lieu`        VARCHAR(255),
    `admin_id`    INT,
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 5. Create candidature table
CREATE TABLE IF NOT EXISTS `candidature` (
    `id`                INT AUTO_INCREMENT PRIMARY KEY,
    `statut`            VARCHAR(50) DEFAULT 'En attente',
    `lettre_motivation` TEXT,
    `etudiant_id`       INT NOT NULL,
    `offre_id`          INT NOT NULL,
    `created_at`        DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`etudiant_id`) REFERENCES `user`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`offre_id`)    REFERENCES `offre`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 6. Create cv table
CREATE TABLE IF NOT EXISTS `cv` (
    `id`          INT AUTO_INCREMENT PRIMARY KEY,
    `fichier`     VARCHAR(500),
    `etudiant_id` INT NOT NULL,
    `created_at`  DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`etudiant_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 7. Create chat_history table
CREATE TABLE IF NOT EXISTS `chat_history` (
    `id`         INT AUTO_INCREMENT PRIMARY KEY,
    `user_id`    INT NOT NULL,
    `role`       VARCHAR(50),
    `question`   TEXT,
    `answer`     TEXT,
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (`user_id`) REFERENCES `user`(`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 8. Update question table to include test_id if it's missing (used by EduQuestionService)
DELIMITER //
CREATE PROCEDURE AddQuestionTestId()
BEGIN
    IF NOT EXISTS (SELECT * FROM information_schema.COLUMNS WHERE TABLE_SCHEMA='smartpath' AND TABLE_NAME='question' AND COLUMN_NAME='test_id') THEN
        ALTER TABLE `question` ADD COLUMN `test_id` INT AFTER `id`;
        ALTER TABLE `question` ADD CONSTRAINT fk_question_test FOREIGN KEY (`test_id`) REFERENCES `test`(`id`) ON DELETE CASCADE;
    END IF;
END //
DELIMITER ;
CALL AddQuestionTestId();
DROP PROCEDURE AddQuestionTestId;
