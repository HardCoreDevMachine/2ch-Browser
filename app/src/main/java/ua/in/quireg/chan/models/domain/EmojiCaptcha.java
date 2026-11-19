package ua.in.quireg.chan.models.domain;

import android.graphics.Bitmap;

import java.util.List;

public final class EmojiCaptcha {
    private final String mTokenId;
    private final String mChallengeNonce;
    private final String mOrigin;
    private final Bitmap mImage;
    private final List<Bitmap> mKeyboard;

    public EmojiCaptcha(String tokenId, String challengeNonce, String origin, Bitmap image, List<Bitmap> keyboard) {
        mTokenId = tokenId;
        mChallengeNonce = challengeNonce;
        mOrigin = origin;
        mImage = image;
        mKeyboard = keyboard;
    }

    public String getChallengeNonce() {
        return mChallengeNonce;
    }

    public String getOrigin() {
        return mOrigin;
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
