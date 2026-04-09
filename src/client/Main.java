package client;

import javax.swing.*;
import java.awt.*;

// page de redirection vers connexion ou inscription avec 2 boutons
public class Main extends JFrame {
    public static void main(String[] args) {


        SwingUtilities.invokeLater(() -> {
            Main mainFrame = new Main();
            mainFrame.setTitle("Bienvenue");
            mainFrame.setSize(300, 200);
            mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            mainFrame.setLayout(new GridLayout(2, 1, 10, 10));

            JButton loginButton = new JButton("Se connecter");
            loginButton.addActionListener(e -> {
                mainFrame.dispose();
                new Connection().setVisible(true);  
            });

            JButton registerButton = new JButton("S'inscrire");
            registerButton.addActionListener(e -> {
                mainFrame.dispose();
                new Inscription().setVisible(true);
            });

            mainFrame.add(loginButton);
            mainFrame.add(registerButton);
            mainFrame.setVisible(true);
        });



    }
}
