package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;


public class MyDatabase {

    // Configuration BDD
    private static final String URL      = "jdbc:mysql://localhost:3306/smartpath";
    private static final String USER     = "root";
    private static final String PASSWORD = "";

    // Singleton
    private static MyDatabase instance;
    private Connection connection;

    private MyDatabase() {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base smartpath réussie !");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver MySQL introuvable : " + e.getMessage());
            throw new RuntimeException("Driver MySQL introuvable", e);
        } catch (SQLException e) {
            System.err.println("Erreur de connexion : " + e.getMessage());
            throw new RuntimeException("Impossible de se connecter à smartpath", e);
        }
    }

    /** Retourne l'unique instance Singleton. */
    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }
    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(2)) {
                System.out.println("Connexion perdue — reconnexion en cours...");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Reconnexion réussie !");
            }
        } catch (SQLException e) {
            System.err.println("Échec de la reconnexion : " + e.getMessage());
            throw new RuntimeException("Impossible de rétablir la connexion à smartpath", e);
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
