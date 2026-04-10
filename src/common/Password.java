package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire de hachage des mots de passe (SHA-256).
 * Placé dans "common" pour être utilisé côté client ET serveur.
 * Le client hache avant d'envoyer — la BD ne voit jamais le clair.
 */
public class Password {

    private Password() {}

    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 indisponible", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 indisponible", e);
        }
    }
}
