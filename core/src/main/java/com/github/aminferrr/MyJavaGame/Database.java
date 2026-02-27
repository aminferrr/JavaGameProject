package com.github.aminferrr.MyJavaGame;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class Database {

    private static final String DB_URL = "jdbc:sqlite:player.db";
    private Connection connection;

    public Database() {
        try {
            // Подключаем драйвер SQLite
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(DB_URL);
            System.out.println("Connected to SQLite database!");
            createPlayerTable();
        } catch (ClassNotFoundException | SQLException e) {
            e.printStackTrace();
        }
    }

    private void createPlayerTable() {
        String sql = """
            CREATE TABLE IF NOT EXISTS player (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                hp INTEGER DEFAULT 100,
                strength INTEGER DEFAULT 0,
                speed INTEGER DEFAULT 20,
                defense INTEGER DEFAULT 0,
                experience INTEGER DEFAULT 1000
            );
            """;
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Table 'player' created or already exists.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // публичный метод для вставки игрока
    public void insertInitialPlayer(String name) {
        String sql = "INSERT INTO player (name) SELECT '" + name + "' " +
            "WHERE NOT EXISTS (SELECT 1 FROM player WHERE name = '" + name + "');";
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(sql);
            System.out.println("Initial player inserted: " + name);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // проверка наличия игрока
    public boolean checkPlayerExists() {
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM player;")) {
            if (rs.next()) return rs.getInt(1) > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    public void close() {
        try {
            if (connection != null) connection.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public void resetPlayerTable() {
        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate("DROP TABLE IF EXISTS player;");
            System.out.println("Player table dropped.");
            createPlayerTable(); // создаем заново
            insertInitialPlayer("Hero"); // добавляем начального игрока
            System.out.println("Player table recreated with initial player.");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
