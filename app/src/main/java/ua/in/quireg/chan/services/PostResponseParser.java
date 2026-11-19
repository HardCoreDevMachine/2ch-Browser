package ua.in.quireg.chan.services;

import ua.in.quireg.chan.common.library.ExtendedObjectMapper;

import ua.in.quireg.chan.boards.makaba.models.ApiError;
import ua.in.quireg.chan.boards.makaba.models.PostReadbackResponse;
import ua.in.quireg.chan.boards.makaba.models.PostingResponse;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.domain.SendPostResult;

public class PostResponseParser {
    private final ExtendedObjectMapper mMapper = new ExtendedObjectMapper();

    /**
     * Parses the POST /posting response. A positive acknowledgement requires
     * result==1 and the positive identifier of the scenario: for a reply
     * (positive parentThread) num&gt;0, for a new thread thread&gt;0.
     * An explicit error.code!=0 rejects the result even if success fields are present.
     * Never copies the raw response body into error messages.
     */
    public SendPostResult parsePostingResponse(String boardName, String parentThread, String responseText) {
        SendPostResult result = new SendPostResult();
        result.board = boardName;

        int parent = parseThreadNumber(parentThread);
        boolean isReply = parent > 0;
        if (isReply) {
            result.threadNumber = parent;
        }

        PostingResponse response;
        try {
            response = mMapper.readValue(responseText, PostingResponse.class);
        } catch (Exception e) {
            result.error = "Сервер не подтвердил отправку: некорректный ответ.";
            return result;
        }
        if (response == null) {
            result.error = "Сервер не подтвердил отправку: пустой ответ.";
            return result;
        }
        result.apiResult = response.result;

        ApiError error = response.error;
        if (error != null && error.code != 0) {
            result.state = SendPostResult.State.REJECTED;
            result.errorCode = error.code;
            result.error = error.getMessage();
            return result;
        }

        if (response.result != 1) {
            result.error = "Сервер не подтвердил отправку (result=" + response.result + ").";
            return result;
        }
        if (isReply) {
            if (response.num <= 0) {
                result.error = "Сервер не подтвердил отправку: ответ не содержит номера поста.";
                return result;
            }
            if (response.thread > 0 && response.thread != parent) {
                result.error = "Сервер не подтвердил отправку: противоречивый ответ.";
                return result;
            }
            result.postNumber = response.num;
        } else {
            if (response.thread <= 0) {
                result.error = "Сервер не подтвердил отправку: ответ не содержит номера треда.";
                return result;
            }
            if (response.num > 0 && response.num != response.thread) {
                result.error = "Сервер не подтвердил отправку: противоречивый ответ.";
                return result;
            }
            result.threadNumber = response.thread;
            result.postNumber = response.thread;
        }

        result.state = SendPostResult.State.ACCEPTED;
        return result;
    }

    /**
     * Applies the GET /api/mobile/v2/post/{board}/{num} readback to an ACCEPTED
     * result. Sets VERIFIED only when result==1, there is no nonzero error and
     * the returned post matches the requested num, board and parent (thread for
     * a reply, 0 for an OP). Otherwise keeps ACCEPTED with the creation ids and
     * records verification metadata plus an explanatory error.
     * Never copies the raw response body into error messages.
     */
    public void applyVerification(SendPostResult result, String responseText) {
        if (result == null || !result.isAccepted()) {
            return;
        }
        PostReadbackResponse readback;
        try {
            readback = mMapper.readValue(responseText, PostReadbackResponse.class);
        } catch (Exception e) {
            result.error = "Пост отправлен, но ответ проверки некорректен.";
            return;
        }
        if (readback == null) {
            result.error = "Пост отправлен, но ответ проверки пуст.";
            return;
        }
        result.verificationResult = readback.result;
        ApiError error = readback.error;
        if (error != null) {
            result.verificationErrorCode = error.code;
        }

        if (error != null && error.code != 0) {
            result.error = "Пост отправлен, но проверка вернула ошибку: " + error.getMessage();
            return;
        }
        if (readback.result != 1) {
            result.error = "Пост отправлен, но проверка не подтвердила его (result=" + readback.result + ").";
            return;
        }
        if (readback.post == null) {
            result.error = "Пост отправлен, но ответ проверки не содержит пост.";
            return;
        }
        if (!matchesRequestedPost(result, readback.post)) {
            result.error = "Пост отправлен, но при проверке получен другой пост.";
            return;
        }

        result.state = SendPostResult.State.VERIFIED;
        result.error = null;
    }

    private static boolean matchesRequestedPost(SendPostResult result, PostReadbackResponse.Post post) {
        if (post.num == null || post.parent == null || StringUtils.isEmpty(post.board)) {
            return false;
        }
        if (post.num != result.postNumber || !post.board.equals(result.board)) {
            return false;
        }
        int expectedParent = result.postNumber == result.threadNumber ? 0 : result.threadNumber;
        return post.parent == expectedParent;
    }

    private static int parseThreadNumber(String parentThread) {
        if (StringUtils.isEmpty(parentThread)) {
            return 0;
        }
        try {
            return Integer.parseInt(parentThread.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
