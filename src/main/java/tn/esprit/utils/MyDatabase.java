package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class MyDatabase {

    // Configuration BDD
    private static final String URL      = "jdbc:mysql://localhost:3306/smartpath";
    private static final String USER     = "root";
    private static final String PASSWORD = "yasmine";

    // Singleton
    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        connectToDatabase();
    }

    private void connectToDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base smartpath réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable : " + e.getMessage());
            connection = null;
        } catch (SQLException e) {
            System.err.println("Erreur de connexion SQL : " + e.getMessage());
            connection = null;
        }
    }

    /** Retourne l'unique instance Singleton. */
    public static MyDatabase getInstance() {
        if (instance == null) {
            try {
                instance = new MyDatabase();
            } catch (Exception e) {
                System.err.println("Erreur création singleton: " + e.getMessage());
                // Retourner une instance avec connection null plutôt que de planter
                if (instance == null) {
                    instance = new MyDatabase();
                }
            }
        }
        return instance;
    }
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                System.out.println("Reconnexion à la base de données...");
                connectToDatabase();
                if (connection != null) {
                    System.out.println("Reconnexion réussie !");
                }
            } else if (!connection.isValid(2)) {
                System.out.println("Connexion invalide - reconnexion en cours...");
                connectToDatabase();
            }
        } catch (SQLException e) {
            System.err.println("Erreur vérification connexion : " + e.getMessage());
            connectToDatabase();
        }
        return connection;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion fermée proprement.");
            }
        } catch (SQLException e) {
            System.err.println(" Erreur lors de la fermeture : " + e.getMessage());
        }
    }
}
