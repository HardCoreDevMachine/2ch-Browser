package ua.in.quireg.chan.boards.makaba;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.StringBody;

import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;

public class MakabaSendPostMapper {
    private static final Charset utf = Constants.UTF8_CHARSET;
    private static final String BOARD = "board";
    private static final String THREAD = "thread";
    private static final String COMMENT = "comment";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String SUBJECT = "subject";
    private static final String CAPTCHA_TYPE = "captcha_type";
    private static final String EMOJI_CAPTCHA_ID = "emoji_captcha_id";
    private static final String FILE = "file[]";

    public HttpEntity mapModelToHttpEntity(String boardName, SendPostModel model) {
        PostingCaptcha captcha = model.getCaptcha();
        if (captcha == null) {
            throw new IllegalArgumentException("Posting captcha is required");
        }

        MultipartEntity multipartEntity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, Constants.MULTIPART_BOUNDARY, Constants.UTF8_CHARSET);
        this.addStringValue(multipartEntity, BOARD, boardName);
        this.addStringValue(multipartEntity, THREAD, model.getParentThread());
        this.addStringValue(multipartEntity, COMMENT, StringUtils.emptyIfNull(model.getComment()));
        this.addStringValue(multipartEntity, CAPTCHA_TYPE, captcha.getType().getApiValue());
        if (captcha.getType() == PostingCaptcha.Type.EMOJI_CAPTCHA) {
            this.addStringValue(multipartEntity, EMOJI_CAPTCHA_ID, captcha.getEmojiCaptchaId());
        }

        this.addStringValue(multipartEntity, SUBJECT, model.getSubject());
        this.addStringValue(multipartEntity, NAME, model.getName());
        this.addStringValue(multipartEntity, EMAIL, model.isSage() ? Constants.SAGE_EMAIL : null);

        List<File> files = model.getAttachedFiles();
        for (File file : files) {
            multipartEntity.addPart(FILE, new FileBody(file));
        }

        if (model.getPolitics() != null) {
            this.addStringValue(multipartEntity, "icon", model.getPolitics());
        }

        return multipartEntity;
    }

    private void addStringValue(MultipartEntity entity, String key, String value) {
        try {
            if (value != null) {
                entity.addPart(key, new StringBody(value, utf));
            }
        } catch (Exception ignored) {
            // ignore
        }
    }
}
