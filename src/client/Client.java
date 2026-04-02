package client;

import java.io.*;
import java.net.*;
import java.util.Scanner;

import common.Message;
import common.Password;

public class Client {
    private Socket             socket;
    private ObjectOutputStream oos;
    private ObjectInputStream  ois;
    private int                userId    = -1;
    private boolean            connected = false;

    private static final int PORT = 10000;

    public Client(String username, String password) {
        try {
            InetAddress host = InetAddress.getLocalHost();
            socket = new Socket(host, PORT);

            oos = new ObjectOutputStream(socket.getOutputStream());
            oos.flush();
            ois = new ObjectInputStream(socket.getInputStream());

            connected = login(username, password);

            if (connected) {
                Thread listener = new Thread(this::listenForMessages);
                listener.setDaemon(true);
                listener.start();
            } else {
                close();
            }

        } catch (IOException e) {
            System.err.println("Connexion impossible : " + e.getMessage());
        }
    }

    private boolean login(String username, String password) throws IOException {
        try {
            // Le mot de passe est haché ICI, côté client,
            // avant d'être envoyé sur le réseau.
            // Le serveur et la BD ne voient que le hash, jamais "monSecret".
            String passwordHash = Password.hash(password);

            oos.writeObject(username);
            oos.writeObject(passwordHash); // ← hash, pas le mot de passe clair
            oos.flush();

            userId = (Integer) ois.readObject();

            if (userId == -1) {
                System.out.println("Authentification échouée.");
                return false;
            }
            System.out.println("Connecté en tant que '" + username + "' (id=" + userId + ")");
            return true;

        } catch (ClassNotFoundException e) {
            System.err.println("Erreur protocole : " + e.getMessage());
            return false;
        }
    }

    public void sendMessage(String text, int groupId) {
        if (!connected) {
            System.err.println("Non connecté.");
            return;
        }
        try {
            oos.writeObject(new Message(groupId, text));
            oos.flush();
        } catch (IOException e) {
            System.err.println("Erreur envoi : " + e.getMessage());
            connected = false;
        }
    }

    private void listenForMessages() {
        try {
            while (connected) {
                Message msg = (Message) ois.readObject();
                System.out.println("\n" + msg);
                System.out.print("> ");
            }
        } catch (IOException | ClassNotFoundException e) {
            if (connected) System.out.println("\nServeur déconnecté.");
        }
    }

    public void close() {
        connected = false;
        try {
            if (socket != null && !socket.isClosed()) socket.close();
        } catch (IOException ignored) {}
    }

    public boolean isConnected() { return connected; }
    public int     getUserId()   { return userId; }

    public static void main(String[] args) {
        Scanner SC = new Scanner(System.in);
        System.out.print("Username : ");
        String username = SC.nextLine().trim();
        System.out.print("Password : ");
        String password = SC.nextLine().trim();

        Client client = new Client(username, password);

        if (!client.isConnected()) {
            SC.close();
            return;
        }

        System.out.println("Format : <groupId> <message> — 'exit' pour quitter");
        String line;
        while (true) {
            System.out.print("> ");
            line = SC.nextLine().trim();
            if (line.equals("exit")) break;

            int space = line.indexOf(' ');
            if (space == -1) { System.out.println("Format : <groupId> <message>"); continue; }

            try {
                int    groupId = Integer.parseInt(line.substring(0, space));
                String text    = line.substring(space + 1);
                client.sendMessage(text, groupId);
            } catch (NumberFormatException e) {
                System.out.println("groupId invalide.");
            }
        }

        client.close();
        SC.close();
        System.out.println("Déconnecté.");
    }
}