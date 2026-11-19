package ua.in.quireg.chan.test;

import junit.framework.TestCase;

import org.apache.http.HttpEntity;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;

import ua.in.quireg.chan.boards.makaba.MakabaSendPostMapper;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;

public class MakabaSendPostMapperTest extends TestCase {
    public void testMapsEmojiCaptchaForModernPostingEndpoint() throws Exception {
        SendPostModel model = new SendPostModel(
                PostingCaptcha.emoji("emoji-solution", "8594", "https://2ch.su"), "comment", false,
                new ArrayList<>(), null, null, null);
        model.setParentThread("123");

        HttpEntity entity = new MakabaSendPostMapper().mapModelToHttpEntity("b", model);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        String body = output.toString("UTF-8");

        assertTrue(body.contains("name=\"captcha_type\""));
        assertTrue(body.contains("\r\n\r\nemoji_captcha\r\n"));
        assertTrue(body.contains("name=\"2ch_challenge\""));
        assertTrue(body.contains("\r\n\r\n8594\r\n"));
        assertTrue(body.contains("name=\"emoji_captcha_id\""));
        assertTrue(body.contains("emoji-solution"));
        assertFalse(body.contains("2chaptcha"));
        assertFalse(body.contains("usercode"));
        assertTrue(body.contains("name=\"task\""));
        assertTrue(body.contains("\r\n\r\npost\r\n"));
    }

    public void testMapsNoCaptchaWithoutEmojiSolution() throws Exception {
        SendPostModel model = new SendPostModel(
                PostingCaptcha.noCaptcha("https://2ch.su"), "comment", false,
                new ArrayList<>(), null, null, null);

        HttpEntity entity = new MakabaSendPostMapper().mapModelToHttpEntity("b", model);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        entity.writeTo(output);
        String body = output.toString("UTF-8");

        assertTrue(body.contains("nocaptcha"));
        assertFalse(body.contains("emoji_captcha_id"));
    }
}
