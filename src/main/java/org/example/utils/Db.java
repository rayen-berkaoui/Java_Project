package org.example.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Db {
    final String DB_PORT = System.getenv().getOrDefault("DB_PORT", "3306");
    final String URL = "jdbc:mysql://localhost:" + DB_PORT + "/tabaani_db?useSSL=false&serverTimezone=UTC";
    final String USER = "root";
    final String PASSWORD = "";

    static Db instance;
    Connection connection;

    private Db() {
        try {
            connection = DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {e.printStackTrace();}
    }
    public static Db getInstance() {
        if (instance == null) {instance = new Db();}
        return instance;}

    public Connection getConnection() {
        return connection;
    }
}
