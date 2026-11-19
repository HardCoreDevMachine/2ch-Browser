package ua.in.quireg.chan.interfaces;

import ua.in.quireg.chan.models.domain.SendPostResult;

public interface IPostSendView {

    void showPostResult(SendPostResult result);

    void showPostLoading();

    void hidePostLoading();
}
