package ua.in.quireg.chan.test;

import android.test.InstrumentationTestCase;

import ua.in.quireg.chan.boards.makaba.models.CaptchaApiResponse;
import ua.in.quireg.chan.boards.makaba.models.EmojiCaptchaShowResponse;
import ua.in.quireg.chan.services.EmojiCaptchaResponseParser;

public class EmojiCaptchaResponseParserTest extends InstrumentationTestCase {
    private final EmojiCaptchaResponseParser mParser = new EmojiCaptchaResponseParser();

    public void testParsesEmojiChallengeId() throws Exception {
        CaptchaApiResponse response = mParser.parseCaptcha("{\"challenge\":{\"hash\":\"h\",\"limit\":20000,\"template\":\"t%d\"},\"id\":\"token\",\"input\":\"emoji\",\"result\":1,\"type\":\"emoji\"}");

        assertEquals("emoji", response.type);
        assertEquals("token", response.id);
    }

    public void testParsesChallengeAndSolvedResponse() throws Exception {
        EmojiCaptchaShowResponse challenge = mParser.parseShow("{\"image\":\"target\",\"keyboard\":[\"one\",\"two\"]}");
        EmojiCaptchaShowResponse solved = mParser.parseShow("{\"success\":\"emoji-id\"}");

        assertEquals("target", challenge.image);
        assertEquals(2, challenge.keyboard.length);
        assertEquals("emoji-id", solved.success);
    }
}
