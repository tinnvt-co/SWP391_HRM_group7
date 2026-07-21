package util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class PasswordUtil {

    public static final String PASSWORD_REQUIREMENTS =
            "Password must be at least 8 characters and include at least one number and one special character.";

    private static final String HASH_PREFIX = "pbkdf2_sha256";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int HASH_BYTES = 32;
    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    public static boolean meetsRequirements(String password) {
        if (password == null || password.codePointCount(0, password.length()) < 8) {
            return false;
        }
        boolean hasNumber = password.codePoints().anyMatch(Character::isDigit);
        boolean hasSpecialCharacter = password.codePoints()
                .anyMatch(c -> !Character.isLetterOrDigit(c) && !Character.isWhitespace(c));
        return hasNumber && hasSpecialCharacter;
    }

    public static String hash(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password must not be null.");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);
        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, HASH_BYTES);

        return HASH_PREFIX + "$"
                + ITERATIONS + "$"
                + Base64.getEncoder().encodeToString(salt) + "$"
                + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean verify(String password, String storedPassword) {
        if (password == null || storedPassword == null || storedPassword.isBlank()) {
            return false;
        }

        if (!isPbkdf2Hash(storedPassword)) {
            return constantTimeEquals(
                    password.getBytes(StandardCharsets.UTF_8),
                    storedPassword.getBytes(StandardCharsets.UTF_8));
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }

        try {
            int iterations = Integer.parseInt(parts[1]);
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
            byte[] actualHash = pbkdf2(password.toCharArray(), salt, iterations, expectedHash.length);
            return constantTimeEquals(actualHash, expectedHash);
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    public static boolean needsRehash(String storedPassword) {
        if (!isPbkdf2Hash(storedPassword)) {
            return true;
        }

        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return true;
        }

        try {
            return Integer.parseInt(parts[1]) < ITERATIONS;
        } catch (NumberFormatException ex) {
            return true;
        }
    }

    private static boolean isPbkdf2Hash(String storedPassword) {
        return storedPassword != null && storedPassword.startsWith(HASH_PREFIX + "$");
    }

    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int hashBytes) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, hashBytes * 8);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException ex) {
            throw new IllegalStateException("Could not hash password.", ex);
        } finally {
            spec.clearPassword();
        }
    }

    private static boolean constantTimeEquals(byte[] left, byte[] right) {
        return MessageDigest.isEqual(left, right);
    }
}
