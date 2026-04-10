package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import common.Message;

/**
 * Serveur de chat multi-clients.
 *
 * Corrections apportées :
 *   - Enrichissement du Message avant broadcast (senderUsername, groupName)
 *     pour que les clients puissent afficher le nom de l'auteur.
 *   - Gestion correcte de EOFException (déconnexion propre sans stacktrace).
 *   - Diffusion de la liste des utilisateurs connectés aux clients UI.
 *   - Envoi de l'historique au client lors de la connexion.
 *   - Commande spéciale "REGISTER" envoyée avant auth pour l'inscription.
 */
public class Server {

    private static final int PORT = 10000;
    private final ServerSocket serverSocket;

    // userId → flux de sortie (thread-safe)
    private final Map<Integer, ObjectOutputStream> connectedClients
            = new ConcurrentHashMap<>();

    // userId → username (pour broadcast liste connectés)
    private final Map<Integer, String> connectedUsernames
            = new ConcurrentHashMap<>();

    public Server() throws IOException {
        serverSocket = new ServerSocket(PORT);
        System.out.println("Serveur démarré sur le port " + PORT);
    }

    public void start() {
        System.out.println("En attente de connexions...");
        while (true) {
            try {
                Socket socket = serverSocket.accept();
                Thread t = new Thread(() -> handleClient(socket));
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                System.err.println("Erreur accept : " + e.getMessage());
            }
        }
    }

    // ── Gestion d'un client ───────────────────────────────────────────────────

