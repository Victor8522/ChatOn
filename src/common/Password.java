package common;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utilitaire de hachage des mots de passe.
 *
 * Placé dans le package "common" pour être utilisable
 * à la fois par le client ET par le serveur.
 *
 * Le client hache avant d'envoyer → le serveur ne voit jamais
 * le mot de passe en clair, la base de données non plus.
 */
public class Password {

    // Empêche l'instanciation — cette classe n'a que des méthodes statiques
    private Password() {}

    /**
     * Retourne le hash SHA-256 d'un mot de passe sous forme hexadécimale.
     *
     * Exemple :
     *   hash("monSecret") → "a3f8c2d1e5..."  (64 caractères hex)
     *   hash("monSecret") → "a3f8c2d1e5..."  (toujours identique)
     *   hash("autreSecret") → "9b2f71c0..."   (complètement différent)
     */
    public static String hash(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = md.digest(password.getBytes("UTF-8"));

            // Convertit chaque byte en 2 caractères hexadécimaux
            StringBuilder sb = new StringBuilder();
            for (byte b : bytes) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();

        } catch (NoSuchAlgorithmException e) {
            // SHA-256 est garanti présent dans tout JRE standard
            throw new RuntimeException("SHA-256 indisponible", e);
        } catch (java.io.UnsupportedEncodingException e) {
            throw new RuntimeException("UTF-8 indisponible", e);
        }
    }
}