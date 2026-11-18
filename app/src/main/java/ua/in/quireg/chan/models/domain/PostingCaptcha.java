package ua.in.quireg.chan.models.domain;

import ua.in.quireg.chan.common.utils.StringUtils;

public final class PostingCaptcha {
    public enum Type {
        EMOJI_CAPTCHA("emoji_captcha"),
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
    private final String mChallengeNonce;

    private PostingCaptcha(Type type, String emojiCaptchaId, String challengeNonce) {
        mType = type;
        mEmojiCaptchaId = emojiCaptchaId;
        mChallengeNonce = challengeNonce;
    }

    public static PostingCaptcha emoji(String emojiCaptchaId, String challengeNonce) {
        if (StringUtils.isEmpty(emojiCaptchaId)) {
            throw new IllegalArgumentException("emoji captcha ID is required");
        }
        return new PostingCaptcha(Type.EMOJI_CAPTCHA, emojiCaptchaId, challengeNonce);
    }

    public static PostingCaptcha passcode() {
        return new PostingCaptcha(Type.PASSCODE, null, null);
    }

    public static PostingCaptcha noCaptcha() {
        return new PostingCaptcha(Type.NO_CAPTCHA, null, null);
    }

    public Type getType() {
        return mType;
    }

    public String getEmojiCaptchaId() {
        return mEmojiCaptchaId;
    }

    public String getChallengeNonce() {
        return mChallengeNonce;
    }
}
