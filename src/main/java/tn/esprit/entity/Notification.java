package tn.esprit.entity;

import java.time.LocalDateTime;

/**
 * Entité représentant une notification envoyée à l'admin
 * lorsqu'un étudiant passe le quiz de personnalité.
 */
public class Notification {

    private int           id;
    private int           etudiantId;
    private String        etudiantNom;
    private String        etudiantPrenom;
    private String        profileType;
    private LocalDateTime createdAt;
    private boolean       isRead;

    public Notification() {}

    public Notification(int etudiantId, String etudiantNom, String etudiantPrenom,
                        String profileType, LocalDateTime createdAt, boolean isRead) {
        this.etudiantId     = etudiantId;
        this.etudiantNom    = etudiantNom;
        this.etudiantPrenom = etudiantPrenom;
        this.profileType    = profileType;
        this.createdAt      = createdAt;
        this.isRead         = isRead;
    }

    // ── Getters / Setters ────────────────────────────────────────────────

    public int           getId()                               { return id; }
    public void          setId(int id)                         { this.id = id; }

    public int           getEtudiantId()                       { return etudiantId; }
    public void          setEtudiantId(int etudiantId)         { this.etudiantId = etudiantId; }

    public String        getEtudiantNom()                      { return etudiantNom; }
    public void          setEtudiantNom(String nom)            { this.etudiantNom = nom; }

    public String        getEtudiantPrenom()                   { return etudiantPrenom; }
    public void          setEtudiantPrenom(String prenom)      { this.etudiantPrenom = prenom; }

    public String        getProfileType()                      { return profileType; }
    public void          setProfileType(String profileType)    { this.profileType = profileType; }

    public LocalDateTime getCreatedAt()                        { return createdAt; }
    public void          setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean       isRead()                              { return isRead; }
    public void          setRead(boolean read)                 { isRead = read; }

    /** Message affiché dans la liste de notifications. */
    public String getMessage() {
        return etudiantPrenom + " " + etudiantNom
                + " a terminé le quiz de personnalité → Profil : " + profileType;
    }
}
