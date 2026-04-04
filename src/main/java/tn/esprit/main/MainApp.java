package tn.esprit.main;

import tn.esprit.utils.MyDatabase;

import java.sql.Connection;

public class MainApp {

    public static void main(String[] args) {

        try {
            // Test JDBC
            Connection cnx = MyDatabase.getInstance().getConnection();
            System.out.println("Connexion JDBC réussie !");

            // Test Singleton
            Connection cnx2 = MyDatabase.getInstance().getConnection();
            if (cnx == cnx2) {
                System.out.println("Singleton fonctionne !");
            }

        } catch (Exception e) {
            System.err.println("Erreur : " + e.getMessage());
        } finally {
            MyDatabase.getInstance().closeConnection();
        }
    }
}
