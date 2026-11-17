package ua.in.quireg.chan.test;

import android.test.InstrumentationTestCase;

import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.PostResponseParser;

public class PostResponseParserTest extends InstrumentationTestCase {

    private final PostResponseParser mParser = new PostResponseParser();

    public void testAcceptsSuccessfulApiResponse() {
        SendPostResult result = mParser.isPostSuccessful("b", "{\"result\":1,\"thread\":123}");

        assertTrue(result.isSuccess);
        assertEquals(0, result.errorCode);
    }

    public void testExposesCaptchaValidationError() {
        SendPostResult result = mParser.isPostSuccessful("b", "{\"result\":0,\"error\":{\"code\":-5,\"message\":\"Капча не валидна\"}}");

        assertFalse(result.isSuccess);
        assertEquals(-5, result.errorCode);
        assertEquals("Капча не валидна", result.error);
    }
}
