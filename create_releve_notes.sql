-- =====================================================
--  TABLE : releve_notes
--  À exécuter dans votre base MySQL "smartpath"
-- =====================================================

CREATE TABLE IF NOT EXISTS releve_notes (
    id                  INT          AUTO_INCREMENT PRIMARY KEY,
    etudiant_id         INT          NOT NULL,
    fichier_path        VARCHAR(500) NULL,
    fichier_type        VARCHAR(10)  NULL,
    texte_extrait       TEXT         NULL,
    notes_detectees     JSON         NULL,
    score_par_filiere   JSON         NULL,
    filiere_recommandee VARCHAR(255) NULL,
    analyse_ia          LONGTEXT     NULL,
    moyenne_generale    DOUBLE       NOT NULL DEFAULT 0,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    FOREIGN KEY (etudiant_id) REFERENCES user(id) ON DELETE CASCADE,
    INDEX idx_etudiant (etudiant_id),
    INDEX idx_created  (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
