package server;

import java.sql.*;
import common.Password;

/**
 * Initialisation de la base de données.
 * À lancer une seule fois (ou pour repartir proprement).
 *
 * Corrections :
 *   - "groups" → "chat_groups" (mot réservé SQL)
 *   - Mots de passe simplifiés pour les tests (alice/alice, bob/bob, charlie/charlie)
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
            drop(conn, "chat_groups");   // ← corrigé
            drop(conn, "users");

            // 2. Création des tables
            exec(conn,
                "CREATE TABLE users ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " username TEXT NOT NULL UNIQUE,"
                + " password_hash TEXT NOT NULL,"
                + " email TEXT UNIQUE,"
                + " status TEXT NOT NULL DEFAULT 'offline'"
                + ")"
            );
            exec(conn,
                "CREATE TABLE chat_groups ("   // ← corrigé
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " name TEXT NOT NULL"
                + ")"
            );
            exec(conn,
                "CREATE TABLE group_members ("
                + " group_id INTEGER NOT NULL, user_id INTEGER NOT NULL,"
                + " PRIMARY KEY (group_id, user_id),"
                + " FOREIGN KEY (group_id) REFERENCES chat_groups(id),"
                + " FOREIGN KEY (user_id)  REFERENCES users(id)"
                + ")"
            );
            exec(conn,
                "CREATE TABLE messages ("
                + " id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + " sender_id INTEGER NOT NULL, group_id INTEGER NOT NULL,"
                + " content TEXT NOT NULL,"
                + " sent_at TEXT NOT NULL DEFAULT (datetime('now')),"
                + " FOREIGN KEY (sender_id) REFERENCES users(id),"
                + " FOREIGN KEY (group_id)  REFERENCES chat_groups(id)"
                + ")"
            );
            System.out.println("[OK] Tables créées.");

            // 3. Utilisateurs par défaut
            insertUser(conn, "alice",   Password.hash("alice"),   "alice@example.com");
            insertUser(conn, "bob",     Password.hash("bob"),     "bob@example.com");
            insertUser(conn, "charlie", Password.hash("charlie"), null);
            System.out.println("[OK] Utilisateurs insérés.");

            // 4. Groupes
            insertGroup(conn, "Général");
            insertGroup(conn, "Projet Java");
            System.out.println("[OK] Groupes insérés.");

            // 5. Membres
            insertMember(conn, 1, 1); // alice  → Général
            insertMember(conn, 1, 2); // bob    → Général
            insertMember(conn, 1, 3); // charlie→ Général
            insertMember(conn, 2, 1); // alice  → Projet Java
            insertMember(conn, 2, 2); // bob    → Projet Java
            System.out.println("[OK] Membres insérés.");

            // 6. Vérification
            System.out.println();
            verify(conn, "users");
            verify(conn, "chat_groups");
            verify(conn, "group_members");
            verify(conn, "messages");

            System.out.println("\n=== Base prête ! ===");
            System.out.println("Comptes de test :");
            System.out.println("  alice / alice");
            System.out.println("  bob / bob");
            System.out.println("  charlie / charlie");
        }
    }

    private static void drop(Connection c, String t) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute("DROP TABLE IF EXISTS " + t); }
    }
    private static void exec(Connection c, String sql) throws SQLException {
        try (Statement s = c.createStatement()) { s.execute(sql); }
    }
    private static void insertUser(Connection c, String u, String h, String e) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO users(username,password_hash,email) VALUES(?,?,?)")) {
            p.setString(1, u); p.setString(2, h); p.setString(3, e); p.executeUpdate();
        }
    }
    private static void insertGroup(Connection c, String name) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO chat_groups(name) VALUES(?)")) {   // ← corrigé
            p.setString(1, name); p.executeUpdate();
        }
    }
    private static void insertMember(Connection c, int gid, int uid) throws SQLException {
        try (PreparedStatement p = c.prepareStatement(
                "INSERT INTO group_members(group_id,user_id) VALUES(?,?)")) {
            p.setInt(1, gid); p.setInt(2, uid); p.executeUpdate();
        }
    }
    private static void verify(Connection c, String t) throws SQLException {
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT COUNT(*) FROM " + t)) {
            System.out.println("  " + t + " : " + rs.getInt(1) + " ligne(s)");
        }
    }
}
