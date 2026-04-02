package server;

import java.sql.*;

import common.Password;

/**
 * Programme d'initialisation de la base de données.
 * À lancer une fois au départ, ou pour repartir sur une base propre.
 *
 * Les mots de passe sont hachés avec PasswordUtils.hash() avant insertion —
 * la BD ne contient que des hash, jamais de mots de passe en clair.
 */
public class InitDB {

    private static final String URL = "jdbc:sqlite:chat.db";

    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");

        System.out.println("=== Initialisation de la base de données ===");

        try (Connection conn = DriverManager.getConnection(URL)) {

            // 1. Suppression dans l'ordre des dépendances
            drop(conn, "messages");
            drop(conn, "group_members");
            drop(conn, "groups");
            drop(conn, "users");

            // 2. Création table par table
            exec(conn,
                "CREATE TABLE users ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password_hash TEXT NOT NULL,"
                + " email TEXT UNIQUE,"
                + " status TEXT NOT NULL DEFAULT 'offline'"
                + ")"
            );
            exec(conn, "CREATE TABLE groups (id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL)");
            exec(conn,
                "CREATE TABLE group_members ("
                + " group_id INTEGER NOT NULL, user_id INTEGER NOT NULL,"
                + " PRIMARY KEY (group_id, user_id),"
                + " FOREIGN KEY (group_id) REFERENCES groups(id),"
                + " FOREIGN KEY (user_id) REFERENCES users(id)"
                + ")"
            );
            exec(conn,
                "CREATE TABLE messages ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " sender_id INTEGER NOT NULL, group_id INTEGER NOT NULL,"
                + " content TEXT NOT NULL,"
                + " sent_at TEXT NOT NULL DEFAULT (datetime('now')),"
                + " FOREIGN KEY (sender_id) REFERENCES users(id),"
                + " FOREIGN KEY (group_id) REFERENCES groups(id)"
                + ")"
            );
            System.out.println("[OK] Tables créées.");

            // 3. Données par défaut — mots de passe hachés ici, jamais stockés en clair
            insertUser(conn, "alice",   Password.hash("alice"),   "alice@example.com");
            insertUser(conn, "bob",     Password.hash("bob"),     "bob@example.com");
            insertUser(conn, "charlie", Password.hash("charlie"), null);
            System.out.println("[OK] Utilisateurs insérés (mots de passe hachés).");

            insertGroup(conn, "Général");
            insertGroup(conn, "Projet Java");
            System.out.println("[OK] Groupes insérés.");

            insertMember(conn, 1, 1);
            insertMember(conn, 1, 2);
            insertMember(conn, 1, 3);
            insertMember(conn, 2, 1);
            insertMember(conn, 2, 2);
            System.out.println("[OK] Membres insérés.");

            // 4. Vérification
            System.out.println();
            verify(conn, "users");
            verify(conn, "groups");
            verify(conn, "group_members");
            verify(conn, "messages");

            System.out.println("\n=== Base de données prête ! ===");
            System.out.println("Comptes de test :");
            System.out.println("  alice   / alice123");
            System.out.println("  bob     / bob123");
            System.out.println("  charlie / charlie123");
        }
    }

    private static void drop(Connection conn, String table) throws SQLException {
        try (Statement s = conn.createStatement()) {
            s.execute("DROP TABLE IF EXISTS " + table);
        }
    }

    private static void exec(Connection conn, String sql) throws SQLException {
        try (Statement s = conn.createStatement()) { s.execute(sql); }
    }

    private static void insertUser(Connection conn, String username,
                                   String hash, String email) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement(
                "INSERT INTO users(username, password_hash, email) VALUES(?,?,?)")) {
            p.setString(1, username);
            p.setString(2, hash);
            p.setString(3, email);
            p.executeUpdate();
        }
    }

    private static void insertGroup(Connection conn, String name) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement(
                "INSERT INTO groups(name) VALUES(?)")) {
            p.setString(1, name);
            p.executeUpdate();
        }
    }

    private static void insertMember(Connection conn, int groupId, int userId) throws SQLException {
        try (PreparedStatement p = conn.prepareStatement(
                "INSERT INTO group_members(group_id, user_id) VALUES(?,?)")) {
            p.setInt(1, groupId);
            p.setInt(2, userId);
            p.executeUpdate();
        }
    }

    private static void verify(Connection conn, String table) throws SQLException {
        try (Statement s = conn.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + table)) {
            System.out.println("  " + table + " : " + rs.getInt(1) + " ligne(s)");
        }
    }
}