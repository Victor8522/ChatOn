package server;

import java.sql.*;
import java.util.*;

import common.Message;

/**
 * Le serveur reçoit déjà un hash de la part du client.
 * verifierLogin() compare simplement deux hash en BD — aucun mot de passe
 * clair ne transite ni n'est stocké ici.
 */
public class DatabaseManager {
    private static final String URL = "jdbc:sqlite:chat.db";

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

    public static List<Integer> getGroupesUtilisateur(int userId) {
        List<Integer> groupes = new ArrayList<>();
        String sql = "SELECT group_id FROM group_members WHERE user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) groupes.add(rs.getInt("group_id"));
        } catch (SQLException e) {
            System.err.println("Erreur getGroupes : " + e.getMessage());
        }
        return groupes;
    }

    public static List<Message> getMessagesGroupe(int groupId) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT sender_id, content, timestamp FROM messages WHERE group_id = ? ORDER BY timestamp";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, groupId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) {
                messages.add(new Message(rs.getInt("sender_id"), groupId, rs.getString("content"), null));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getMessages : " + e.getMessage());
        }
        return messages;
    }

    public static boolean createUser(String username, String passwordHash) {
        String sql = "INSERT INTO users(username, password_hash) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, passwordHash);
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur createUser : " + e.getMessage());
            return false;
        }
    }

    public static boolean createGroup(String groupName, List<Integer> membres) {
        String sqlGroup = "INSERT INTO groups(name) VALUES(?)";
        String sqlMember = "INSERT INTO group_members(group_id, user_id) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pGroup = conn.prepareStatement(sqlGroup, Statement.RETURN_GENERATED_KEYS)) {
            pGroup.setString(1, groupName);
            pGroup.executeUpdate();
            ResultSet rs = pGroup.getGeneratedKeys();
            if (rs.next()) {
                int groupId = rs.getInt(1);
                try (PreparedStatement pMember = conn.prepareStatement(sqlMember)) {
                    for (int userId : membres) {
                        pMember.setInt(1, groupId);
                        pMember.setInt(2, userId);
                        pMember.executeUpdate();
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur createGroup : " + e.getMessage());
        }
        return false;
    }

    public static List<String> getUser(int userId) {
        List<String> info = new ArrayList<>();
        String sql = "SELECT username, status FROM users WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) {
                info.add(rs.getString("username"));
                info.add(rs.getString("status"));
            }
        } catch (SQLException e) {
            System.err.println("Erreur getUser : " + e.getMessage());
        }
        return info;
    }

}