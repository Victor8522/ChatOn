package client;

import common.Message;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.Map;

/**
 * Fenêtre de connexion / inscription.
 * Lance Window (la fenêtre principale) après authentification réussie.
 *
 * Démontre :
 *   - Classe anonyme : KeyAdapter sur le champ mot de passe.
 *   - Classe interne locale : CloseHandler dans initClose().
 */
public class LoginWindow extends JFrame {

    private static final Color C_BG      = new Color(30,  31,  34);
    private static final Color C_CARD    = new Color(49,  51,  56);
    private static final Color C_INPUT   = new Color(30,  31,  34);
    private static final Color C_ACCENT  = new Color(88, 101, 242);
    private static final Color C_HOVER   = new Color(71,  82, 196);
    private static final Color C_TEXT    = new Color(220, 221, 222);
    private static final Color C_MUTED   = new Color(140, 132, 142);
    private static final Color C_ERROR   = new Color(240,  71,  71);

    private JTextField    hostField;
    private JTextField    usernameField;
    private JPasswordField passwordField;
    private JLabel        statusLabel;
    private JButton       loginBtn;
    private JButton       registerBtn;

    public LoginWindow() {
        setTitle("Chat — Connexion");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setResizable(false);
        getContentPane().setBackground(C_BG);
        setLayout(new GridBagLayout());

        add(buildCard());

        pack();
        setMinimumSize(new Dimension(460, 540));
        setLocationRelativeTo(null);
        initClose();
    }

    // ── Construction de la carte ──────────────────────────────────────────────

