package common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public class HashUtils {

    /**
     * Genera el hash SHA-256 de una cadena de texto.
     * @param input El texto a procesar.
     * @return El hash en formato hexadecimal (String).
     */
    public static String getSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(input.getBytes(StandardCharsets.UTF_8));

            // Característica de Java 17+: HexFormat para convertir bytes a String limpio
            return HexFormat.of().formatHex(hashBytes);

        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Error inicializando SHA-256", e);
        }
    }

    /**
     * Verifica si un hash cumple con la dificultad (empieza por ceros).
     * @param hash El hash calculado.
     * @param zeros Número de ceros requeridos (ej. 2).
     * @return true si es válido.
     */
    public static boolean checkHash(String hash, int zeros) {
        String prefix = "0".repeat(zeros);
        return hash.startsWith(prefix);
    }
}