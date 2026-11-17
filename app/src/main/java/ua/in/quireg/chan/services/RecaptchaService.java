package ua.in.quireg.chan.services;

import android.net.Uri;

import java.util.regex.Pattern;

import org.apache.http.Header;
import org.apache.http.message.BasicHeader;

import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.common.utils.RegexUtils;
import ua.in.quireg.chan.exceptions.HttpRequestException;
import ua.in.quireg.chan.models.domain.CaptchaEntity;
import ua.in.quireg.chan.services.http.HttpStringReader;
import ua.in.quireg.chan.settings.ApplicationSettings;

public class RecaptchaService {
    private static final String CLOUDFLARE_CHECK_KEY = "6LeT6gcAAAAAAAZ_yDmTMqPH57dJQZdQcu6VFqog";
    private static final String RECAPTCHA_CHALLENGE_URI = "http://www.google.com/recaptcha/api/challenge?k=";
    private static final String IMAGE_URI = "http://www.google.com/recaptcha/api/image?c=";
    private static final String RELOAD_URI = "http://www.google.com/recaptcha/api/reload?type=image&c=%s&k=%s";

    private static final Pattern jsChallengePattern = Pattern.compile("challenge.?:.?'([\\w-]+)'");
    private static final Pattern jsReloadChallengePattern = Pattern.compile("Recaptcha.finish_reload.'(.*?)'.*");


    public static boolean isCloudflareCaptchaPage(String html) {
        return html.contains(CLOUDFLARE_CHECK_KEY);
    }

    public static CaptchaEntity loadCloudflareCaptcha() {
        Uri refererUri = Factory.resolve(ApplicationSettings.class).getDomainUri();
        return loadRecaptcha(CLOUDFLARE_CHECK_KEY, refererUri.toString());
    }


    public static CaptchaEntity loadRecaptcha(String key, String referer) {
        HttpStringReader httpReader = Factory.resolve(HttpStringReader.class);
        Header[] extraHeaders = referer != null ? new Header[] { new BasicHeader("Referer", referer) } : null;

        try {
            // Download the first challenge id
            String originalResponse = httpReader.fromUri(RECAPTCHA_CHALLENGE_URI + key, extraHeaders);
            String originalChallenge = RegexUtils.getGroupValue(originalResponse, jsChallengePattern, 1);
            if (originalChallenge == null) {
                return null;
            }

            // Download the final challenge id based on the first one
            String reloadUri = String.format(RELOAD_URI, originalChallenge, key);
            String reloadResponse = httpReader.fromUri(reloadUri, extraHeaders);
            String reloadChallenge = RegexUtils.getGroupValue(reloadResponse, jsReloadChallengePattern, 1);
            if (reloadChallenge == null) {
                return null;
            }

            CaptchaEntity captcha = new CaptchaEntity();
            captcha.setKey(reloadChallenge);
            captcha.setUrl(IMAGE_URI + reloadChallenge);
            return captcha;
        } catch (HttpRequestException e) {
            return null;
        }
    }

}
