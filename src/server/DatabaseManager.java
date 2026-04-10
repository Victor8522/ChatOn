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

    /** Appelé au démarrage du serveur pour charger le driver SQLite. */
    public static void initialiser() {
        try {
            Class.forName("org.sqlite.JDBC");
            System.out.println("Driver SQLite chargé.");
        } catch (ClassNotFoundException e) {
            System.err.println("Driver SQLite introuvable : " + e.getMessage());
        }
    }

    /**
     * Vérifie le login en comparant directement deux hash en SQL.
     * Retourne le userId si ok, -1 sinon.
     */
    public static int verifierLogin(String username, String passwordHash) {
        String sql = "SELECT id FROM users WHERE username = ? AND password_hash = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, passwordHash);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getInt("id");
        } catch (SQLException e) {
            System.err.println("Erreur login : " + e.getMessage());
        }
        return -1;
    }

    /** Inscrit un nouvel utilisateur. Retourne false si le nom est déjà pris. */
    public static boolean registerUser(String username, String passwordHash, String email) {
        String sql = "INSERT INTO users(username, password_hash, email) VALUES(?,?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setString(1, username);
            p.setString(2, passwordHash);
            p.setString(3, email.isEmpty() ? null : email);
            p.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur registerUser : " + e.getMessage());
            return false;
        }
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

    /** Retourne les groupes d'un utilisateur sous forme id→nom. */
    public static Map<Integer, String> getUserGroups(int userId) {
        Map<Integer, String> groupes = new LinkedHashMap<>();
        String sql = "SELECT g.id, g.name FROM chat_groups g "
                   + "JOIN group_members gm ON g.id = gm.group_id "
                   + "WHERE gm.user_id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            ResultSet rs = p.executeQuery();
            while (rs.next()) groupes.put(rs.getInt("id"), rs.getString("name"));
        } catch (SQLException e) {
            System.err.println("Erreur getUserGroups : " + e.getMessage());
        }
        return groupes;
    }

    /** Retourne le nom d'un groupe. */
    public static String getGroupName(int groupId) {
        String sql = "SELECT name FROM chat_groups WHERE id = ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, groupId);
            ResultSet rs = p.executeQuery();
            if (rs.next()) return rs.getString("name");
        } catch (SQLException e) {
            System.err.println("Erreur getGroupName : " + e.getMessage());
        }
        return "";
    }

    /** Retourne les N derniers messages d'un groupe (ordre chronologique). */
    public static List<Message> getHistoriqueGroupe(int groupId, int limit) {
        List<Message> messages = new ArrayList<>();
        String sql = "SELECT u.username, m.content "
                   + "FROM messages m JOIN users u ON m.sender_id = u.id "
                   + "WHERE m.group_id = ? "
                   + "ORDER BY m.id DESC LIMIT ?";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, groupId);
            p.setInt(2, limit);
            ResultSet rs = p.executeQuery();
            List<Message> tmp = new ArrayList<>();
            String groupName = getGroupName(groupId);
            while (rs.next()) {
                Message msg = new Message(groupId, rs.getString("content"));
                msg.setSenderUsername(rs.getString("username"));
                msg.setGroupName(groupName);
                tmp.add(msg);
            }
            // Inverser pour ordre chronologique
            for (int i = tmp.size() - 1; i >= 0; i--) messages.add(tmp.get(i));
        } catch (SQLException e) {
            System.err.println("Erreur getHistorique : " + e.getMessage());
        }
        return messages;
    }

    public static List<Message> getMessagesGroupe(int groupId) {
        return getHistoriqueGroupe(groupId, 100);
    }

    public static boolean createUser(String username, String passwordHash) {
        return registerUser(username, passwordHash, "");
    }

    public static boolean createGroup(String groupName, List<Integer> membres) {
        String sqlGroup  = "INSERT INTO chat_groups(name) VALUES(?)";
        String sqlMember = "INSERT INTO group_members(group_id, user_id) VALUES(?,?)";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement pGroup = conn.prepareStatement(sqlGroup,
                     Statement.RETURN_GENERATED_KEYS)) {
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

    /** Retourne tous les utilisateurs sous forme id→username. */
    public static Map<Integer, String> getAllUsers() {
        Map<Integer, String> users = new LinkedHashMap<>();
        String sql = "SELECT id, username FROM users ORDER BY username";
        try (Connection conn = DriverManager.getConnection(URL);
             PreparedStatement p = conn.prepareStatement(sql)) {
            ResultSet rs = p.executeQuery();
            while (rs.next()) users.put(rs.getInt("id"), rs.getString("username"));
        } catch (SQLException e) {
            System.err.println("Erreur getAllUsers : " + e.getMessage());
        }
        return users;
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
