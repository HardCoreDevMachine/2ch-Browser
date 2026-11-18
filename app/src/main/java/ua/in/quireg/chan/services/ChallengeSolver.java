package ua.in.quireg.chan.services;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Solves the posting proof-of-work returned with an emoji captcha ID:
 * finds i in [0, limit) such that sha512(template with %d replaced by i) == hash.
 * The result is sent to /user/posting as 2ch_challenge, as the 2ch web client does.
 */
public final class ChallengeSolver {
    private static final Charset UTF8 = Charset.forName("UTF-8");

    private ChallengeSolver() {
    }

    /** @return the nonce, or null when no value below limit matches. */
    public static String solve(String hash, String template, int limit) {
        if (hash == null || template == null || !template.contains("%d")) {
            return null;
        }
        MessageDigest digest;
        try {
            digest = MessageDigest.getInstance("SHA-512");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
        for (int i = 0; i < limit; i++) {
            String candidate = Integer.toString(i);
            byte[] bytes = digest.digest(template.replace("%d", candidate).getBytes(UTF8));
            if (hash.equalsIgnoreCase(toHex(bytes))) {
                return candidate;
            }
        }
        return null;
    }

    private static String toHex(byte[] bytes) {
        char[] chars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int value = bytes[i] & 0xff;
            chars[i * 2] = Character.forDigit(value >>> 4, 16);
            chars[i * 2 + 1] = Character.forDigit(value & 0x0f, 16);
        }
        return new String(chars);
    }
}
