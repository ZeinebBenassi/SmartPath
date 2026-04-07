package tn.esprit.main;

import tn.esprit.entity.Etudiant;
import tn.esprit.entity.Prof;
import tn.esprit.services.AdminEtudiantService;
import tn.esprit.services.AdminProfService;
import tn.esprit.utils.MyDatabase;

import java.sql.Connection;
import java.sql.Date;
import java.sql.Timestamp;
import java.util.Scanner;

public class MainApp {

    private static final Scanner SCANNER = new Scanner(System.in);

    public static void main(String[] args) {
        AdminEtudiantService etudiantService = new AdminEtudiantService();
        AdminProfService profService = new AdminProfService();

        try {
            Connection cnx = MyDatabase.getInstance().getConnection();
            System.out.println("Connexion JDBC réussie !");

            Connection cnx2 = MyDatabase.getInstance().getConnection();
            if (cnx == cnx2) {
                System.out.println("Singleton fonctionne !");
            }

            menuPrincipal(etudiantService, profService);

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        } finally {
            MyDatabase.getInstance().closeConnection();
        }
    }

    private static void menuPrincipal(AdminEtudiantService etudiantService, AdminProfService profService) {
        int choix;
        do {
            System.out.println("\n===== MENU ESSAI CRUD =====");
            System.out.println("1. CRUD Etudiants");
            System.out.println("2. CRUD Enseignants");
            System.out.println("0. Quitter");
            choix = lireInt("Choix: ");

            switch (choix) {
                case 1 -> menuEtudiant(etudiantService);
                case 2 -> menuProf(profService);
                case 0 -> System.out.println("Fin du test CRUD.");
                default -> System.out.println("Choix invalide.");
            }
        } while (choix != 0);
    }

