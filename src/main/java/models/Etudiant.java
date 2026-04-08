package models;

import java.util.Date;

public class Etudiant extends User {
    private String niveau;
    private String status;
    private Date suspendedUntil;
    private int filiereId;

    public Etudiant() {
        setType("etudiant");
    }

    public String getNiveau() {
        return niveau;
    }

    public void setNiveau(String niveau) {
        this.niveau = niveau;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getSuspendedUntil() {
        return suspendedUntil;
    }

    public void setSuspendedUntil(Date suspendedUntil) {
        this.suspendedUntil = suspendedUntil;
    }

    public int getFiliereId() {
        return filiereId;
    }

    public void setFiliereId(int filiereId) {
        this.filiereId = filiereId;
    }
}
