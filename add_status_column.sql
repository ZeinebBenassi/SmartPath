-- =====================================================================
-- Script d'intégration : ajouter la colonne 'status' dans la table user
-- À exécuter dans phpMyAdmin ou MySQL Workbench sur la base 'smartpath'
-- =====================================================================

-- 1. Ajouter la colonne status (si elle n'existe pas déjà)
ALTER TABLE `user`
    ADD COLUMN IF NOT EXISTS `status` VARCHAR(20) DEFAULT 'actif';

-- 2. Mettre tous les utilisateurs existants en "actif"
UPDATE `user` SET `status` = 'actif' WHERE `status` IS NULL;

-- Vérification : afficher la structure de la table
DESCRIBE `user`;
