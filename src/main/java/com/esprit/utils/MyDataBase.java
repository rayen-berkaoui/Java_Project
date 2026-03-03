package com.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class MyDataBase {

    private static MyDataBase instance;
    private Connection connection;
    private boolean connectionFailed = false;

    private final String URL = "jdbc:mysql://localhost:3306/tabaany?autoReconnect=true&useSSL=false&connectTimeout=5000&socketTimeout=10000";
    private final String USER = "root";
    private final String PASSWORD = "";

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
        try {
            if (connection == null || connection.isClosed() || !connection.isValid(3)) {
                if (connectionFailed) {
                    return null;
                }
                System.out.println("Connexion DB en cours...");
                DriverManager.setLoginTimeout(5);
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Connexion a la base OK !");
            }
        } catch (SQLException e) {
            System.err.println("Erreur connexion DB: " + e.getMessage());
            try {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                System.out.println("Reconnexion DB reussie !");
            } catch (SQLException ex) {
                connectionFailed = true;
                System.err.println("Reconnexion DB echouee: " + ex.getMessage());
            }
        }
        return connection;
    }

    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed() && !connectionFailed;
        } catch (SQLException e) {
            return false;
        }
    }
}
