-- ============================================================
--  SmartPath — Migration photo vers Cloudinary
--  À exécuter UNE SEULE FOIS sur ta base MySQL
-- ============================================================

-- 1. Agrandir la colonne photo pour accueillir les URLs Cloudinary
--    (une URL Cloudinary fait ~120 chars, VARCHAR(500) est largement suffisant)
ALTER TABLE `user`
    MODIFY COLUMN `photo` VARCHAR(500) DEFAULT NULL;

-- 2. (Optionnel) Vider les anciens chemins locaux pour repartir proprement
--    ⚠️  Décommente seulement si tu veux effacer les anciens chemins locaux
-- UPDATE `user` SET photo = NULL WHERE photo IS NOT NULL AND photo NOT LIKE 'http%';

-- 3. Vérification
SELECT id, nom, prenom, photo FROM `user` LIMIT 10;
