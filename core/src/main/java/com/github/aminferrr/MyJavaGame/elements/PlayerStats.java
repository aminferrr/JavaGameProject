package com.github.aminferrr.MyJavaGame.elements;

import com.github.aminferrr.MyJavaGame.Database;

import java.sql.SQLException;
import java.sql.Statement;

public class PlayerStats {

    private int hp;
    private int strength;
    private int speed;
    private int defense;
    private int experience;

    // стоимость прокачки каждого показателя
    private int hpCost = 20;
    private int strengthCost = 20;
    private int speedCost = 20;
    private int defenseCost = 20;

    private Database db;

    public PlayerStats(Database db) {
        this.db = db;
        loadStats();
    }

    private void loadStats() {
        try (Statement stmt = db.getConnection().createStatement();
             var rs = stmt.executeQuery("SELECT hp, strength, speed, defense, experience FROM player LIMIT 1;")) {
            if (rs.next()) {
                hp = rs.getInt("hp");
                strength = rs.getInt("strength");
                speed = rs.getInt("speed");
                defense = rs.getInt("defense");
                experience = rs.getInt("experience");
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ====== методы для прокачки ======
    public boolean upgradeStrength() {
        return upgradeStat("strength", 20, strengthCost);
    }

    public boolean upgradeSpeed() {
        return upgradeStat("speed", 20, speedCost);
    }

    public boolean upgradeDefense() {
        return upgradeStat("defense", 20, defenseCost);
    }

    public boolean upgradeHp() {
        return upgradeStat("hp", 20, hpCost);
    }

    private boolean upgradeStat(String statName, int increaseAmount, int cost) {
        if (experience < cost) return false; // недостаточно опыта

        experience -= cost; // снимаем опыт

        // увеличиваем показатель
        switch (statName) {
            case "hp" -> hp += increaseAmount;
            case "strength" -> strength += increaseAmount;
            case "speed" -> speed += increaseAmount;
            case "defense" -> defense += increaseAmount;
        }

        // удваиваем стоимость следующего повышения
        switch (statName) {
            case "hp" -> hpCost *= 2;
            case "strength" -> strengthCost *= 2;
            case "speed" -> speedCost *= 2;
            case "defense" -> defenseCost *= 2;
        }

        // сохраняем изменения в базе
        try (Statement stmt = db.getConnection().createStatement()) {
            stmt.executeUpdate("UPDATE player SET " +
                "hp=" + hp + ", " +
                "strength=" + strength + ", " +
                "speed=" + speed + ", " +
                "defense=" + defense + ", " +
                "experience=" + experience +
                " WHERE id=1;");
        } catch (SQLException e) {
            e.printStackTrace();
        }

        return true;
    }

    // ====== геттеры ======
    public int getHp() { return hp; }
    public int getStrength() { return strength; }
    public int getSpeed() { return speed; }
    public int getDefense() { return defense; }
    public int getExperience() { return experience; }

    // ====== геттеры стоимости прокачки ======
    public int getHpCost() { return hpCost; }
    public int getStrengthCost() { return strengthCost; }
    public int getSpeedCost() { return speedCost; }
    public int getDefenseCost() { return defenseCost; }
}
