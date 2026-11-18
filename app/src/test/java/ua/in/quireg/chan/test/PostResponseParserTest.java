package ua.in.quireg.chan.test;

import junit.framework.TestCase;

import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.PostResponseParser;

public class PostResponseParserTest extends TestCase {

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

    public void testAcceptsCreatedReplyNumber() {
        assertTrue(mParser.isPostSuccessful("b", "{\"result\":1,\"num\":456}").isSuccess);
    }

    public void testRejectsUnconfirmedResponses() {
        String[] responses = {
                "{}", "null", "", "<html>Redirect</html>",
                "{\"result\":0}", "{\"result\":1}",
                "{\"result\":0,\"num\":456}", "{\"num\":456}",
                "{\"result\":1,\"num\":0,\"thread\":-1}",
                "{\"result\":0,\"error\":{\"code\":0}}"
        };
        for (String response : responses) {
            SendPostResult result = mParser.isPostSuccessful("b", response);
            assertFalse(response, result.isSuccess);
            assertNotNull(response, result.error);
            assertFalse(response, result.error.isEmpty());
        }
    }

    public void testServerErrorOverridesSuccessFields() {
        SendPostResult result = mParser.isPostSuccessful("b",
                "{\"result\":1,\"num\":456,\"error\":{\"code\":-5}}");
        assertFalse(result.isSuccess);
        assertEquals(-5, result.errorCode);
        assertTrue(result.error.contains("-5"));
    }

    public void testExplainsMissingAcknowledgement() {
        SendPostResult result = mParser.isPostSuccessful("b", "{\"result\":0}");
        assertTrue(result.error.contains("result=0"));
        assertTrue(result.error.contains("num=0"));
    }
}
