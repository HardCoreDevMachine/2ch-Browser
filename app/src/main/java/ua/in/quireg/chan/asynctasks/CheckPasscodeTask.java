package ua.in.quireg.chan.asynctasks;

import android.net.Uri;
import android.os.AsyncTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;

import okhttp3.MultipartBody;
import ua.in.quireg.chan.common.library.ExtendedObjectMapper;
import ua.in.quireg.chan.services.http.MultipartHttpEntity;

import ua.in.quireg.chan.boards.makaba.models.ApiError;
import ua.in.quireg.chan.boards.makaba.models.CaptchaApiResponse;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.common.library.ExtendedHttpClient;
import ua.in.quireg.chan.common.library.MyLog;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.common.utils.UriUtils;
import ua.in.quireg.chan.interfaces.ICheckPasscodeView;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.settings.ApplicationSettings;

public class CheckPasscodeTask extends AsyncTask<Void, Void, String> {
    private final ICheckPasscodeView mCheckPasscodeView;
    private final String mPasscode;
    private final DefaultHttpClient mHttpClient = Factory.resolve(DefaultHttpClient.class);
    private final ApplicationSettings mApplicationSettings = Factory.resolve(ApplicationSettings.class);
    private final IWebsite mWebsite;

    private Cookie mUserCodeCookie = null;
    private String mErrorMessage = null;

    public CheckPasscodeTask(IWebsite website, ICheckPasscodeView view) {
        this.mWebsite = website;
        this.mCheckPasscodeView = view;
        this.mPasscode = this.mApplicationSettings.getPasscodeRaw();
    }

    public CheckPasscodeTask(IWebsite website, ICheckPasscodeView view, String passcode) {
        this.mWebsite = website;
        this.mCheckPasscodeView = view;
        this.mPasscode = passcode;
    }

    @Override
    protected String doInBackground(Void... params) {
        if (StringUtils.isEmpty(mPasscode)) {
            return "removed";
        }

        HttpPost post = null;
        HttpResponse response = null;
        try {
            String url = mWebsite.getUrlBuilder().getPasscodeCheckUrl();
            post = new HttpPost(url);
            post.setEntity(new MultipartHttpEntity(new MultipartBody.Builder(Constants.MULTIPART_BOUNDARY)
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("passcode", mPasscode)
                    .build()));

            response = mHttpClient.execute(post);
            if (response.getStatusLine().getStatusCode() != 200) {
                mErrorMessage = response.getStatusLine().toString();
                return null;
            }

            CaptchaApiResponse apiResponse = new ExtendedObjectMapper().readValue(
                    org.apache.http.util.EntityUtils.toString(response.getEntity(), Constants.UTF8_CHARSET.name()),
                    CaptchaApiResponse.class);
            ApiError error = apiResponse.error;
            if (error != null && error.code != 0) {
                mErrorMessage = error.message;
                return null;
            }

            for (Cookie cookie : mHttpClient.getCookieStore().getCookies()) {
                if (cookie.getName().equals(Constants.USERCODE_NOCAPTCHA_COOKIE)
                        && UriUtils.areCookieDomainsEqual(cookie.getDomain(), Uri.parse(url).getHost())) {
                    mUserCodeCookie = cookie;
                    break;
                }
            }
            if (mUserCodeCookie == null) {
                mErrorMessage = "Authentication response does not set passcode_auth";
                return null;
            }
            return "authenticated";
        } catch (Exception e) {
            MyLog.e("CheckPasscodeTask", e);
            mErrorMessage = e.getMessage();
        } finally {
            ExtendedHttpClient.releaseRequestResponse(post, response);
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        if (StringUtils.isEmpty(this.mPasscode)) {
            this.mApplicationSettings.clearPassCodeCookie();
            this.mCheckPasscodeView.onPasscodeRemoved();
            return;
        }

        if (this.mUserCodeCookie != null) {
            this.mApplicationSettings.savePassCodeCookie(this.mUserCodeCookie);
        } else {
            this.mApplicationSettings.clearPassCodeCookie();
        }

        boolean isSuccess = "authenticated".equals(result);
        this.mCheckPasscodeView.onPasscodeChecked(isSuccess, this.mErrorMessage);
    }
}