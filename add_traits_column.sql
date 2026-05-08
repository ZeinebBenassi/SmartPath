-- ═══════════════════════════════════════════════════════════════
--  SmartPath — Migration : ajout colonne `traits` à la table filiere
--  À exécuter UNE SEULE FOIS dans phpMyAdmin sur la base "smartpath"
-- ═══════════════════════════════════════════════════════════════

USE smartpath;

-- Ajouter la colonne traits (JSON) si elle n'existe pas encore
ALTER TABLE filiere
    ADD COLUMN IF NOT EXISTS traits JSON NULL
    COMMENT 'Pondération des traits de personnalité : {"analytique":5,"pratique":3,...}';

-- Initialiser les filières existantes avec des valeurs par défaut
UPDATE filiere
SET traits = '{"analytique":3,"pratique":3,"creatif":2,"technique":4,"mathematique":3,"algorithmique":3,"systemes":2,"reseaux":2,"securite":2,"donnees":3}'
WHERE traits IS NULL
  AND nom LIKE '%Logiciel%';

UPDATE filiere
SET traits = '{"analytique":5,"pratique":4,"creatif":2,"technique":4,"mathematique":5,"algorithmique":5,"systemes":2,"reseaux":2,"securite":2,"donnees":5}'
WHERE traits IS NULL
  AND (nom LIKE '%Data%' OR nom LIKE '%IA%' OR nom LIKE '%Intelligence%');

UPDATE filiere
SET traits = '{"analytique":4,"pratique":4,"creatif":1,"technique":5,"mathematique":2,"algorithmique":3,"systemes":5,"reseaux":5,"securite":5,"donnees":2}'
WHERE traits IS NULL
  AND nom LIKE '%Cybersécurité%';

UPDATE filiere
SET traits = '{"analytique":3,"pratique":5,"creatif":2,"technique":4,"mathematique":2,"algorithmique":3,"systemes":5,"reseaux":5,"securite":3,"donnees":2}'
WHERE traits IS NULL
  AND nom LIKE '%Réseaux%';

UPDATE filiere
SET traits = '{"analytique":4,"pratique":4,"creatif":4,"technique":3,"mathematique":2,"algorithmique":3,"systemes":2,"reseaux":3,"securite":2,"donnees":3}'
WHERE traits IS NULL
  AND nom LIKE '%Web%';

-- Valeur par défaut pour toutes les autres filières sans traits
UPDATE filiere
SET traits = '{"analytique":3,"pratique":3,"creatif":3,"technique":3,"mathematique":2,"algorithmique":3,"systemes":2,"reseaux":2,"securite":2,"donnees":2}'
WHERE traits IS NULL;

SELECT
    id,
    nom,
    traits
FROM filiere
ORDER BY nom;

SELECT CONCAT('✅ Migration terminée ! ', COUNT(*), ' filières ont maintenant des traits.') AS resultat
FROM filiere
WHERE traits IS NOT NULL;
