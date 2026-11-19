package ua.in.quireg.chan.services;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.codehaus.jackson.map.ObjectMapper;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import ua.in.quireg.chan.boards.makaba.models.ApiError;
import ua.in.quireg.chan.boards.makaba.models.CaptchaApiResponse;
import ua.in.quireg.chan.boards.makaba.models.EmojiCaptchaShowResponse;
import ua.in.quireg.chan.common.library.ExtendedHttpClient;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.CaptchaStep;
import ua.in.quireg.chan.models.domain.EmojiCaptcha;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.services.http.HttpStringReader;

public class EmojiCaptchaService {
    private static final String CAPTCHA_TYPE_EMOJI = "emoji";
    private static final String CAPTCHA_TYPE_PASSCODE = "passcode";
    private static final String CAPTCHA_TYPE_NO_CAPTCHA = "nocaptcha";

    private final DefaultHttpClient mHttpClient;
    private final HttpStringReader mHttpStringReader;
    private final EmojiCaptchaResponseParser mResponseParser;
    private final ObjectMapper mJsonMapper = new ObjectMapper();

    public EmojiCaptchaService(DefaultHttpClient httpClient, HttpStringReader httpStringReader) {
        mHttpClient = httpClient;
        mHttpStringReader = httpStringReader;
        mResponseParser = new EmojiCaptchaResponseParser();
    }

    public CaptchaStep load(IWebsite website, String board, String thread) {
        try {
            URI base = new URI(website.getUrlBuilder().makeAbsolute("/"));
            String origin = new URI(base.getScheme(), null, base.getHost(), base.getPort(), null, null, null).toASCIIString();
            CaptchaApiResponse response = mResponseParser.parseCaptcha(
                    getJson(createIdUrl(origin, board, thread)));
            CaptchaStep error = errorStep(response.error);
            if (error != null) {
                return error;
            }
            if (CAPTCHA_TYPE_EMOJI.equals(response.type)) {
                if (StringUtils.isEmpty(response.id)) {
                    return new CaptchaStep.Error("Captcha response does not contain an ID");
                }
                return loadChallenge(origin, response.id, solveChallenge(response.challenge));
            }
            if (CAPTCHA_TYPE_PASSCODE.equals(response.type)) {
                return new CaptchaStep.Ready(PostingCaptcha.passcode(origin));
            }
            if (CAPTCHA_TYPE_NO_CAPTCHA.equals(response.type)) {
                return new CaptchaStep.Ready(PostingCaptcha.noCaptcha(origin));
            }
            return new CaptchaStep.Error("Unsupported captcha type: " + response.type);
        } catch (Exception e) {
            return new CaptchaStep.Error(errorMessage(e));
        }
    }

    public CaptchaStep click(EmojiCaptcha captcha, int emojiNumber) {
        if (captcha == null || emojiNumber < 0) {
            return new CaptchaStep.Error("Invalid emoji captcha selection");
        }
        try {
            EmojiCaptchaShowResponse response = mResponseParser.parseShow(postClick(captcha.getOrigin(), captcha.getTokenId(), emojiNumber));
            return toStep(captcha.getTokenId(), captcha.getChallengeNonce(), captcha.getOrigin(), response);
        } catch (Exception e) {
            return new CaptchaStep.Error(errorMessage(e));
        }
    }

    private CaptchaStep loadChallenge(String origin, String tokenId, String challengeNonce) throws Exception {
        Uri uri = Uri.parse(origin + "/api/captcha/emoji/show")
                .buildUpon()
                .appendQueryParameter("id", tokenId)
                .build();
        EmojiCaptchaShowResponse response = mResponseParser.parseShow(getJson(uri.toString()));
        return toStep(tokenId, challengeNonce, origin, response);
    }

    private static String solveChallenge(CaptchaApiResponse.Challenge challenge) {
        return challenge == null
                ? null
                : ChallengeSolver.solve(challenge.hash, challenge.template, challenge.limit);
    }

    private CaptchaStep toStep(String tokenId, String challengeNonce, String origin, EmojiCaptchaShowResponse response) {
        if (!StringUtils.isEmpty(response.success)) {
            return new CaptchaStep.Ready(PostingCaptcha.emoji(response.success, challengeNonce, origin));
        }
        if (StringUtils.isEmpty(response.image) || response.keyboard == null || response.keyboard.length == 0) {
            return new CaptchaStep.Error("Invalid emoji captcha challenge");
        }
        try {
            Bitmap image = decode(response.image);
            List<Bitmap> keyboard = new ArrayList<Bitmap>(response.keyboard.length);
            for (String key : response.keyboard) {
                keyboard.add(decode(key));
            }
            return new CaptchaStep.EmojiChallenge(new EmojiCaptcha(tokenId, challengeNonce, origin, image, keyboard));
        } catch (Exception e) {
            return new CaptchaStep.Error(errorMessage(e));
        }
    }

    private String getJson(String url) throws Exception {
        HttpGet request = new HttpGet(url);
        HttpResponse response = null;
        try {
            HttpClientParams.setRedirecting(request.getParams(), false);
            request.setHeader("Accept", "application/json");
            request.setHeader("Cache-Control", "no-cache");
            response = mHttpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalStateException("Captcha HTTP " + response.getStatusLine().getStatusCode());
            }
            return mHttpStringReader.fromResponse(response);
        } finally {
            ExtendedHttpClient.releaseRequestResponse(request, response);
        }
    }

    private String postClick(String origin, String tokenId, int emojiNumber) throws Exception {
        HttpPost request = null;
        HttpResponse response = null;
        try {
            request = new HttpPost(origin + "/api/captcha/emoji/click");
            HttpClientParams.setRedirecting(request.getParams(), false);
            request.setHeader("Content-Type", "application/json; charset=UTF-8");
            request.setEntity(new StringEntity(mJsonMapper.writeValueAsString(
                    new EmojiClickRequest(tokenId, emojiNumber)), "UTF-8"));
            response = mHttpClient.execute(request);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new IllegalStateException(response.getStatusLine().toString());
            }
            return mHttpStringReader.fromResponse(response);
        } finally {
            ExtendedHttpClient.releaseRequestResponse(request, response);
        }
    }

    private String createIdUrl(String origin, String board, String thread) {
        Uri.Builder builder = Uri.parse(origin + "/api/captcha/emoji/id")
                .buildUpon()
                .appendQueryParameter("board", board);
        if (!StringUtils.isEmpty(thread)) {
            builder.appendQueryParameter("thread", thread);
        }
        return builder.build().toString();
    }

    private CaptchaStep errorStep(ApiError error) {
        if (error != null && error.code != 0) {
            return new CaptchaStep.Error(error.getMessage());
        }
        return null;
    }

    private Bitmap decode(String base64) {
        byte[] bytes = Base64.decode(base64, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        if (bitmap == null) {
            throw new IllegalArgumentException("Invalid PNG in emoji captcha response");
        }
        return bitmap;
    }

    private String errorMessage(Exception e) {
        return !StringUtils.isEmpty(e.getMessage()) ? e.getMessage() : "Cannot load emoji captcha";
    }

    private static class EmojiClickRequest {
        public String captchaTokenID;
        public int emojiNumber;

        EmojiClickRequest(String captchaTokenID, int emojiNumber) {
            this.captchaTokenID = captchaTokenID;
            this.emojiNumber = emojiNumber;
        }
    }
}
