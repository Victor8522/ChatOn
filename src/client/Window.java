package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

public class Window extends JFrame {

    // Couleurs définies dans ton code
    private Color BG_DARK    = new Color(49, 51, 56);
    private Color BG_SIDEBAR = new Color(35, 36, 40);
    private Color BG_INPUT   = new Color(64, 68, 75);
    private Color TEXT_COLOR = new Color(220, 221, 222);
    private Color ACCENT     = new Color(88, 101, 242);

    public Window() {
        setTitle("Discord Clone - Swing");
        setSize(1000, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Construction de l'interface
        add(buildSidebar(), BorderLayout.WEST);
        add(buildChatArea(), BorderLayout.CENTER);
        add(buildUserList(), BorderLayout.EAST);
    }

    private JPanel buildSidebar() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_SIDEBAR);
        panel.setPreferredSize(new Dimension(240, 0));

        JLabel title = new JLabel("  💬 Mon Chat");
        title.setForeground(TEXT_COLOR);
        title.setFont(new Font("Segoe UI", Font.BOLD, 16));
        title.setPreferredSize(new Dimension(240, 50));
        title.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(60, 62, 67)));

        DefaultListModel<String> model = new DefaultListModel<>();
        model.addElement("# général");
        model.addElement("# blabla");
        
        JList<String> channelList = new JList<>(model);
        channelList.setBackground(BG_SIDEBAR);
        channelList.setForeground(new Color(148, 155, 164));
        channelList.setSelectionBackground(new Color(64, 68, 75));
        channelList.setSelectionForeground(Color.WHITE);
        channelList.setFont(new Font("Segoe UI", Font.PLAIN, 14));

        panel.add(title, BorderLayout.NORTH);
        panel.add(new JScrollPane(channelList), BorderLayout.CENTER);
        panel.add(buildUserInfoBar(), BorderLayout.SOUTH);
        return panel;
    }

    private JPanel buildChatArea() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setBackground(BG_DARK);

        JPanel messagesContainer = new JPanel();
        messagesContainer.setLayout(new BoxLayout(messagesContainer, BoxLayout.Y_AXIS));
        messagesContainer.setBackground(BG_DARK);

        JScrollPane scrollPane = new JScrollPane(messagesContainer);
        scrollPane.setBorder(null);

        panel.add(scrollPane, BorderLayout.CENTER);
        panel.add(buildInputArea(), BorderLayout.SOUTH);
        
        // Ajout d'un message de test pour voir le rendu
        messagesContainer.add(new MessageBubble("Gemini", "Salut ! Comment ça va ?", "14:30", false));
        
        return panel;
    }

    private JPanel buildInputArea() {
        JPanel panel = new JPanel(new BorderLayout(8, 0));
        panel.setBackground(BG_DARK);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 16, 16, 16));

        JTextField inputField = new JTextField();
        inputField.setBackground(BG_INPUT);
        inputField.setForeground(TEXT_COLOR);
        inputField.setCaretColor(Color.WHITE);
        inputField.setBorder(BorderFactory.createEmptyBorder(10, 12, 10, 12));

        // Action de la touche Entrée
        inputField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    System.out.println("Message envoyé : " + inputField.getText());
                    inputField.setText("");
                }
            }
        });

        panel.add(inputField, BorderLayout.CENTER);
        return panel;
    }

    // Méthodes simplifiées pour que le code compile
    private JPanel buildUserList() {
        JPanel p = new JPanel();
        p.setBackground(new Color(43, 45, 49));
        p.setPreferredSize(new Dimension(200, 0));
        return p;
    }

    private JPanel buildUserInfoBar() {
        JPanel p = new JPanel();
        p.setBackground(new Color(35, 36, 40));
        p.setPreferredSize(new Dimension(240, 50));
        return p;
    }

    // Classe interne pour les messages[cite: 1]
    class MessageBubble extends JPanel {
        public MessageBubble(String author, String content, String time, boolean isOwn) {
            setLayout(new BorderLayout(8, 2));
            setBackground(BG_DARK);
            setBorder(BorderFactory.createEmptyBorder(6, 16, 6, 16));

            JLabel avatar = new JLabel(String.valueOf(author.charAt(0)).toUpperCase());
            avatar.setPreferredSize(new Dimension(40, 40));
            avatar.setOpaque(true);
            avatar.setHorizontalAlignment(SwingConstants.CENTER);
            avatar.setBackground(isOwn ? ACCENT : new Color(87, 242, 135));
            avatar.setForeground(Color.WHITE);

            JPanel right = new JPanel(new BorderLayout());
            right.setBackground(BG_DARK);
            
            JLabel nameLabel = new JLabel(author + "  " + time);
            nameLabel.setForeground(isOwn ? ACCENT : Color.WHITE);
            
            JTextArea contentArea = new JTextArea(content);
            contentArea.setEditable(false);
            contentArea.setBackground(BG_DARK);
            contentArea.setForeground(TEXT_COLOR);

            right.add(nameLabel, BorderLayout.NORTH);
            right.add(contentArea, BorderLayout.CENTER);

            add(avatar, BorderLayout.WEST);
            add(right, BorderLayout.CENTER);
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new Window().setVisible(true);
        });
    }
}