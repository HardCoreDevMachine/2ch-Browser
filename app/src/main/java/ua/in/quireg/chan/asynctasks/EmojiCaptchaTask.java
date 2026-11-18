package ua.in.quireg.chan.asynctasks;

import android.os.AsyncTask;

import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.CaptchaStep;
import ua.in.quireg.chan.models.domain.EmojiCaptcha;
import ua.in.quireg.chan.services.EmojiCaptchaService;

public class EmojiCaptchaTask extends AsyncTask<Void, Void, CaptchaStep> {
    public interface Listener {
        void onCaptchaStep(CaptchaStep step);
    }

    private final EmojiCaptchaService mService = Factory.resolve(EmojiCaptchaService.class);
    private final IWebsite mWebsite;
    private final String mBoard;
    private final String mThread;
    private final EmojiCaptcha mCaptcha;
    private final int mEmojiNumber;
    private final Listener mListener;

    public EmojiCaptchaTask(IWebsite website, String board, String thread, Listener listener) {
        mWebsite = website;
        mBoard = board;
        mThread = thread;
        mCaptcha = null;
        mEmojiNumber = -1;
        mListener = listener;
    }

    public EmojiCaptchaTask(IWebsite website, EmojiCaptcha captcha, int emojiNumber, Listener listener) {
        mWebsite = website;
        mBoard = null;
        mThread = null;
        mCaptcha = captcha;
        mEmojiNumber = emojiNumber;
        mListener = listener;
    }

    @Override
    protected CaptchaStep doInBackground(Void... params) {
        return mCaptcha == null
                ? mService.load(mWebsite, mBoard, mThread)
                : mService.click(mWebsite, mCaptcha, mEmojiNumber);
    }

    @Override
    protected void onPostExecute(CaptchaStep step) {
        if (!isCancelled()) {
            mListener.onCaptchaStep(step);
        }
    }
}