    private static void menuEtudiant(AdminEtudiantService service) {
        int choix;
        do {
            System.out.println("\n--- CRUD ETUDIANT ---");
            System.out.println("1. Ajouter");
            System.out.println("2. Modifier");
            System.out.println("3. Supprimer");
            System.out.println("4. Afficher");
            System.out.println("5. Rechercher par nom");
            System.out.println("0. Retour");
            choix = lireInt("Choix: ");

            try {
                switch (choix) {
                    case 1 -> {
                        Etudiant e = lireEtudiant(false);
                        service.ajouter(e);
                        System.out.println("Etudiant ajoute.");
                    }
                    case 2 -> {
                        Etudiant e = lireEtudiant(true);
                        service.modifier(e);
                        System.out.println("Etudiant modifie.");
                    }
                    case 3 -> {
                        int id = lireInt("ID etudiant: ");
                        service.supprimer(id);
                        System.out.println("Etudiant supprime.");
                    }
                    case 4 -> service.afficher().forEach(System.out::println);
                    case 5 -> {
                        System.out.print("Nom: ");
                        String nom = SCANNER.nextLine().trim();
                        service.search(nom).forEach(System.out::println);
                    }
                    case 0 -> {
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (Exception ex) {
                System.err.println("Erreur CRUD etudiant: " + ex.getMessage());
            }
        } while (choix != 0);
    }

    private static void menuProf(AdminProfService service) {
        int choix;
        do {
            System.out.println("\n--- CRUD ENSEIGNANT ---");
            System.out.println("1. Ajouter");
            System.out.println("2. Modifier");
            System.out.println("3. Supprimer");
            System.out.println("4. Afficher");
            System.out.println("5. Rechercher par nom");
            System.out.println("0. Retour");
            choix = lireInt("Choix: ");

            try {
                switch (choix) {
                    case 1 -> {
                        Prof p = lireProf(false);
                        service.ajouter(p);
                        System.out.println("Enseignant ajoute.");
                    }
                    case 2 -> {
                        Prof p = lireProf(true);
                        service.modifier(p);
                        System.out.println("Enseignant modifie.");
                    }
                    case 3 -> {
                        int id = lireInt("ID enseignant: ");
                        service.supprimer(id);
                        System.out.println("Enseignant supprime.");
                    }
                    case 4 -> service.afficher().forEach(System.out::println);
                    case 5 -> {
                        System.out.print("Nom: ");
                        String nom = SCANNER.nextLine().trim();
                        service.search(nom).forEach(System.out::println);
                    }
                    case 0 -> {
                    }
                    default -> System.out.println("Choix invalide.");
                }
            } catch (Exception ex) {
                System.err.println("Erreur CRUD enseignant: " + ex.getMessage());
            }
        } while (choix != 0);
    }

    private static Etudiant lireEtudiant(boolean update) {
        Etudiant e = new Etudiant();
        if (update) {
            e.setId(lireInt("ID: "));
        }

        System.out.print("Nom: ");
        e.setNom(SCANNER.nextLine().trim());
        System.out.print("Prenom: ");
        e.setPrenom(SCANNER.nextLine().trim());
        System.out.print("Email: ");
        e.setEmail(SCANNER.nextLine().trim());
        System.out.print("Password: ");
        e.setPassword(SCANNER.nextLine().trim());
        System.out.print("CIN: ");
        e.setCIN(SCANNER.nextLine().trim());
        System.out.print("Telephone: ");
        e.setTelephone(SCANNER.nextLine().trim());
        System.out.print("Adresse: ");
        e.setAdresse(SCANNER.nextLine().trim());
        System.out.print("Date naissance yyyy-mm-dd (vide=skip): ");
        String date = SCANNER.nextLine().trim();
        if (!date.isEmpty()) {
            e.setDateNaissance(Date.valueOf(date));
        }
        System.out.print("Photo: ");
        e.setPhoto(SCANNER.nextLine().trim());
        System.out.print("Niveau: ");
        e.setNiveau(SCANNER.nextLine().trim());
        if (update) {
            System.out.print("Status (actif/ban/suspendu): ");
            e.setStatus(SCANNER.nextLine().trim());
            System.out.print("Suspended until yyyy-mm-dd HH:mm:ss (vide=skip): ");
            String suspended = SCANNER.nextLine().trim();
            if (!suspended.isEmpty()) {
                e.setSuspendedUntil(Timestamp.valueOf(suspended));
            }
            e.setFiliereId(lireInt("Filiere ID: "));
        } else {
            e.setStatus(Etudiant.STATUS_ACTIF);
            e.setSuspendedUntil(null);
            e.setFiliereId(0);
        }
        return e;
    }

    private static Prof lireProf(boolean update) {
        Prof p = new Prof();
        if (update) {
            p.setId(lireInt("ID: "));
        }

        System.out.print("Nom: ");
        p.setNom(SCANNER.nextLine().trim());
        System.out.print("Prenom: ");
        p.setPrenom(SCANNER.nextLine().trim());
        System.out.print("Email: ");
        p.setEmail(SCANNER.nextLine().trim());
        System.out.print("Password: ");
        p.setPassword(SCANNER.nextLine().trim());
        System.out.print("CIN: ");
        p.setCIN(SCANNER.nextLine().trim());
        System.out.print("Telephone: ");
        p.setTelephone(SCANNER.nextLine().trim());
        System.out.print("Adresse: ");
        p.setAdresse(SCANNER.nextLine().trim());
        System.out.print("Date naissance yyyy-mm-dd (vide=skip): ");
        String date = SCANNER.nextLine().trim();
        if (!date.isEmpty()) {
            p.setDateNaissance(Date.valueOf(date));
        }
        System.out.print("Photo: ");
        p.setPhoto(SCANNER.nextLine().trim());
        System.out.print("Specialite: ");
        p.setSpecialite(SCANNER.nextLine().trim());
        return p;
    }

    private static int lireInt(String prompt) {
        while (true) {
            System.out.print(prompt);
            String text = SCANNER.nextLine().trim();
            try {
                return Integer.parseInt(text);
            } catch (NumberFormatException e) {
                System.out.println("Nombre invalide.");
            }
        }
    }
}
