package common;

import java.io.Serializable;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Objet échangé entre client et serveur via ObjectOutputStream/ObjectInputStream.
 * Doit implémenter Serializable.
 *
 * Corrections apportées :
 *   - Ajout du champ "senderUsername" : le serveur le renseigne avant le broadcast,
 *     permettant à l'UI d'afficher le nom de l'auteur sans requête supplémentaire.
 *   - Ajout du champ "timestamp" généré à la création.
 *   - Ajout de groupName pour l'affichage dans l'UI.
 */
public class Message implements Serializable {
    private static final long serialVersionUID = 2L;

    private final String text;
    private final int    groupId;
    private String senderUsername; // renseigné par le serveur avant broadcast
    private String groupName;      // renseigné par le serveur avant broadcast
    private final String timestamp;

    public Message(int groupId, String text) {
        this.groupId   = groupId;
        this.text      = text;
        this.timestamp = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        this.senderUsername = "?";
        this.groupName      = "";
    }

    @Override
    public String toString() {
        return "[" + groupName + "] " + senderUsername + " (" + timestamp + ") : " + text;
    }

    // Getters
    public String getText()           { return text; }
    public int    getGroupId()        { return groupId; }
    public String getSenderUsername() { return senderUsername; }
    public String getGroupName()      { return groupName; }
    public String getTimestamp()      { return timestamp; }

    // Setters (utilisés par le serveur)
    public void setSenderUsername(String u) { this.senderUsername = u; }
    public void setGroupName(String n)      { this.groupName = n; }
}
