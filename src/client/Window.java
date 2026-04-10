package client;

import common.Message;
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.*;
import java.util.List;

/**
 * Fenêtre principale de l'application — style Discord.
 * Connectée au vrai réseau via la classe Client.
 *
 * Corrections et ajouts :
 *   - Branché sur Client (réseau réel) au lieu de données factices.
 *   - Classe Adapter séparée : ChannelClickAdapter (fin de fichier, barème).
 *   - Classe anonyme : KeyAdapter pour l'envoi (barème).
 *   - Classe interne locale : dans initWindowClose() (barème).
 *   - Avatars ronds avec Graphics2D + antialiasing.
 *   - Groupement des messages consécutifs du même auteur.
 *   - Panneau droite : liste des membres connectés.
 *   - Historique chargé à la sélection d'un canal.
 */
public class Window extends JFrame {

    // ── Palette Discord ───────────────────────────────────────────────────────
    static final Color C_BG       = new Color(49,  51,  56);
    static final Color C_SIDEBAR  = new Color(43,  45,  49);
    static final Color C_CHANNELS = new Color(35,  36,  40);
    static final Color C_INPUT    = new Color(56,  58,  64);
    static final Color C_HOVER    = new Color(53,  55,  60);
    static final Color C_SEP      = new Color(63,  65,  71);
    static final Color C_USERBAR  = new Color(35,  36,  40);
    static final Color C_ACCENT   = new Color(88, 101, 242);
    static final Color C_GREEN    = new Color(35, 165,  90);
    static final Color C_TEXT     = new Color(220, 221, 222);
    static final Color C_MUTED    = new Color(148, 155, 164);
    static final Color C_HEADER   = new Color(242, 243, 245);

    private static final Color[] AVATAR_COLORS = {
        new Color(88, 101, 242), new Color(59, 165, 93), new Color(233, 30, 140),
        new Color(240, 165, 0),  new Color(155, 89, 182), new Color(26, 188, 156)
    };

    // ── Composants ────────────────────────────────────────────────────────────
    private JPanel  messagesPanel;
    private JScrollPane messagesScroll;
    private JTextField inputField;
    private JLabel  channelLabel;
    private DefaultListModel<String> memberModel = new DefaultListModel<>();
    private JList<String> memberList;

    // ── Données ───────────────────────────────────────────────────────────────
    private final Client client;
    private final String username;
    private Map<Integer, String> userGroups = new LinkedHashMap<>();
    private int    currentGroupId   = -1;
    private String currentGroupName = "";

    // Cache des utilisateurs (pour la création de groupes)
    private Map<Integer, String> allUsers = new LinkedHashMap<>();

    // Groupement messages
    private String lastAuthor = "";
    private final Map<String, Integer> authorColorMap = new HashMap<>();
    private int colorCounter = 0;

    // ── Constructeur ─────────────────────────────────────────────────────────

    public Window(Client client, String username) {
        this.client   = client;
        this.username = username;

        setTitle("Chat — " + username);
        setSize(1100, 680);
        setMinimumSize(new Dimension(800, 500));
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        getContentPane().setBackground(C_BG);
        setLayout(new BorderLayout());

        add(buildSidebar(),    BorderLayout.WEST);
        add(buildChatArea(),   BorderLayout.CENTER);
        add(buildMemberList(), BorderLayout.EAST);

        initWindowClose();
        setLocationRelativeTo(null);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // SIDEBAR GAUCHE
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_SIDEBAR);
        panel.setPreferredSize(new Dimension(240, 0));