    private JPanel buildCard() {
        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(C_CARD);
        card.setBorder(BorderFactory.createEmptyBorder(32, 40, 32, 40));
        card.setPreferredSize(new Dimension(400, 470));

        // Titre
        JLabel title = makeLabel("Bon retour !", new Font("Segoe UI", Font.BOLD, 24), C_TEXT);
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel sub = makeLabel("Content de vous revoir !", new Font("Segoe UI", Font.PLAIN, 15),
                new Color(178, 185, 194));
        sub.setAlignmentX(Component.CENTER_ALIGNMENT);

        // Champs
        hostField     = buildTextField("localhost");
        usernameField = buildTextField("");
        passwordField = new JPasswordField();
        styleTextField(passwordField);

        statusLabel = new JLabel(" ");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        statusLabel.setForeground(C_ERROR);
        statusLabel.setAlignmentX(Component.LEFT_ALIGNMENT);

        loginBtn    = buildButton("Se connecter", C_ACCENT, C_HOVER);
        registerBtn = buildButton("Créer un compte", new Color(64, 68, 75),
                new Color(80, 85, 93));

        // ── Classe anonyme (barème) ───────────────────────────────────────────
        KeyAdapter enterKey = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) doLogin();
            }
        };
        passwordField.addKeyListener(enterKey);
        usernameField.addKeyListener(enterKey);

        loginBtn.addActionListener(e -> doLogin());
        registerBtn.addActionListener(e -> doRegister());

        // Assemblage
        card.add(title);
        card.add(Box.createVerticalStrut(8));
        card.add(sub);
        card.add(Box.createVerticalStrut(24));
        card.add(sectionLabel("SERVEUR"));
        card.add(Box.createVerticalStrut(6));
        card.add(hostField);
        card.add(Box.createVerticalStrut(16));
        card.add(sectionLabel("NOM D'UTILISATEUR"));
        card.add(Box.createVerticalStrut(6));
        card.add(usernameField);
        card.add(Box.createVerticalStrut(16));
        card.add(sectionLabel("MOT DE PASSE"));
        card.add(Box.createVerticalStrut(6));
        card.add(passwordField);
        card.add(Box.createVerticalStrut(8));
        card.add(statusLabel);
        card.add(Box.createVerticalStrut(16));
        card.add(loginBtn);
        card.add(Box.createVerticalStrut(8));
        card.add(registerBtn);

        return card;
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    private void doLogin() {
        String host     = hostField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        loginBtn.setEnabled(false);
        loginBtn.setText("Connexion...");
        statusLabel.setText(" ");

        new Thread(() -> {
            // Création du client avec le listener UI
            Client[] holder = new Client[1];
            Window[] winHolder = new Window[1];

            // Le listener doit référencer la fenêtre qui n'existe pas encore
            // → on utilise un tableau pour le remplir après création
            Client.MessageListener listener = new Client.MessageListener() {
                @Override public void onMessage(Message msg) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].appendMessage(msg);
                    });
                }
                @Override public void onHistoryReceived(List<Message> history) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].loadHistory(history);
                    });
                }
                @Override public void onOnlineUsersUpdated(java.util.List<String> users) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].updateOnlineUsers(users);
                    });
                }
                @Override public void onGroupsReceived(Map<Integer, String> groups) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].populateChannels(groups);
                    });
                }
                @Override public void onUsersReceived(Map<Integer, String> users) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].onUsersReceived(users);
                    });
                }
                @Override public void onGroupCreated(boolean success) {
                    SwingUtilities.invokeLater(() -> {
                        if (winHolder[0] != null) winHolder[0].onGroupCreated(success);
                    });
                }
                @Override public void onConnectionLost() {
                    SwingUtilities.invokeLater(() -> {
                        JOptionPane.showMessageDialog(null,
                                "Connexion perdue.", "Erreur", JOptionPane.WARNING_MESSAGE);
                    });
                }
            };

            holder[0] = new Client(listener);
            boolean ok = holder[0].connect(host, username, password);

            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    Window win = new Window(holder[0], username);
                    winHolder[0] = win;
                    win.setVisible(true);
                    LoginWindow.this.dispose();
                } else {
                    showError("Identifiants incorrects ou serveur inaccessible.");
                    loginBtn.setEnabled(true);
                    loginBtn.setText("Se connecter");
                }
            });
        }).start();
    }

    private void doRegister() {
        String host     = hostField.getText().trim();
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());

        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        registerBtn.setEnabled(false);
        registerBtn.setText("Inscription...");

        new Thread(() -> {
            boolean ok = Client.register(host, username, password, "");
            SwingUtilities.invokeLater(() -> {
                if (ok) {
                    JOptionPane.showMessageDialog(LoginWindow.this,
                            "Compte créé ! Vous pouvez maintenant vous connecter.",
                            "Inscription réussie", JOptionPane.INFORMATION_MESSAGE);
                } else {
                    showError("Ce nom d'utilisateur est déjà pris.");
                }
                registerBtn.setEnabled(true);
                registerBtn.setText("Créer un compte");
            });
        }).start();
    }

    // ── Fermeture ─────────────────────────────────────────────────────────────

    private void initClose() {
        // ── Classe interne locale (barème) ────────────────────────────────────
        class CloseHandler extends WindowAdapter {
            @Override
            public void windowClosing(WindowEvent e) {
                System.exit(0);
            }
        }
        addWindowListener(new CloseHandler());
    }

    // ── Helpers UI ────────────────────────────────────────────────────────────

    private void showError(String msg) {
        statusLabel.setText(msg);
        statusLabel.setForeground(C_ERROR);
    }

    private JLabel makeLabel(String text, Font font, Color color) {
        JLabel l = new JLabel(text);
        l.setFont(font);
        l.setForeground(color);
        return l;
    }

    private JLabel sectionLabel(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("Segoe UI", Font.BOLD, 11));
        l.setForeground(C_MUTED);
        l.setAlignmentX(Component.LEFT_ALIGNMENT);
        return l;
    }

    private JTextField buildTextField(String text) {
        JTextField f = new JTextField(text);
        styleTextField(f);
        return f;
    }

    private void styleTextField(JTextField f) {
        f.setBackground(C_INPUT);
        f.setForeground(C_TEXT);
        f.setCaretColor(C_TEXT);
        f.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        f.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 80), 1),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        f.setAlignmentX(Component.LEFT_ALIGNMENT);
        f.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
    }

    private JButton buildButton(String text, Color bg, Color hover) {
        JButton btn = new JButton(text) {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(getModel().isRollover() ? hover : bg);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        btn.setForeground(Color.WHITE);
        btn.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setAlignmentX(Component.LEFT_ALIGNMENT);
        btn.setMaximumSize(new Dimension(Integer.MAX_VALUE, 44));
        return btn;
    }

    // ── Point d'entrée ────────────────────────────────────────────────────────

    public static void main(String[] args) {
        try { UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); }
        catch (Exception ignored) {}
        SwingUtilities.invokeLater(() -> new LoginWindow().setVisible(true));
    }
}