    private void handleClient(Socket socket) {
        int userId = -1;
        ObjectOutputStream oos = null;
        try {
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // ── 1. Authentification ou inscription ────────────────────────────
            String firstToken = (String) ois.readObject();

            if ("REGISTER".equals(firstToken)) {
                // Inscription : username | passwordHash | email
                String username     = (String) ois.readObject();
                String passwordHash = (String) ois.readObject();
                String email        = (String) ois.readObject();
                boolean ok = DatabaseManager.registerUser(username, passwordHash, email);
                oos.writeObject(ok ? "OK" : "ERROR:NOM_PRIS");
                oos.flush();
                socket.close();
                return;
            }

            // Sinon firstToken = username (protocole normal)
            String username     = firstToken;
            String passwordHash = (String) ois.readObject();

            userId = DatabaseManager.verifierLogin(username, passwordHash);
            oos.writeObject(userId);
            oos.flush();

            if (userId == -1) {
                System.out.println("Login refusé pour '" + username + "'");
                socket.close();
                return;
            }

            System.out.println("'" + username + "' (id=" + userId + ") connecté.");
            connectedClients.put(userId, oos);
            connectedUsernames.put(userId, username);
            DatabaseManager.setStatus(userId, "online");

            // Envoyer la liste des groupes de l'utilisateur
            Map<Integer, String> groupes = DatabaseManager.getUserGroups(userId);
            oos.writeObject(groupes);
            oos.flush();

            // Envoyer liste des connectés
            broadcastConnectedList();

            // ── 2. Boucle de réception ────────────────────────────────────────
            while (true) {
                Object obj = ois.readObject();

                if (obj instanceof String) {
                    String cmd = (String) obj;
                    if (cmd.startsWith("HISTORY:")) {
                        int groupId = Integer.parseInt(cmd.substring(8));
                        List<Message> hist = DatabaseManager.getHistoriqueGroupe(groupId, 50);
                        oos.writeObject(hist);
                        oos.flush();

                    } else if ("GET_USERS".equals(cmd)) {
                        // Envoyer la liste de tous les utilisateurs
                        Map<Integer, String> allUsers = DatabaseManager.getAllUsers();
                        oos.writeObject("USERS:" + mapToString(allUsers));
                        oos.flush();

                    } else if (cmd.startsWith("CREATE_GROUP:")) {
                        // Format: CREATE_GROUP:nomGroupe:id1,id2,id3
                        String payload = cmd.substring(13);
                        int sep = payload.indexOf(':');
                        String groupName = payload.substring(0, sep);
                        String[] idStrs  = payload.substring(sep + 1).split(",");
                        List<Integer> membres = new ArrayList<>();
                        membres.add(userId); // toujours inclure le créateur
                        for (String id : idStrs) {
                            try {
                                int mid = Integer.parseInt(id.trim());
                                if (mid != userId) membres.add(mid);
                            } catch (NumberFormatException ignored) {}
                        }
                        boolean ok = DatabaseManager.createGroup(groupName, membres);
                        if (ok) {
                            // Récupérer les groupes mis à jour pour chaque membre connecté
                            for (int memberId : membres) {
                                ObjectOutputStream memberOos = connectedClients.get(memberId);
                                if (memberOos != null) {
                                    Map<Integer, String> memberGroups = DatabaseManager.getUserGroups(memberId);
                                    synchronized (memberOos) {
                                        memberOos.writeObject("REFRESH_GROUPS");
                                        memberOos.writeObject(memberGroups);
                                        memberOos.flush();
                                    }
                                }
                            }
                        }
                        oos.writeObject(ok ? "GROUP_CREATED" : "GROUP_ERROR");
                        oos.flush();
                    }
                } else if (obj instanceof Message) {
                    Message msg = (Message) obj;
                    System.out.println(username + " → groupe " + msg.getGroupId()
                            + " : " + msg.getText());

                    // Enrichissement du message
                    msg.setSenderUsername(username);
                    msg.setGroupName(DatabaseManager.getGroupName(msg.getGroupId()));

                    // Sauvegarde en BD
                    DatabaseManager.sauvegarderMessage(userId, msg.getGroupId(), msg.getText());

                    // Broadcast aux membres du groupe
                    broadcast(msg, userId);
                }
            }

        } catch (EOFException | SocketException e) {
            // Déconnexion normale
            System.out.println("Déconnexion de userId=" + userId);
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur client " + userId + " : " + e.getMessage());
        } finally {
            if (userId != -1) {
                connectedClients.remove(userId);
                connectedUsernames.remove(userId);
                DatabaseManager.setStatus(userId, "offline");
                broadcastConnectedList();
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    // ── Broadcast message ─────────────────────────────────────────────────────

    /** Diffuse un message à tous les membres connectés du groupe, sauf l'expéditeur. */
    private void broadcast(Message msg, int senderId) {
        List<Integer> members = DatabaseManager.getMembresGroupe(msg.getGroupId());
        for (int memberId : members) {
            if (memberId == senderId) continue;
            ObjectOutputStream oos = connectedClients.get(memberId);
            if (oos != null) {
                try {
                    synchronized (oos) {
                        oos.writeObject(msg);
                        oos.flush();
                    }
                } catch (IOException e) {
                    System.err.println("Erreur broadcast → userId=" + memberId);
                    connectedClients.remove(memberId);
                }
            }
        }
    }

    // ── Broadcast liste connectés ─────────────────────────────────────────────

    /** Envoie à tous les clients la liste des utilisateurs actuellement connectés. */
    private void broadcastConnectedList() {
        List<String> names = new ArrayList<>(connectedUsernames.values());
        String cmd = "ONLINE:" + String.join(",", names);
        for (ObjectOutputStream oos : connectedClients.values()) {
            try {
                synchronized (oos) {
                    oos.writeObject(cmd);
                    oos.flush();
                }
            } catch (IOException ignored) {}
        }
    }

    // ── Utilitaire ────────────────────────────────────────────────────────────

    /** Sérialise un Map<Integer,String> en "id1=nom1|id2=nom2|..." */
    private String mapToString(Map<Integer, String> map) {
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<Integer, String> e : map.entrySet()) {
            if (sb.length() > 0) sb.append("|");
            sb.append(e.getKey()).append("=").append(e.getValue());
        }
        return sb.toString();
    }

    // ── Point d'entrée ────────────────────────────────────────────────────────

    public static void main(String[] args) throws IOException {
        DatabaseManager.initialiser();
        new Server().start();
    }
}
