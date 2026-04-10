package client;

import java.io.*;
import java.net.*;
import java.util.*;
import common.Message;
import common.Password;

/**
 * Couche réseau du client.
 *
 * Corrections apportées :
 *   - La connexion se fait sur "localhost" par défaut mais accepte un host en paramètre.
 *   - Réception du protocole enrichi (groupes, liste connectés, historique).
 *   - Callback MessageListener pour notifier l'UI sans bloquer l'EDT.
 *   - Méthode register() pour l'inscription.
 *   - Méthode requestHistory() pour demander l'historique d'un groupe.
 */
public class Client {

    private Socket             socket;
    private ObjectOutputStream oos;
    private ObjectInputStream  ois;
    private int                userId    = -1;
    private String             username;
    private boolean            connected = false;

    private static final int PORT = 10000;

    // ── Interface de callback vers l'UI ──────────────────────────────────────

    public interface MessageListener {
        void onMessage(Message msg);
        void onHistoryReceived(List<Message> history);
        void onOnlineUsersUpdated(List<String> users);
        void onGroupsReceived(Map<Integer, String> groups);
        void onUsersReceived(Map<Integer, String> users);
        void onGroupCreated(boolean success);
        void onConnectionLost();
    }

    private final MessageListener listener;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public Client(MessageListener listener) {
        this.listener = listener;
    }

    // ── Connexion ─────────────────────────────────────────────────────────────

    /**
     * Se connecte au serveur et s'authentifie.
     * @return true si la connexion et l'auth ont réussi.
     */
    public boolean connect(String host, String username, String password) {
        try {
            socket = new Socket(host, PORT);
            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            this.username = username;
            String passwordHash = Password.hash(password);

            // Envoi credentials
            oos.writeObject(username);
            oos.writeObject(passwordHash);
            oos.flush();

            // Réponse : userId ou -1
            userId = (Integer) ois.readObject();
            if (userId == -1) return false;

            connected = true;

            // Le serveur envoie les groupes en premier
            @SuppressWarnings("unchecked")
            Map<Integer, String> groups = (Map<Integer, String>) ois.readObject();
            if (listener != null) listener.onGroupsReceived(groups);

            // Démarrer le thread d'écoute
            startListenerThread();
            return true;

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur connexion : " + e.getMessage());
            return false;
        }
    }

    // ── Inscription ───────────────────────────────────────────────────────────

    /**
     * Envoie une demande d'inscription au serveur.
     * Ouvre une connexion temporaire distincte.
     * @return true si succès, false si nom déjà pris.
     */
    public static boolean register(String host, String username,
                                    String password, String email) {
        try {
            Socket s = new Socket(host, PORT);
            ObjectOutputStream o = new ObjectOutputStream(s.getOutputStream());
            o.flush();
            ObjectInputStream i = new ObjectInputStream(s.getInputStream());

            o.writeObject("REGISTER");
            o.writeObject(username);
            o.writeObject(Password.hash(password));
            o.writeObject(email);
            o.flush();

            String response = (String) i.readObject();
            s.close();
            return "OK".equals(response);

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur inscription : " + e.getMessage());
            return false;
        }
    }

    // ── Thread d'écoute ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void startListenerThread() {
        Thread t = new Thread(() -> {
            try {
                while (connected) {
                    Object obj = ois.readObject();

                    if (obj instanceof Message) {
                        Message msg = (Message) obj;
                        if (listener != null) listener.onMessage(msg);

                    } else if (obj instanceof List) {
                        // Historique de messages
                        List<Message> hist = (List<Message>) obj;
                        if (listener != null) listener.onHistoryReceived(hist);

                    } else if (obj instanceof String) {
                        String cmd = (String) obj;
                        if (cmd.startsWith("ONLINE:")) {
                            String[] users = cmd.substring(7).split(",");
                            if (listener != null)
                                listener.onOnlineUsersUpdated(Arrays.asList(users));
                        } else if (cmd.startsWith("USERS:")) {
                            Map<Integer, String> users = parseMap(cmd.substring(6));
                            if (listener != null) listener.onUsersReceived(users);
                        } else if ("REFRESH_GROUPS".equals(cmd)) {
                            // Le prochain objet est la map des groupes
                            @SuppressWarnings("unchecked")
                            Map<Integer, String> groups = (Map<Integer, String>) ois.readObject();
                            if (listener != null) listener.onGroupsReceived(groups);
                        } else if ("GROUP_CREATED".equals(cmd)) {
                            if (listener != null) listener.onGroupCreated(true);
                        } else if ("GROUP_ERROR".equals(cmd)) {
                            if (listener != null) listener.onGroupCreated(false);
                        }
                    }
                }
            } catch (EOFException | SocketException e) {
                if (connected && listener != null) listener.onConnectionLost();
            } catch (IOException | ClassNotFoundException e) {
                if (connected) System.err.println("Erreur écoute : " + e.getMessage());
                if (listener != null) listener.onConnectionLost();
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ── Envoi ─────────────────────────────────────────────────────────────────

    /** Envoie un message dans un groupe. */
    public void sendMessage(String text, int groupId) {
        if (!connected) return;
        try {
            oos.writeObject(new Message(groupId, text));
            oos.flush();
        } catch (IOException e) {
            System.err.println("Erreur envoi : " + e.getMessage());
            connected = false;
        }
    }

    /** Demande la liste de tous les utilisateurs au serveur. */
    public void requestUsers() {
        if (!connected) return;
        try {
            oos.writeObject("GET_USERS");
            oos.flush();
        } catch (IOException e) {
            System.err.println("Erreur requestUsers : " + e.getMessage());
        }
    }

    /** Demande la création d'un groupe avec les membres donnés. */
    public void createGroup(String groupName, List<Integer> memberIds) {
        if (!connected) return;
        try {
            StringBuilder ids = new StringBuilder();
            for (int id : memberIds) {
                if (ids.length() > 0) ids.append(",");
                ids.append(id);
            }
            oos.writeObject("CREATE_GROUP:" + groupName + ":" + ids);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Erreur createGroup : " + e.getMessage());
        }
    }

    /** Demande l'historique d'un groupe au serveur. */
    public void requestHistory(int groupId) {
        if (!connected) return;
        try {
            oos.writeObject("HISTORY:" + groupId);
            oos.flush();
        } catch (IOException e) {
            System.err.println("Erreur historique : " + e.getMessage());
        }
    }

    /** Parse "id1=nom1|id2=nom2" en Map. */
    private Map<Integer, String> parseMap(String s) {
        Map<Integer, String> map = new LinkedHashMap<>();
        if (s == null || s.isEmpty()) return map;
        for (String entry : s.split("\\|")) {
            int eq = entry.indexOf('=');
            if (eq > 0) {
                try { map.put(Integer.parseInt(entry.substring(0, eq)), entry.substring(eq + 1)); }
                catch (NumberFormatException ignored) {}
            }
        }
        return map;
    }

    /** Ferme la connexion proprement. */
    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public boolean isConnected() { return connected; }
    public int     getUserId()   { return userId; }
    public String  getUsername() { return username; }
}
