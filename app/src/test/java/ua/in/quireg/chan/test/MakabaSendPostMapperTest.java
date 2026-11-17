package ua.in.quireg.chan.test;

import android.test.InstrumentationTestCase;

import org.apache.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import ua.in.quireg.chan.boards.makaba.MakabaSendPostMapper;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;

public class MakabaSendPostMapperTest extends InstrumentationTestCase {
    public void testMapsEmojiCaptchaForModernPostingEndpoint() throws Exception {
        SendPostModel model = new SendPostModel(
                PostingCaptcha.emoji("emoji-solution"), "comment", false,
                new ArrayList<>(), null, null, null);
        model.setParentThread("123");

        HttpEntity entity = new MakabaSendPostMapper().mapModelToHttpEntity("b", model);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        String body = output.toString("UTF-8");

        assertTrue(body.contains("name=\"captcha_type\""));
        assertTrue(body.contains("emoji_captcha"));
        assertTrue(body.contains("name=\"emoji_captcha_id\""));
        assertTrue(body.contains("emoji-solution"));
        assertFalse(body.contains("2chaptcha"));
        assertFalse(body.contains("usercode"));
        assertFalse(body.contains("name=\"task\""));
    }

    public void testMapsNoCaptchaWithoutEmojiSolution() throws Exception {
        SendPostModel model = new SendPostModel(
                PostingCaptcha.noCaptcha(), "comment", false,
                new ArrayList<>(), null, null, null);

        HttpEntity entity = new MakabaSendPostMapper().mapModelToHttpEntity("b", model);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        String body = output.toString("UTF-8");

        assertTrue(body.contains("nocaptcha"));
        assertFalse(body.contains("emoji_captcha_id"));
    }
}
