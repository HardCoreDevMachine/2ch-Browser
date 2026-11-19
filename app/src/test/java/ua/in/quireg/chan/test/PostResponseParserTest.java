package ua.in.quireg.chan.test;

import junit.framework.TestCase;

import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.PostResponseParser;

public class PostResponseParserTest extends TestCase {

    private final PostResponseParser mParser = new PostResponseParser();

    public void testAcceptsHarReplyAcknowledgement() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491", "{\"num\":256354,\"result\":1}");

        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        assertTrue(result.isAccepted());
        assertEquals("test", result.board);
        assertEquals(251491, result.threadNumber);
        assertEquals(256354, result.postNumber);
        assertEquals(Integer.valueOf(1), result.apiResult);
        assertNull(result.error);
    }

    public void testAcceptsNewThreadAcknowledgement() {
        SendPostResult result = mParser.parsePostingResponse("b", "0", "{\"result\":1,\"thread\":777}");

        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        assertEquals("b", result.board);
        assertEquals(777, result.threadNumber);
        assertEquals(777, result.postNumber);
        assertEquals(Integer.valueOf(1), result.apiResult);
        assertNull(result.error);
    }

    public void testAcceptsNewThreadAcknowledgementWithNullParent() {
        SendPostResult result = mParser.parsePostingResponse("b", null, "{\"result\":1,\"thread\":777}");

        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        assertEquals(777, result.threadNumber);
    }

    public void testAcceptsMatchingOpIdentifiersButRejectsContradictoryOnes() {
        SendPostResult matching = mParser.parsePostingResponse("test", null,
                "{\"result\":1,\"thread\":777,\"num\":777}");
        assertEquals(SendPostResult.State.ACCEPTED, matching.state);
        assertEquals(777, matching.postNumber);
        assertUnknown(mParser.parsePostingResponse("test", null,
                "{\"result\":1,\"thread\":777,\"num\":778}"));
    }

    public void testReadbackCannotConfirmAnUnacknowledgedSubmission() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491", "{}");
        mParser.applyVerification(result,
                "{\"result\":1,\"post\":{\"num\":256354,\"parent\":251491,\"board\":\"test\"}}");
        assertUnknown(result);
    }

    public void testUnknownOnUnsupportedResponses() {
        String[] responses = {
                "", "null", "{}", "<html>Redirect</html>", "not json", "[1,2,3]", "\"text\"",
                "{\"result\":0}", "{\"result\":1}", "{\"result\":2,\"num\":456}",
                "{\"result\":0,\"num\":456}", "{\"num\":456}",
                "{\"result\":1,\"num\":0,\"thread\":-1}", "{\"result\":0,\"error\":{\"code\":0}}"
        };
        for (String response : responses) {
            SendPostResult result = mParser.parsePostingResponse("test", "251491", response);
            assertEquals(response, SendPostResult.State.UNKNOWN, result.state);
            assertFalse(result.isAccepted());
            assertNotNull(response, result.error);
            assertFalse(response, result.error.isEmpty());
        }
    }

    public void testUnknownWhenScenarioIdentifierMissing() {
        // Reply ack without num.
        assertUnknown(mParser.parsePostingResponse("test", "251491", "{\"result\":1,\"thread\":251491}"));
        // New thread ack without thread.
        assertUnknown(mParser.parsePostingResponse("b", "0", "{\"result\":1,\"num\":456}"));
    }

    public void testUnknownOnContradictoryReply() {
        // Reply ack whose thread contradicts the requested parent.
        assertUnknown(mParser.parsePostingResponse("test", "251491", "{\"result\":1,\"num\":456,\"thread\":999}"));
    }

    private void assertUnknown(SendPostResult result) {
        assertEquals(SendPostResult.State.UNKNOWN, result.state);
        assertNotNull(result.error);
        assertFalse(result.error.isEmpty());
    }

    public void testServerErrorRejectsEvenWithSuccessFields() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491",
                "{\"result\":1,\"num\":256354,\"error\":{\"code\":-5}}");

        assertEquals(SendPostResult.State.REJECTED, result.state);
        assertFalse(result.isAccepted());
        assertEquals(-5, result.errorCode);
        assertNotNull(result.error);
        assertFalse(result.error.isEmpty());
    }

    public void testServerMessageIsPreserved() {
        SendPostResult result = mParser.parsePostingResponse("b", "10",
                "{\"result\":0,\"error\":{\"code\":-6,\"message\":\"Бан №42: флуд\"}}");

        assertEquals(SendPostResult.State.REJECTED, result.state);
        assertEquals(-6, result.errorCode);
        assertEquals("Бан №42: флуд", result.error);
    }

    public void testMalformedBodyDoesNotLeakIntoError() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491",
                "{\"result\":1,\"num\":256354 secretCookieValue");

        assertEquals(SendPostResult.State.UNKNOWN, result.state);
        assertFalse(result.error.contains("secretCookieValue"));
        assertFalse(result.error.contains("256354"));
    }

    public void testUnrelatedJsonDoesNotLeakIntoError() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491",
                "{\"session\":\"secretValue\"}");

        assertEquals(SendPostResult.State.UNKNOWN, result.state);
        assertFalse(result.error.contains("secretValue"));
    }

    public void testVerificationVerifiesExactReplyReadback() {
        SendPostResult result = acceptedReply();
        mParser.applyVerification(result,
                "{\"result\":1,\"post\":{\"num\":256354,\"parent\":251491,\"board\":\"test\"}}");

        assertEquals(SendPostResult.State.VERIFIED, result.state);
        assertTrue(result.isAccepted());
        assertEquals(251491, result.threadNumber);
        assertEquals(256354, result.postNumber);
        assertEquals(Integer.valueOf(1), result.verificationResult);
        assertEquals(0, result.verificationErrorCode);
        assertNull(result.error);
    }

    public void testVerificationVerifiesOpReadback() {
        SendPostResult result = mParser.parsePostingResponse("b", "0", "{\"result\":1,\"thread\":777}");
        mParser.applyVerification(result,
                "{\"result\":1,\"post\":{\"num\":777,\"parent\":0,\"board\":\"b\"}}");

        assertEquals(SendPostResult.State.VERIFIED, result.state);
        assertEquals(777, result.threadNumber);
        assertEquals(777, result.postNumber);
    }

    public void testVerificationRejectsOpReadbackWithParent() {
        SendPostResult result = mParser.parsePostingResponse("b", "0", "{\"result\":1,\"thread\":777}");
        mParser.applyVerification(result,
                "{\"result\":1,\"post\":{\"num\":777,\"parent\":776,\"board\":\"b\"}}");

        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        assertEquals(Integer.valueOf(1), result.verificationResult);
        assertNotNull(result.error);
    }

    public void testFailedReadbackKeepsAcknowledgement() {
        SendPostResult result = acceptedReply();
        mParser.applyVerification(result,
                "{\"result\":0,\"error\":{\"code\":-4,\"message\":\"Нет доступа\"}}");

        assertAcceptedWithIds(result);
        assertEquals(Integer.valueOf(0), result.verificationResult);
        assertEquals(-4, result.verificationErrorCode);
        assertTrue(result.error.contains("Нет доступа"));
    }

    public void testMismatchedReadbackKeepsAcknowledgement() {
        String[] readbacks = {
                "{\"result\":1,\"post\":{\"num\":999999,\"parent\":251491,\"board\":\"test\"}}",
                "{\"result\":1,\"post\":{\"num\":256354,\"parent\":251491,\"board\":\"b\"}}",
                "{\"result\":1,\"post\":{\"num\":256354,\"parent\":1,\"board\":\"test\"}}",
                "{\"result\":1,\"post\":{\"num\":256354,\"board\":\"test\"}}",
                "{\"result\":1}",
                "{\"result\":0}",
                "garbage secretTokenValue"
        };
        for (String readback : readbacks) {
            SendPostResult result = acceptedReply();
            mParser.applyVerification(result, readback);
            assertAcceptedWithIds(result);
            assertNotNull(readback, result.error);
            assertFalse(readback, result.error.isEmpty());
            assertFalse(readback, result.error.contains("secretTokenValue"));
        }
    }

    private SendPostResult acceptedReply() {
        SendPostResult result = mParser.parsePostingResponse("test", "251491", "{\"num\":256354,\"result\":1}");
        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        return result;
    }

    private void assertAcceptedWithIds(SendPostResult result) {
        assertEquals(SendPostResult.State.ACCEPTED, result.state);
        assertTrue(result.isAccepted());
        assertEquals(251491, result.threadNumber);
        assertEquals(256354, result.postNumber);
        assertEquals(Integer.valueOf(1), result.apiResult);
    }
}
