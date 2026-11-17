package ua.in.quireg.chan.models.domain;

import ua.in.quireg.chan.common.utils.StringUtils;

public final class PostingCaptcha {
    public enum Type {
        EMOJI_CAPTCHA("emoji"),
        PASSCODE("passcode"),
        NO_CAPTCHA("nocaptcha");

        private final String mApiValue;

        Type(String apiValue) {
            mApiValue = apiValue;
        }

        public String getApiValue() {
            return mApiValue;
        }
    }

    private final Type mType;
    private final String mEmojiCaptchaId;

    private PostingCaptcha(Type type, String emojiCaptchaId) {
        mType = type;
        mEmojiCaptchaId = emojiCaptchaId;
    }

    public static PostingCaptcha emoji(String emojiCaptchaId) {
        if (StringUtils.isEmpty(emojiCaptchaId)) {
            throw new IllegalArgumentException("emoji captcha ID is required");
        }
        return new PostingCaptcha(Type.EMOJI_CAPTCHA, emojiCaptchaId);
    }

    public static PostingCaptcha passcode() {
        return new PostingCaptcha(Type.PASSCODE, null);
    }

    public static PostingCaptcha noCaptcha() {
        return new PostingCaptcha(Type.NO_CAPTCHA, null);
    }

    public Type getType() {
        return mType;
    }

    public String getEmojiCaptchaId() {
        return mEmojiCaptchaId;
    }
}
