package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;

    private final String URL = "jdbc:mysql://localhost:3306/tabaany?autoReconnect=true&useSSL=false";
    private final String USER = "root"; // ton user
    private final String PASSWORD = ""; // ton mot de passe

    private MyDataBase() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("Connexion à la base OK !");
        } catch (SQLException e) {
            System.out.println("Erreur de connexion : " + e.getMessage());
        }
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                System.out.println("⚠️ Connexion DB perdue, reconnexion...");
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Reconnexion DB réussie !");
            }
        } catch (SQLException e) {
            System.out.println("❌ Erreur reconnexion DB: " + e.getMessage());
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Reconnexion DB forcée réussie !");
            } catch (SQLException ex) {
                System.out.println("❌ Reconnexion DB échouée: " + ex.getMessage());
            }
        }
        return connection;
    }
}