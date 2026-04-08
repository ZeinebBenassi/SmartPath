package models;

public class Prof extends User {
    private String specialite;

    public Prof() {
        setType("prof");
    }

    public String getSpecialite() {
        return specialite;
    }

    public void setSpecialite(String specialite) {
        this.specialite = specialite;
    }
}
