package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;
    private boolean connectionFailed = false;

    private final String URL = "jdbc:mysql://localhost:3306/tabbani?connectTimeout=5000&socketTimeout=10000";
    private final String USER = "root"; // ton user
    private final String PASSWORD = ""; // ton mot de passe

    private MyDataBase() {
        // Lazy initialization: connection is created when first requested
    }

    public static MyDataBase getInstance() {
        if (instance == null) {
            instance = new MyDataBase();
        }
        return instance;
    }

    public Connection getConnection() {
        if (connection == null && !connectionFailed) {
            try {
                // Set a global login timeout (seconds) as safety net
                DriverManager.setLoginTimeout(5);
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("✅ Connexion à la base OK !");
            } catch (SQLException e) {
                connectionFailed = true;
                System.err.println("❌ Erreur de connexion : " + e.getMessage());
                System.err.println("Assurez-vous que MySQL est en cours d'exécution et que la base 'tabbani' existe.");
            }
        }
        return connection;
    }

    public boolean isConnected() {
        return connection != null && !connectionFailed;
    }
}
