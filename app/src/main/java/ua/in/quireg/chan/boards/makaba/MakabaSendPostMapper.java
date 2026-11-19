package ua.in.quireg.chan.boards.makaba;

import java.io.File;

import org.apache.http.HttpEntity;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;
import ua.in.quireg.chan.services.http.MultipartHttpEntity;

public class MakabaSendPostMapper {
    private static final MediaType OCTET_STREAM = MediaType.parse("application/octet-stream");
    private static final String BOARD = "board";
    private static final String THREAD = "thread";
    private static final String COMMENT = "comment";
    private static final String EMAIL = "email";
    private static final String NAME = "name";
    private static final String SUBJECT = "subject";
    private static final String CAPTCHA_TYPE = "captcha_type";
    private static final String EMOJI_CAPTCHA_ID = "emoji_captcha_id";
    private static final String CHALLENGE = "2ch_challenge";
    private static final String FILE = "file[]";

    public HttpEntity mapModelToHttpEntity(String boardName, SendPostModel model) {
        PostingCaptcha captcha = model.getCaptcha();
        if (captcha == null) {
            throw new IllegalArgumentException("Posting captcha is required");
        }

        MultipartBody.Builder body = new MultipartBody.Builder(Constants.MULTIPART_BOUNDARY)
                .setType(MultipartBody.FORM);
        addStringValue(body, "task", "post");
        addStringValue(body, BOARD, boardName);
        addStringValue(body, THREAD, StringUtils.nullIfEmpty(model.getParentThread()));
        addStringValue(body, COMMENT, StringUtils.emptyIfNull(model.getComment()));
        addStringValue(body, CAPTCHA_TYPE, captcha.getType().getApiValue());
        if (captcha.getType() == PostingCaptcha.Type.EMOJI_CAPTCHA) {
            addStringValue(body, EMOJI_CAPTCHA_ID, captcha.getEmojiCaptchaId());
            addStringValue(body, CHALLENGE, captcha.getChallengeNonce());
        }

        addStringValue(body, SUBJECT, model.getSubject());
        addStringValue(body, NAME, model.getName());
        addStringValue(body, EMAIL, model.isSage() ? Constants.SAGE_EMAIL : null);

        for (File file : model.getAttachedFiles()) {
            body.addFormDataPart(FILE, file.getName(), RequestBody.create(OCTET_STREAM, file));
        }

        if (model.getPolitics() != null) {
            addStringValue(body, "icon", model.getPolitics());
        }

        return new MultipartHttpEntity(body.build());
    }

    private void addStringValue(MultipartBody.Builder body, String key, String value) {
        if (value != null) {
            body.addFormDataPart(key, value);
        }
    }
}