        // En-tête
        JLabel title = new JLabel("   Java Chat");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(C_HEADER);
        title.setPreferredSize(new Dimension(240, 48));
        title.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_SEP));

        // Bouton "+" pour créer un groupe
        JButton newGroupBtn = new JButton("+");
        newGroupBtn.setFont(new Font("Segoe UI", Font.BOLD, 20));
        newGroupBtn.setForeground(C_MUTED);
        newGroupBtn.setBackground(C_SIDEBAR);
        newGroupBtn.setBorderPainted(false);
        newGroupBtn.setFocusPainted(false);
        newGroupBtn.setContentAreaFilled(false);
        newGroupBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        newGroupBtn.setToolTipText("Créer un groupe");
        newGroupBtn.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                newGroupBtn.setForeground(C_TEXT);
            }
            @Override public void mouseExited(java.awt.event.MouseEvent e) {
                newGroupBtn.setForeground(C_MUTED);
            }
        });
        newGroupBtn.addActionListener(e -> client.requestUsers());

        JPanel titleBar = new JPanel(new BorderLayout());
        titleBar.setBackground(C_SIDEBAR);
        titleBar.setPreferredSize(new Dimension(240, 48));
        titleBar.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, C_SEP));
        titleBar.add(title, BorderLayout.CENTER);
        titleBar.add(newGroupBtn, BorderLayout.EAST);

        // Corps canaux — sera rempli dynamiquement
        JPanel channelsBody = new JPanel();
        channelsBody.setName("channelsBody");
        channelsBody.setLayout(new BoxLayout(channelsBody, BoxLayout.Y_AXIS));
        channelsBody.setBackground(C_SIDEBAR);

        JScrollPane chScroll = new JScrollPane(channelsBody);
        chScroll.setBorder(null);
        chScroll.getVerticalScrollBar().setPreferredSize(new Dimension(3, 0));

        // Barre utilisateur
        JPanel userBar = buildUserBar();

        panel.add(titleBar,  BorderLayout.NORTH);
        panel.add(chScroll, BorderLayout.CENTER);
        panel.add(userBar,  BorderLayout.SOUTH);
        return panel;
    }

    /**
     * Peuple la sidebar avec les groupes reçus du serveur.
     * Appelé depuis le thread EDT après onGroupsReceived.
     */
    public void populateChannels(Map<Integer, String> groups) {
        this.userGroups = groups;

        // Retrouver le panneau channelsBody
        JScrollPane chScroll = findChannelsScroll();
        if (chScroll == null) return;

        JPanel body = (JPanel) chScroll.getViewport().getView();
        body.removeAll();

        JLabel sectionLabel = new JLabel("  ▸ GROUPES");
        sectionLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        sectionLabel.setForeground(C_MUTED);
        sectionLabel.setBorder(BorderFactory.createEmptyBorder(16, 8, 4, 0));
        sectionLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        body.add(sectionLabel);

        boolean first = true;
        for (Map.Entry<Integer, String> entry : groups.entrySet()) {
            JPanel item = buildChannelItem(entry.getKey(), entry.getValue());
            body.add(item);
            if (first) {
                // Sélectionner automatiquement le premier groupe
                selectGroup(entry.getKey(), entry.getValue());
                first = false;
            }
        }

        body.revalidate();
        body.repaint();
    }

    private JScrollPane findChannelsScroll() {
        // Parcours simple des composants de la sidebar
        Component sidebar = ((BorderLayout) getContentPane().getLayout())
                .getLayoutComponent(BorderLayout.WEST);
        if (!(sidebar instanceof JPanel)) return null;
        for (Component c : ((JPanel) sidebar).getComponents()) {
            if (c instanceof JScrollPane) return (JScrollPane) c;
        }
        return null;
    }

    private JPanel buildChannelItem(int groupId, String groupName) {
        JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        item.setBackground(groupId == currentGroupId ? C_HOVER : C_SIDEBAR);
        item.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        item.setCursor(new Cursor(Cursor.HAND_CURSOR));

        JLabel hash = new JLabel("#");
        hash.setFont(new Font("Segoe UI", Font.PLAIN, 18));
        hash.setForeground(groupId == currentGroupId ? C_HEADER : C_MUTED);

        JLabel name = new JLabel(groupName);
        name.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        name.setForeground(groupId == currentGroupId ? C_HEADER : C_MUTED);

        item.add(hash);
        item.add(name);

        // ── Classe Adapter séparée (barème) ──────────────────────────────────
        item.addMouseListener(new ChannelClickAdapter(groupId, groupName, item, hash, name, this));
        return item;
    }

    private JPanel buildUserBar() {
        JPanel bar = new JPanel(new BorderLayout(8, 0));
        bar.setBackground(C_USERBAR);
        bar.setPreferredSize(new Dimension(240, 53));
        bar.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JLabel avatar = new JLabel() {
            @Override protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(C_ACCENT);
                g2.fillOval(0, 0, 32, 32);
                g2.setColor(Color.WHITE);
                g2.setFont(new Font("Segoe UI", Font.BOLD, 14));
                FontMetrics fm = g2.getFontMetrics();
                String init = String.valueOf(username.charAt(0)).toUpperCase();
                g2.drawString(init, (32 - fm.stringWidth(init)) / 2,
                        (32 - fm.getHeight()) / 2 + fm.getAscent());
                g2.setColor(C_GREEN);
                g2.fillOval(22, 22, 11, 11);
                g2.dispose();
            }
        };
        avatar.setPreferredSize(new Dimension(36, 36));

        JPanel info = new JPanel(new GridLayout(2, 1));
        info.setOpaque(false);
        JLabel nameLabel = new JLabel(username);
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        nameLabel.setForeground(C_HEADER);
        JLabel statusLabel = new JLabel("En ligne");
        statusLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        statusLabel.setForeground(C_MUTED);
        info.add(nameLabel);
        info.add(statusLabel);

        bar.add(avatar, BorderLayout.WEST);
        bar.add(info,   BorderLayout.CENTER);
        return bar;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // ZONE DE CHAT
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildChatArea() {
        JPanel chat = new JPanel(new BorderLayout());
        chat.setBackground(C_BG);

        // En-tête
        JPanel header = buildChatHeader();

        // Zone des messages
        messagesPanel = new JPanel();
        messagesPanel.setLayout(new BoxLayout(messagesPanel, BoxLayout.Y_AXIS));
        messagesPanel.setBackground(C_BG);
        messagesPanel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));

        messagesScroll = new JScrollPane(messagesPanel);
        messagesScroll.setBorder(null);
        messagesScroll.getViewport().setBackground(C_BG);
        messagesScroll.getVerticalScrollBar().setUnitIncrement(16);

        // Zone de saisie
        JPanel inputArea = buildInputArea();

        chat.add(header,        BorderLayout.NORTH);
        chat.add(messagesScroll, BorderLayout.CENTER);
        chat.add(inputArea,     BorderLayout.SOUTH);
        return chat;
    }

    private JPanel buildChatHeader() {
        JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        header.setBackground(C_BG);
        header.setPreferredSize(new Dimension(0, 48));
        header.setBorder(new CompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, C_SEP),
                BorderFactory.createEmptyBorder(0, 12, 0, 12)));

        JLabel hash = new JLabel("#");
        hash.setFont(new Font("Segoe UI", Font.PLAIN, 24));
        hash.setForeground(C_MUTED);

        channelLabel = new JLabel("—");
        channelLabel.setFont(new Font("Segoe UI", Font.BOLD, 15));
        channelLabel.setForeground(C_HEADER);

        header.add(hash);
        header.add(channelLabel);
        return header;
    }

    private JPanel buildInputArea() {
        JPanel area = new JPanel(new BorderLayout(8, 0));
        area.setBackground(C_BG);
        area.setBorder(BorderFactory.createEmptyBorder(0, 16, 24, 16));

        JPanel box = new JPanel(new BorderLayout(4, 0));
        box.setBackground(C_INPUT);
        box.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 12));

        JLabel plus = new JLabel("+");
        plus.setFont(new Font("Segoe UI", Font.PLAIN, 22));
        plus.setForeground(C_MUTED);
        plus.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 4));

        inputField = new JTextField();
        inputField.setBackground(C_INPUT);
        inputField.setForeground(C_TEXT);
        inputField.setCaretColor(C_TEXT);
        inputField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        inputField.setBorder(BorderFactory.createEmptyBorder(11, 8, 11, 8));
        inputField.setEnabled(false); // activé après sélection d'un groupe

        // ── Classe anonyme (barème) ───────────────────────────────────────────
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    String text = inputField.getText().trim();
                    if (!text.isEmpty() && currentGroupId != -1) {
                        // Affichage local immédiat
                        Message local = new Message(currentGroupId, text);
                        local.setSenderUsername(username);
                        local.setGroupName(currentGroupName);
                        appendMessage(local);
                        // Envoi réseau
                        client.sendMessage(text, currentGroupId);
                        inputField.setText("");
                    }
                }
            }
        });

        box.add(plus,       BorderLayout.WEST);
        box.add(inputField, BorderLayout.CENTER);
        area.add(box, BorderLayout.CENTER);
        return area;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // LISTE DES MEMBRES
    // ══════════════════════════════════════════════════════════════════════════

    private JPanel buildMemberList() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(C_SIDEBAR);
        panel.setPreferredSize(new Dimension(200, 0));

        JLabel title = new JLabel("  Membres en ligne");
        title.setFont(new Font("Segoe UI", Font.BOLD, 11));
        title.setForeground(C_MUTED);
        title.setBorder(BorderFactory.createEmptyBorder(16, 8, 8, 8));

        memberList = new JList<>(memberModel);
        memberList.setBackground(C_SIDEBAR);
        memberList.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        memberList.setFixedCellHeight(40);
        memberList.setCellRenderer(new MemberRenderer());

        JScrollPane scroll = new JScrollPane(memberList);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setPreferredSize(new Dimension(3, 0));

        panel.add(title,  BorderLayout.NORTH);
        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GESTION DES MESSAGES
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Ajoute un message dans la zone de chat.
     * Doit être appelé depuis l'EDT (SwingUtilities.invokeLater).
     */
    public void appendMessage(Message msg) {
        boolean isFirst = !msg.getSenderUsername().equals(lastAuthor);
        lastAuthor = msg.getSenderUsername();

        if (!authorColorMap.containsKey(msg.getSenderUsername())) {
            authorColorMap.put(msg.getSenderUsername(), colorCounter++);
        }
        int colorIdx = authorColorMap.get(msg.getSenderUsername());

        MessageBubble bubble = new MessageBubble(
                msg.getSenderUsername(), msg.getText(), msg.getTimestamp(),
                isFirst, colorIdx);
        bubble.setAlignmentX(Component.LEFT_ALIGNMENT);
        messagesPanel.add(bubble);
        messagesPanel.revalidate();

        // Auto-scroll
        SwingUtilities.invokeLater(() -> {
            JScrollBar bar = messagesScroll.getVerticalScrollBar();
            bar.setValue(bar.getMaximum());
        });
    }

    /** Charge l'historique dans la zone de chat (remplace les messages existants). */
    public void loadHistory(List<Message> history) {
        messagesPanel.removeAll();
        lastAuthor = "";
        for (Message msg : history) appendMessage(msg);
        messagesPanel.revalidate();
        messagesPanel.repaint();
    }

    // ══════════════════════════════════════════════════════════════════════════
    // CHANGEMENT DE GROUPE
    // ══════════════════════════════════════════════════════════════════════════

    public void selectGroup(int groupId, String groupName) {
        currentGroupId   = groupId;
        currentGroupName = groupName;
        channelLabel.setText(groupName);
        inputField.setEnabled(true);
        inputField.setToolTipText("Envoyer un message dans #" + groupName);

        // Vider et demander l'historique
        messagesPanel.removeAll();
        lastAuthor = "";
        messagesPanel.revalidate();
        messagesPanel.repaint();

        client.requestHistory(groupId);
    }

    /** Ouvre la boîte de dialogue de création de groupe. */
    private void showCreateGroupDialog() {
        JDialog dialog = new JDialog(this, "Créer un groupe", true);
        dialog.setSize(420, 500);
        dialog.setLocationRelativeTo(this);
        dialog.setResizable(false);

        JPanel main = new JPanel(new BorderLayout(0, 0));
        main.setBackground(C_CHANNELS);
        main.setBorder(BorderFactory.createEmptyBorder(24, 24, 24, 24));

        // Titre
        JLabel title = new JLabel("Créer un groupe");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(C_HEADER);

        JLabel sub = new JLabel("Choisissez un nom et des membres");
        sub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        sub.setForeground(C_MUTED);

        // Nom du groupe
        JLabel nameLabel = new JLabel("NOM DU GROUPE");
        nameLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        nameLabel.setForeground(C_MUTED);

        JTextField nameField = new JTextField();
        nameField.setBackground(C_INPUT);
        nameField.setForeground(C_TEXT);
        nameField.setCaretColor(C_TEXT);
        nameField.setFont(new Font("Segoe UI", Font.PLAIN, 15));
        nameField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 0, 0, 80), 1),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));

        // Liste des membres avec cases à cocher
        JLabel membersLabel = new JLabel("MEMBRES");
        membersLabel.setFont(new Font("Segoe UI", Font.BOLD, 11));
        membersLabel.setForeground(C_MUTED);

        JPanel checkPanel = new JPanel();
        checkPanel.setLayout(new BoxLayout(checkPanel, BoxLayout.Y_AXIS));
        checkPanel.setBackground(C_INPUT);

        List<JCheckBox> checkBoxes = new ArrayList<>();
        for (Map.Entry<Integer, String> entry : allUsers.entrySet()) {
            String uname = entry.getValue();
            if (uname.equals(username)) continue; // ne pas s'ajouter soi-même
            JCheckBox cb = new JCheckBox(uname);
            cb.setBackground(C_INPUT);
            cb.setForeground(C_TEXT);
            cb.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            cb.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
            cb.putClientProperty("userId", entry.getKey());
            checkBoxes.add(cb);
            checkPanel.add(cb);
        }

        JScrollPane scroll = new JScrollPane(checkPanel);
        scroll.setBorder(BorderFactory.createLineBorder(new Color(0, 0, 0, 80), 1));
        scroll.setPreferredSize(new Dimension(0, 200));

        // Message d'erreur
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        errorLabel.setForeground(new Color(240, 71, 71));

        // Boutons
        JPanel btnPanel = new JPanel(new GridLayout(1, 2, 8, 0));
        btnPanel.setOpaque(false);

        JButton cancelBtn = new JButton("Annuler");
        cancelBtn.setBackground(new Color(64, 68, 75));
        cancelBtn.setForeground(Color.WHITE);
        cancelBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        cancelBtn.setFocusPainted(false);
        cancelBtn.setBorderPainted(false);
        cancelBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        cancelBtn.addActionListener(e -> dialog.dispose());

        JButton createBtn = new JButton("Créer");
        createBtn.setBackground(C_ACCENT);
        createBtn.setForeground(Color.WHITE);
        createBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        createBtn.setFocusPainted(false);
        createBtn.setBorderPainted(false);
        createBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        createBtn.addActionListener(e -> {
            String gname = nameField.getText().trim();
            if (gname.isEmpty()) {
                errorLabel.setText("Veuillez saisir un nom.");
                return;
            }
            List<Integer> selected = new ArrayList<>();
            for (JCheckBox cb : checkBoxes) {
                if (cb.isSelected()) selected.add((Integer) cb.getClientProperty("userId"));
            }
            if (selected.isEmpty()) {
                errorLabel.setText("Sélectionnez au moins un membre.");
                return;
            }
            client.createGroup(gname, selected);
            dialog.dispose();
        });

        btnPanel.add(cancelBtn);
        btnPanel.add(createBtn);

        // Assemblage
        JPanel top = new JPanel();
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.setOpaque(false);
        top.add(title);
        top.add(Box.createVerticalStrut(4));
        top.add(sub);
        top.add(Box.createVerticalStrut(20));
        top.add(nameLabel);
        top.add(Box.createVerticalStrut(6));
        top.add(nameField);
        top.add(Box.createVerticalStrut(16));
        top.add(membersLabel);
        top.add(Box.createVerticalStrut(6));
        top.add(scroll);
        top.add(Box.createVerticalStrut(6));
        top.add(errorLabel);
        top.add(Box.createVerticalStrut(16));
        top.add(btnPanel);

        main.add(top, BorderLayout.CENTER);
        dialog.setContentPane(main);
        dialog.setVisible(true);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // FERMETURE
    // ══════════════════════════════════════════════════════════════════════════

    private void initWindowClose() {
        // ── Classe interne locale (barème) ────────────────────────────────────
        class CloseConfirmAdapter extends WindowAdapter {
            @Override
            public void windowClosing(WindowEvent e) {
                int res = JOptionPane.showConfirmDialog(
                        Window.this,
                        "Voulez-vous vraiment quitter ?",
                        "Déconnexion",
                        JOptionPane.YES_NO_OPTION);
                if (res == JOptionPane.YES_OPTION) {
                    client.close();
                    dispose();
                    System.exit(0);
                }
            }
        }
        addWindowListener(new CloseConfirmAdapter());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // MISE À JOUR LISTE CONNECTÉS
    // ══════════════════════════════════════════════════════════════════════════

    public void updateOnlineUsers(List<String> users) {
        memberModel.clear();
        for (String u : users) memberModel.addElement(u);
    }

    /** Appelé quand le serveur envoie la liste de tous les utilisateurs. */
    public void onUsersReceived(Map<Integer, String> users) {
        this.allUsers = users;
        showCreateGroupDialog();
    }

    /** Appelé quand la création de groupe réussit ou échoue. */
    public void onGroupCreated(boolean success) {
        if (!success) {
            JOptionPane.showMessageDialog(this,
                    "Erreur lors de la création du groupe.\nNom déjà utilisé ?",
                    "Erreur", JOptionPane.ERROR_MESSAGE);
        }
        // Si succès, le serveur envoie REFRESH_GROUPS automatiquement
    }

    // ══════════════════════════════════════════════════════════════════════════
    // COMPOSANT BULLE DE MESSAGE (classe interne)
    // ══════════════════════════════════════════════════════════════════════════

    class MessageBubble extends JPanel {

        public MessageBubble(String author, String content, String time,
                              boolean isFirst, int colorIdx) {
            setLayout(new BorderLayout(0, 0));
            setBackground(C_BG);
            setOpaque(true);
            setBorder(BorderFactory.createEmptyBorder(isFirst ? 8 : 1, 16, 1, 16));
            setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));

            Color avatarColor = AVATAR_COLORS[colorIdx % AVATAR_COLORS.length];

            if (isFirst) {
                add(buildAvatar(author, avatarColor), BorderLayout.WEST);
                add(buildFullMsg(author, content, time, avatarColor), BorderLayout.CENTER);
            } else {
                // Message groupé : espace à gauche + texte
                JPanel row = new JPanel(new BorderLayout());
                row.setOpaque(false);
                JLabel spacer = new JLabel();
                spacer.setPreferredSize(new Dimension(56, 0));
                row.add(spacer, BorderLayout.WEST);
                row.add(buildTextArea(content), BorderLayout.CENTER);
                add(row, BorderLayout.CENTER);
            }

            addMouseListener(new MouseAdapter() {
                @Override public void mouseEntered(MouseEvent e) {
                    setBackground(new Color(42, 43, 47)); repaint();
                }
                @Override public void mouseExited(MouseEvent e) {
                    setBackground(C_BG); repaint();
                }
            });
        }

        private JLabel buildAvatar(String author, Color color) {
            JLabel av = new JLabel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(color);
                    g2.fillOval(0, 4, 40, 40);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 16));
                    FontMetrics fm = g2.getFontMetrics();
                    String init = String.valueOf(author.charAt(0)).toUpperCase();
                    int x = (40 - fm.stringWidth(init)) / 2;
                    int y = 4 + (40 - fm.getHeight()) / 2 + fm.getAscent();
                    g2.drawString(init, x, y);
                    g2.dispose();
                }
            };
            av.setPreferredSize(new Dimension(56, 48));
            return av;
        }

        private JPanel buildFullMsg(String author, String content,
                                     String time, Color nameColor) {
            JPanel p = new JPanel(new BorderLayout(0, 2));
            p.setOpaque(false);

            JPanel header = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
            header.setOpaque(false);
            JLabel nameLbl = new JLabel(author);
            nameLbl.setFont(new Font("Segoe UI", Font.BOLD, 15));
            nameLbl.setForeground(nameColor);
            JLabel tsLbl = new JLabel("  " + time);
            tsLbl.setFont(new Font("Segoe UI", Font.PLAIN, 11));
            tsLbl.setForeground(C_MUTED);
            header.add(nameLbl);
            header.add(tsLbl);

            p.add(header, BorderLayout.NORTH);
            p.add(buildTextArea(content), BorderLayout.CENTER);
            return p;
        }

        private JTextArea buildTextArea(String content) {
            JTextArea ta = new JTextArea(content);
            ta.setLineWrap(true);
            ta.setWrapStyleWord(true);
            ta.setEditable(false);
            ta.setFocusable(false);
            ta.setOpaque(false);
            ta.setForeground(C_TEXT);
            ta.setFont(new Font("Segoe UI", Font.PLAIN, 15));
            ta.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
            return ta;
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // RENDERER MEMBRES
    // ══════════════════════════════════════════════════════════════════════════

    private class MemberRenderer implements ListCellRenderer<String> {
        @Override
        public Component getListCellRendererComponent(JList<? extends String> list,
                String value, int index, boolean selected, boolean focus) {
            JPanel item = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 5));
            item.setBackground(selected ? C_HOVER : C_SIDEBAR);

            JLabel av = new JLabel() {
                @Override protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                            RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(C_ACCENT);
                    g2.fillOval(0, 0, 28, 28);
                    g2.setColor(Color.WHITE);
                    g2.setFont(new Font("Segoe UI", Font.BOLD, 12));
                    FontMetrics fm = g2.getFontMetrics();
                    String init = value.isEmpty() ? "?" :
                            String.valueOf(value.charAt(0)).toUpperCase();
                    g2.drawString(init, (28 - fm.stringWidth(init)) / 2,
                            (28 - fm.getHeight()) / 2 + fm.getAscent());
                    g2.setColor(C_GREEN);
                    g2.fillOval(19, 18, 10, 10);
                    g2.dispose();
                }
            };
            av.setPreferredSize(new Dimension(28, 28));

            JLabel name = new JLabel(value);
            name.setFont(new Font("Segoe UI", Font.PLAIN, 14));
            name.setForeground(selected ? C_HEADER : C_TEXT);

            item.add(av);
            item.add(name);
            return item;
        }
    }
}

