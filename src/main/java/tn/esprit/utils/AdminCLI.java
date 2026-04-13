package tn.esprit.utils;

import tn.esprit.entity.User;
import tn.esprit.services.UserService;

import java.util.Scanner;


public class AdminCLI {

    public static void main(String[] args) {

        System.out.println("   SmartPath - Création d'Administrateur    ");

        try (Scanner scanner = new Scanner(System.in)) {
            // Saisir les informations
            System.out.print("Nom: ");
            String nom = scanner.nextLine().trim();
            if (nom.isEmpty()) {
                System.err.println(" Le nom est obligatoire");
                return;
            }

            System.out.print("Prénom: ");
            String prenom = scanner.nextLine().trim();
            if (prenom.isEmpty()) {
                System.err.println(" Le prénom est obligatoire");
                return;
            }

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            if (email.isEmpty() || !email.contains("@")) {
                System.err.println(" Email invalide");
                return;
            }

            System.out.print("CIN: ");
            String cin = scanner.nextLine().trim();

            System.out.print("Téléphone: ");
            String telephone = scanner.nextLine().trim();

            System.out.print("Adresse: ");
            String adresse = scanner.nextLine().trim();

            System.out.print("Mot de passe: ");
            String password = scanner.nextLine().trim();
            if (password.isEmpty() || password.length() < 6) {
                System.err.println(" Le mot de passe doit avoir au minimum 6 caractères");
                return;
            }

            // Créer l'admin
            UserService userService = new UserService();

            // Vérifier si l'email existe déjà
            if (userService.emailExists(email)) {
                System.err.println("Cet email est déjà utilisé");
                return;
            }

            // Créer l'utilisateur
            User admin = new User();
            admin.setNom(nom);
            admin.setPrenom(prenom);
            admin.setEmail(email);
            admin.setPassword(password);
            admin.setCin(cin);
            admin.setTelephone(telephone);
            admin.setAdresse(adresse);
            admin.setType("admin");
            admin.setRoles("[\"ROLE_ADMIN\"]");
            admin.setStatus("actif");

            int userId = userService.create(admin);

            System.out.println("\n Administrateur créé avec succès!");
            System.out.println("┌──────────────────────────────────┐");
            System.out.println("│ ID:    " + String.format("%-25s", userId) + "│");
            System.out.println("│ Nom:   " + String.format("%-25s", nom + " " + prenom) + "│");
            System.out.println("│ Email: " + String.format("%-25s", email) + "│");
            System.out.println("│ Rôle:  " + String.format("%-25s", "ADMIN") + "│");
            System.out.println("└──────────────────────────────────┘");

        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la création: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
