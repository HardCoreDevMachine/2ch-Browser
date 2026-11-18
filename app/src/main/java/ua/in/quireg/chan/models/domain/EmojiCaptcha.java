package ua.in.quireg.chan.models.domain;

import android.graphics.Bitmap;

import java.util.List;

public final class EmojiCaptcha {
    private final String mTokenId;
    private final String mChallengeNonce;
    private final Bitmap mImage;
    private final List<Bitmap> mKeyboard;

    public EmojiCaptcha(String tokenId, String challengeNonce, Bitmap image, List<Bitmap> keyboard) {
        mTokenId = tokenId;
        mChallengeNonce = challengeNonce;
        mImage = image;
        mKeyboard = keyboard;
    }

    public String getChallengeNonce() {
        return mChallengeNonce;
    }

    public String getTokenId() {
        return mTokenId;
    }

    public Bitmap getImage() {
        return mImage;
    }

    public List<Bitmap> getKeyboard() {
        return mKeyboard;
    }
}
