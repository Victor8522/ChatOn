package server;

import java.sql.*;
import java.util.*;

/**
 * Add existing user to all groups.
 */
public class AddUserToGroups {

    private static final String URL = "jdbc:sqlite:chat.db";

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try (Connection conn = DriverManager.getConnection(URL)) {
            // List all users
            String sql = "SELECT id, username FROM users";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                ResultSet rs = p.executeQuery();
                System.out.println("Users:");
                while (rs.next()) {
                    System.out.println(rs.getInt("id") + ": " + rs.getString("username"));
                }
            }

            if (args.length < 1) {
                System.out.println("Usage: java -cp ... server.AddUserToGroups <username>");
                return;
            }
            String username = args[0];

            // Get userId
            sql = "SELECT id FROM users WHERE username = ?";
            try (PreparedStatement p = conn.prepareStatement(sql)) {
                p.setString(1, username);
                ResultSet rs = p.executeQuery();
                if (rs.next()) {
                    int userId = rs.getInt("id");
                    addUserToAllGroups(conn, userId);
                    System.out.println("Added " + username + " to all groups.");
                } else {
                    System.out.println("User not found: " + username);
                }
            }
        }
    }

    private static void addUserToAllGroups(Connection conn, int userId) throws SQLException {
        String sql = "INSERT OR IGNORE INTO group_members(group_id, user_id) SELECT id, ? FROM chat_groups";
        try (PreparedStatement p = conn.prepareStatement(sql)) {
            p.setInt(1, userId);
            int rows = p.executeUpdate();
            System.out.println("Inserted " + rows + " memberships.");
        }
    }
}