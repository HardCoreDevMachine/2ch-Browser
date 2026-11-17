package ua.in.quireg.chan.services;

import ua.in.quireg.chan.common.library.ExtendedObjectMapper;

import ua.in.quireg.chan.boards.makaba.models.CaptchaApiResponse;
import ua.in.quireg.chan.boards.makaba.models.EmojiCaptchaShowResponse;

public class EmojiCaptchaResponseParser {
    private final ExtendedObjectMapper mMapper = new ExtendedObjectMapper();

    public CaptchaApiResponse parseCaptcha(String response) throws Exception {
        return mMapper.readValue(response, CaptchaApiResponse.class);
    }

    public EmojiCaptchaShowResponse parseShow(String response) throws Exception {
        return mMapper.readValue(response, EmojiCaptchaShowResponse.class);
    }
}
