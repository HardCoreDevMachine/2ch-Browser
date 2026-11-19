package ua.in.quireg.chan.test;

import junit.framework.TestCase;

import ua.in.quireg.chan.boards.makaba.models.ApiError;

public class ApiErrorTest extends TestCase {

    /** Every code described in api.yml ErrorCode, including -50/-51/-52 omitted from the enum. */
    private static final int[] DESCRIBED_CODES = {
            0, 403, 666, 667,
            -2, -3, -31, -4, -41, -42, -5, -6, -7, -8, -9,
            -10, -11, -12, -13, -14, -15, -16, -19, -20, -21, -22, -23,
            -50, -51, -52,
            -300, -301, -302, -303, -304,
            -24, -25
    };

    private static ApiError error(int code, String message) {
        ApiError error = new ApiError();
        error.code = code;
        error.message = message;
        return error;
    }

    public void testServerMessageHasPriority() {
        assertEquals("Бан №42: флуд", error(-6, "Бан №42: флуд").getMessage());
        assertEquals("Бан №42: флуд", error(-9999, "Бан №42: флуд").getMessage());
        assertEquals("Бан №42: флуд", error(0, "Бан №42: флуд").getMessage());
    }

    public void testEveryDescribedCodeHasHumanReadableFallback() {
        for (int code : DESCRIBED_CODES) {
            String message = error(code, null).getMessage();
            assertNotNull("code=" + code, message);
            assertFalse("code=" + code, message.trim().isEmpty());
            assertFalse("code=" + code + " fell back to unknown", message.contains("Неизвестная"));

            // Blank server message must not suppress the mapping.
            assertEquals("code=" + code, message, error(code, "   ").getMessage());
            assertEquals("code=" + code, message, error(code, "").getMessage());
        }
    }

    public void testUnknownCodeHasIntelligibleFallback() {
        String message = error(-7777, null).getMessage();
        assertNotNull(message);
        assertFalse(message.trim().isEmpty());
        assertTrue(message.contains("-7777"));
    }

    public void testCodeZeroMeansNoError() {
        assertEquals("Ошибки нет.", error(0, null).getMessage());
    }
}
