package server;

import java.sql.*;
import java.util.*;

/**
 * Le serveur reçoit déjà un hash de la part du client.
 * verifierLogin() compare simplement deux hash en BD — aucun mot de passe
 * clair ne transite ni n'est stocké ici.
 */
public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:chat.db";

    public static void initialiser() {
        try { Class.forName("org.sqlite.JDBC"); }
        catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite non trouvé !");
            return;
        }
        String[] tables = {
            "CREATE TABLE IF NOT EXISTS users ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password_hash TEXT NOT NULL,"
                + " email TEXT UNIQUE,"
                + " status TEXT NOT NULL DEFAULT 'offline'"
                + ")",
            "CREATE TABLE IF NOT EXISTS groups ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " name TEXT NOT NULL"
                + ")",
            "CREATE TABLE IF NOT EXISTS group_members ("
                + " group_id INTEGER NOT NULL,"
                + " user_id  INTEGER NOT NULL,"
                + " PRIMARY KEY (group_id, user_id),"
                + " FOREIGN KEY (group_id) REFERENCES groups(id),"
                + " FOREIGN KEY (user_id)  REFERENCES users(id)"
                + ")",
            "CREATE TABLE IF NOT EXISTS messages ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " sender_id INTEGER NOT NULL,"
                + " group_id  INTEGER NOT NULL,"
                + " content   TEXT NOT NULL,"
                + " sent_at   TEXT NOT NULL DEFAULT (datetime('now')),"
                + " FOREIGN KEY (sender_id) REFERENCES users(id),"
                + " FOREIGN KEY (group_id)  REFERENCES groups(id)"
                + ")"
        };
        try (Connection conn = DriverManager.getConnection(URL);
             Statement  stmt = conn.createStatement()) {
            for (String sql : tables) stmt.execute(sql);
            System.out.println("Base de données prête.");
        } catch (SQLException e) {
            System.err.println("Erreur init DB : " + e.getMessage());
        }
    }

    /**
     * Vérifie le login en comparant directement deux hash en SQL.
     * Le paramètre "passwordHash" est déjà un hash SHA-256 envoyé par le client.
     * Retourne le userId si ok, -1 sinon.
     */
    public static int verifierLogin(String username, String passwordHash) {
        // Comparaison hash == hash : simple égalité de chaînes
        String sql = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, passwordHash); // hash reçu du client, jamais le clair
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("Erreur login : " + e.getMessage());
        }
        return -1;
    }

    public static void setStatus(int userId, String status) {
        String sql = "UPDATE users SET status = ? WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, status);
            p.setInt(2, userId);
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur setStatus : " + e.getMessage());
        }
    }

    public static void sauvegarderMessage(int senderId, int groupId, String content) {
        String sql = "INSERT INTO messages(sender_id, group_id, content) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, senderId);
            p.setInt(2, groupId);
            p.setString(3, content);
            p.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde : " + e.getMessage());
        }
    }

    public static List<Integer> getMembresGroupe(int groupId) {
        List<Integer> membres = new ArrayList<>();
        String sql = "SELECT user_id FROM group_members WHERE group_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, groupId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) membres.add(rs.getInt("user_id"));
        } catch (SQLException e) {
            System.err.println("Erreur getMembres : " + e.getMessage());
        }
        return membres;
    }
}