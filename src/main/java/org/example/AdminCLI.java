package org.example;

import tn.esprit.entity.Admin;
import services.UserService;
import java.util.Scanner;
import java.util.regex.Pattern;

/**
 * CLI Tool pour ajouter un administrateur à la base de données
 * Usage: java AdminCLI --add-admin
 */
public class AdminCLI {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[A-Za-z0-9+_.-]+@(.+)$");
    private static final Pattern PHONE_PATTERN = Pattern.compile(
        "^(?:\\+216|0)[0-9]{8}$");
    
    private UserService userService;

    public AdminCLI() {
        this.userService = new UserService();
    }

    /**
     * Lance l'interface interactive pour ajouter un admin
     */
    public void addAdminInteractive() {
        System.out.println("\n========================================");
        System.out.println("   Ajouter un Administrateur");
        System.out.println("========================================\n");

        Scanner scanner = new Scanner(System.in);

        // Nom
        String nom = "";
        while (nom.isEmpty()) {
            System.out.print("Nom: ");
            nom = scanner.nextLine().trim();
            if (nom.isEmpty()) {
                System.out.println("Le nom ne doit pas etre vide.");
            } else if (!nom.matches("[a-zA-ZA-Za-zÀ-ÿ\\s'-]+")) {
                System.out.println("Le nom ne doit contenir que des lettres.");
                nom = "";
            }
        }

        // Prenom
        String prenom = "";
        while (prenom.isEmpty()) {
            System.out.print("Prenom: ");
            prenom = scanner.nextLine().trim();
            if (prenom.isEmpty()) {
                System.out.println("Le prenom ne doit pas etre vide.");
            } else if (!prenom.matches("[a-zA-ZA-Za-zÀ-ÿ\\s'-]+")) {
                System.out.println("Le prenom ne doit contenir que des lettres.");
                prenom = "";
            }
        }

        // Email
        String email = "";
        while (email.isEmpty()) {
            System.out.print("Email: ");
            email = scanner.nextLine().trim();
            if (email.isEmpty()) {
                System.out.println("L'email ne doit pas etre vide.");
            } else if (!EMAIL_PATTERN.matcher(email).matches()) {
                System.out.println("Format email invalide.");
                email = "";
            } else if (userService.emailExists(email)) {
                System.out.println("Cet email est déjà utilisé.");
                email = "";
            }
        }

        // CIN
        System.out.print("CIN (optionnel): ");
        String cin = scanner.nextLine().trim();

        // Telephone
        String telephone = "";
        System.out.print("Telephone (optionnel): ");
        String inputTel = scanner.nextLine().trim();
        if (!inputTel.isEmpty()) {
            while (!PHONE_PATTERN.matcher(inputTel).matches()) {
                System.out.println("Format: +216XXXXXXXX ou 0XXXXXXXX");
                System.out.print("Telephone (optionnel): ");
                inputTel = scanner.nextLine().trim();
                if (inputTel.isEmpty()) break;
            }
            telephone = inputTel;
        }

        // Adresse
        System.out.print("Adresse (optionnel): ");
        String adresse = scanner.nextLine().trim();

        // Mot de passe
        String pwd = "";
        while (pwd.isEmpty()) {
            System.out.print("Mot de passe (min 6 caracteres): ");
            pwd = scanner.nextLine().trim();
            if (pwd.isEmpty()) {
                System.out.println("Le mot de passe ne doit pas etre vide.");
            } else if (pwd.length() < 6) {
                System.out.println("Le mot de passe doit faire minimum 6 caracteres.");
                pwd = "";
            }
        }

        // Confirmation mot de passe
        String confirm = "";
        while (!confirm.equals(pwd)) {
            System.out.print("Confirmer le mot de passe: ");
            confirm = scanner.nextLine().trim();
            if (!confirm.equals(pwd)) {
                System.out.println("Les mots de passe ne correspondent pas.");
            }
        }

        // Créer l'admin
        Admin admin = new Admin();
        admin.setNom(nom);
        admin.setPrenom(prenom);
        admin.setEmail(email);
        admin.setPassword(pwd);
        admin.setCin(cin);
        admin.setTelephone(telephone);
        admin.setAdresse(adresse);
        admin.setStatus("actif");

        System.out.println("\nCréation de l'administrateur...");
        boolean success = userService.registerAdmin(admin);

        if (success) {
            System.out.println("\n✓ Administrateur créé avec succès!");
            System.out.println("  Nom: " + nom);
            System.out.println("  Prenom: " + prenom);
            System.out.println("  Email: " + email);
            System.out.println("\n========================================\n");
        } else {
            System.out.println("\n✗ Erreur lors de la création de l'administrateur.");
            System.out.println("Consultez les logs pour plus de détails.\n");
        }

        scanner.close();
    }

    /**
     * Affiche l'aide
     */
    public static void printHelp() {
        System.out.println("\n========================================");
        System.out.println("   SmartPath - Admin Management");
        System.out.println("========================================\n");
        System.out.println("Usage:");
        System.out.println("  java AdminCLI --add-admin    Ajouter un administrateur interactif");
        System.out.println("  java AdminCLI --help         Afficher cette aide\n");
        System.out.println("========================================\n");
    }

    public static void main(String[] args) {
        if (args.length == 0 || "--help".equals(args[0])) {
            printHelp();
            return;
        }

        if ("--add-admin".equals(args[0])) {
            AdminCLI cli = new AdminCLI();
            cli.addAdminInteractive();
        } else {
            System.out.println("Commande inconnue: " + args[0]);
            printHelp();
        }
    }
}
