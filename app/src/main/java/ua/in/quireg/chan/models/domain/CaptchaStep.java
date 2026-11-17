package ua.in.quireg.chan.models.domain;

public abstract class CaptchaStep {
    private CaptchaStep() {
    }

    public static final class EmojiChallenge extends CaptchaStep {
        private final EmojiCaptcha mCaptcha;

        public EmojiChallenge(EmojiCaptcha captcha) {
            mCaptcha = captcha;
        }

        public EmojiCaptcha getCaptcha() {
            return mCaptcha;
        }
    }

    public static final class Ready extends CaptchaStep {
        private final PostingCaptcha mCaptcha;

        public Ready(PostingCaptcha captcha) {
            mCaptcha = captcha;
        }

        public PostingCaptcha getCaptcha() {
            return mCaptcha;
        }
    }

    public static final class Error extends CaptchaStep {
        private final String mMessage;

        public Error(String message) {
            mMessage = message;
        }

        public String getMessage() {
            return mMessage;
        }
    }
}
