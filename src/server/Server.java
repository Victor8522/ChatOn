package server;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

import common.Message;

public class Server {
    private static final int PORT = 10000;
    private final ServerSocket serverSocket;

    // userId → flux de sortie vers ce client (un par connexion active)
    private final Map<Integer, ObjectOutputStream> connectedClients
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

    private void handleClient(Socket socket) {
        int userId = -1;
        try {
            ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ObjectInputStream ois = new ObjectInputStream(socket.getInputStream());

            // --- 1. Authentification ---
            String username = (String) ois.readObject();
            String password = (String) ois.readObject();

            userId = DatabaseManager.verifierLogin(username, password);

            // Réponse : userId si ok, -1 si refusé
            oos.writeObject(userId);
            oos.flush();

            if (userId == -1) {
                System.out.println("Login refusé pour '" + username + "'");
                socket.close();
                return;
            }

            System.out.println("User '" + username + "' (id=" + userId + ") connecté.");
            connectedClients.put(userId, oos);
            DatabaseManager.setStatus(userId, "online");

            // --- 2. Boucle de réception des messages ---
            while (true) {
                Message msg = (Message) ois.readObject();
                System.out.println("User " + userId + " → groupe " + msg.getGroupId() + " : " + msg.getText());
                DatabaseManager.sauvegarderMessage(userId, msg.getGroupId(), msg.getText());
                broadcast(msg, userId);
            }

        } catch (EOFException | SocketException e) {
            // Déconnexion normale du client
            System.out.println("User " + userId + " déconnecté.");
        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Erreur client " + userId + " : " + e.getMessage());
        } finally {
            if (userId != -1) {
                connectedClients.remove(userId);
                DatabaseManager.setStatus(userId, "offline");
            }
            try { socket.close(); } catch (IOException ignored) {}
        }
    }

    /** Diffuse le message à tous les membres connectés du groupe, sauf l'expéditeur. */
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
                    System.err.println("Erreur broadcast → user " + memberId);
                    connectedClients.remove(memberId);
                }
            }
        }
    }

    public static void main(String[] args) throws IOException {
        DatabaseManager.initialiser();
        new Server().start();
    }
}