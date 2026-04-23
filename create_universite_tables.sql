-- ============================================================
-- Script SQL : tables universite et universite_filiere
-- À exécuter dans la base smartpath si elles n'existent pas.
-- ============================================================

CREATE TABLE IF NOT EXISTS `universite` (
    `id`                   INT AUTO_INCREMENT PRIMARY KEY,
    `nom`                  VARCHAR(255) NOT NULL,
    `ville`                VARCHAR(255),
    `type`                 VARCHAR(100),
    `description`          TEXT,
    `site_web`             VARCHAR(255),
    `adresse`              VARCHAR(255),
    `telephone`            VARCHAR(50),
    `email`                VARCHAR(255),
    `filieres_proposes`    JSON,
    `diplomes`             JSON,
    `frais_annuels`        DOUBLE DEFAULT 0,
    `acces`                VARCHAR(50),
    `conditions_admission` TEXT,
    `capacite_accueil`     INT DEFAULT 0,
    `taux_reussite`        DOUBLE DEFAULT 0,
    `taux_insertion`       DOUBLE DEFAULT 0,
    `source_url`           TEXT,
    `created_at`           DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at`           DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Table de jointure universite <-> filiere
CREATE TABLE IF NOT EXISTS `universite_filiere` (
    `universite_id` INT NOT NULL,
    `filiere_id`    INT NOT NULL,
    PRIMARY KEY (`universite_id`, `filiere_id`),
    FOREIGN KEY (`universite_id`) REFERENCES `universite`(`id`) ON DELETE CASCADE,
    FOREIGN KEY (`filiere_id`)    REFERENCES `filiere`(`id`)    ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