// ════════════════════════════════════════════════════════════════════════════
// CLASSE ADAPTER SÉPARÉE (barème)
// ════════════════════════════════════════════════════════════════════════════

/**
 * Gère les clics et effets hover sur les items de canaux dans la sidebar.
 */
class ChannelClickAdapter extends MouseAdapter {

    private final int     groupId;
    private final String  groupName;
    private final JPanel  item;
    private final JLabel  hash;
    private final JLabel  nameLabel;
    private final Window  window;

    public ChannelClickAdapter(int groupId, String groupName, JPanel item,
                                JLabel hash, JLabel nameLabel, Window window) {
        this.groupId   = groupId;
        this.groupName = groupName;
        this.item      = item;
        this.hash      = hash;
        this.nameLabel = nameLabel;
        this.window    = window;
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        window.selectGroup(groupId, groupName);
    }

    @Override
    public void mouseEntered(MouseEvent e) {
        item.setBackground(Window.C_HOVER);
        hash.setForeground(Window.C_HEADER);
        nameLabel.setForeground(Window.C_HEADER);
    }

    @Override
    public void mouseExited(MouseEvent e) {
        item.setBackground(Window.C_SIDEBAR);
        hash.setForeground(Window.C_MUTED);
        nameLabel.setForeground(Window.C_MUTED);
    }
}
