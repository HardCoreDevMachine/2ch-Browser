package ua.in.quireg.chan.services;

import ua.in.quireg.chan.common.library.ExtendedObjectMapper;

import ua.in.quireg.chan.boards.makaba.models.ApiError;
import ua.in.quireg.chan.boards.makaba.models.PostingResponse;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.models.domain.SendPostResult;

public class PostResponseParser {
    private final ExtendedObjectMapper mMapper = new ExtendedObjectMapper();

    public SendPostResult isPostSuccessful(String boardName, String responseText) {
        SendPostResult result = new SendPostResult();
        try {
            PostingResponse response = mMapper.readValue(responseText, PostingResponse.class);
            if (response == null) {
                result.error = "Сервер не подтвердил отправку: пустой ответ.";
                return result;
            }
            ApiError error = response.error;
            if (error != null && error.code != 0) {
                result.error = StringUtils.isEmpty(error.message)
                        ? "Ошибка отправки: " + error.code
                        : error.message;
                result.errorCode = error.code;
                return result;
            }
            if (response.result != 1 || (response.num <= 0 && response.thread <= 0)) {
                result.error = "Сервер не подтвердил отправку: result=" + response.result
                        + ", num=" + response.num + ", thread=" + response.thread + ".";
                return result;
            }
            result.isSuccess = true;
        } catch (Exception e) {
            result.error = !StringUtils.isEmpty(e.getMessage()) ? e.getMessage() : "Invalid posting response";
        }
        return result;
    }
}
