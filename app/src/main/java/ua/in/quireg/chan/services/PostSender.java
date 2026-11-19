package ua.in.quireg.chan.services;

import java.net.URI;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpRequestRetryHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.cookie.Cookie;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.HttpContext;

import ua.in.quireg.chan.boards.makaba.MakabaSendPostMapper;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.library.ExtendedHttpClient;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.SendPostModel;
import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.http.HttpStringReader;
import ua.in.quireg.chan.settings.ApplicationSettings;

public class PostSender {
    private static final String NO_RETRY = "2ch.posting.no-retry";
    private final DefaultHttpClient mHttpClient;
    private final HttpStringReader mHttpStringReader;
    private final ApplicationSettings mApplicationSettings;
    private final PostResponseParser mResponseParser = new PostResponseParser();
    private final MakabaSendPostMapper mMapper = new MakabaSendPostMapper();

    public PostSender(DefaultHttpClient client, HttpStringReader reader, ApplicationSettings settings) {
        mHttpClient = client;
        mHttpStringReader = reader;
        mApplicationSettings = settings;
        // The legacy client can also retry internally. Disable that only for this workflow.
        HttpRequestRetryHandler previous = client.getHttpRequestRetryHandler();
        client.setHttpRequestRetryHandler((exception, count, context) ->
                !Boolean.TRUE.equals(context.getAttribute(NO_RETRY))
                        && previous.retryRequest(exception, count, context));
    }

    public SendPostResult sendPost(IWebsite website, String boardName, SendPostModel model) {
        SendPostResult result = new SendPostResult();
        result.board = boardName;
        HttpPost request = null;
        HttpResponse response = null;
        URI postingUri = null;
        boolean attempted = false;
        try {
            // Capture the origin once: the read-back must not follow a domain setting change.
            postingUri = new URI(website.getUrlBuilder().getPostingUrlApi());
            result.postingEndpoint = new URI(postingUri.getScheme(), null, postingUri.getHost(),
                    postingUri.getPort(), postingUri.getPath(), "nc=1", null).toASCIIString();
            String parent = StringUtils.nullIfEmpty(model.getParentThread());
            if (parent != null) {
                result.threadNumber = Integer.parseInt(parent);
            }
            String origin = new URI(postingUri.getScheme(), null, postingUri.getHost(),
                    postingUri.getPort(), null, null, null).toASCIIString();
            if (!origin.equals(model.getCaptcha().getOrigin())) {
                result.state = SendPostResult.State.REJECTED;
                result.error = "Домен изменился после загрузки капчи. Обновите капчу перед отправкой.";
                return result;
            }
            String refererPath = "/" + boardName + (parent == null ? "/" : "/res/" + parent + ".html");
            String referer = postingUri.resolve(new URI(null, null, refererPath, null)).toASCIIString();
            request = new HttpPost(postingUri);
            HttpClientParams.setRedirecting(request.getParams(), false);
            request.setHeader("Accept", "application/json, text/javascript, */*; q=0.01");
            request.setHeader("X-Requested-With", "XMLHttpRequest");
            request.setHeader("Origin", origin);
            request.setHeader("Referer", referer);
            HttpEntity body = mMapper.mapModelToHttpEntity(boardName, model);
            request.setEntity(body);
            request.setHeader(body.getContentType());
            attempted = true;
            response = mHttpClient.execute(request, noRetryContext());
            result.httpStatus = response.getStatusLine().getStatusCode();
            String responseText = mHttpStringReader.fromResponse(response);
            SendPostResult parsed = mResponseParser.parsePostingResponse(boardName, parent, responseText);
            if (result.httpStatus == 200 || parsed.state == SendPostResult.State.REJECTED) {
                parsed.postingEndpoint = result.postingEndpoint;
                parsed.httpStatus = result.httpStatus;
                result = parsed;
            } else {
                result.apiResult = parsed.apiResult;
                result.error = "Нет подтверждения отправки: HTTP " + result.httpStatus
                        + ". Проверьте тред перед повторной отправкой.";
                result.requiresCloudflare = result.httpStatus == 403 && responseText != null
                        && RecaptchaService.isCloudflareCaptchaPage(responseText);
            }
            for (Cookie cookie : mHttpClient.getCookieStore().getCookies()) {
                String domain = cookie.getDomain();
                if (domain != null && domain.startsWith(".")) {
                    domain = domain.substring(1);
                }
                if (Constants.ADULT_ACCESS_COOKIE.equals(cookie.getName())
                        && postingUri.getHost().equalsIgnoreCase(domain)) {
                    mApplicationSettings.saveAdultAccessCookie(cookie);
                    break;
                }
            }
        } catch (Exception e) {
            // Do not copy exception text: it may contain the request URL, body or credentials.
            result.error = attempted
                    ? "Ответ на отправку не получен. Пост мог быть создан; проверьте тред перед повтором."
                    : "Не удалось подготовить запрос. Пост не отправлялся.";
            if (!attempted) {
                result.state = SendPostResult.State.REJECTED;
            }
        } finally {
            ExtendedHttpClient.releaseRequestResponse(request, response);
        }
        if (result.isAccepted()) {
            verifyPost(postingUri, result);
        }
        return result;
    }

    private void verifyPost(URI postingUri, SendPostResult result) {
        HttpGet request = null;
        HttpResponse response = null;
        try {
            URI uri = postingUri.resolve(new URI(null, null,
                    "/api/mobile/v2/post/" + result.board + "/" + result.postNumber, null));
            result.verificationEndpoint = new URI(uri.getScheme(), null, uri.getHost(),
                    uri.getPort(), uri.getPath(), null, null).toASCIIString();
            request = new HttpGet(uri);
            HttpClientParams.setRedirecting(request.getParams(), false);
            request.setHeader("Accept", "application/json");
            request.setHeader("Cache-Control", "no-cache");
            request.setHeader("Pragma", "no-cache");
            response = mHttpClient.execute(request, noRetryContext());
            result.verificationHttpStatus = response.getStatusLine().getStatusCode();
            if (result.verificationHttpStatus == 200) {
                mResponseParser.applyVerification(result, mHttpStringReader.fromResponse(response));
            } else {
                result.error = "Создание подтверждено, но чтение поста вернуло HTTP "
                        + result.verificationHttpStatus + ". Повторно отправлять пост не нужно.";
            }
        } catch (Exception e) {
            result.error = "Создание подтверждено, но проверить пост чтением не удалось."
                    + " Повторно отправлять пост не нужно.";
        } finally {
            ExtendedHttpClient.releaseRequestResponse(request, response);
        }
    }

    private HttpContext noRetryContext() {
        HttpContext context = new BasicHttpContext();
        context.setAttribute(NO_RETRY, true);
        return context;
    }
}
