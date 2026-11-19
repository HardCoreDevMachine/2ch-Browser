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
    private final String mOrigin;

    private PostingCaptcha(Type type, String emojiCaptchaId, String challengeNonce, String origin) {
        mType = type;
        mEmojiCaptchaId = emojiCaptchaId;
        mChallengeNonce = challengeNonce;
        mOrigin = origin;
    }

    public static PostingCaptcha emoji(String emojiCaptchaId, String challengeNonce, String origin) {
        if (StringUtils.isEmpty(emojiCaptchaId)) {
            throw new IllegalArgumentException("emoji captcha ID is required");
        }
        return new PostingCaptcha(Type.EMOJI_CAPTCHA, emojiCaptchaId, challengeNonce, origin);
    }

    public static PostingCaptcha passcode(String origin) {
        return new PostingCaptcha(Type.PASSCODE, null, null, origin);
    }

    public static PostingCaptcha noCaptcha(String origin) {
        return new PostingCaptcha(Type.NO_CAPTCHA, null, null, origin);
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

    public String getOrigin() {
        return mOrigin;
    }
}
