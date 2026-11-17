package ua.in.quireg.chan.interfaces;

public interface IPostSendView {

    void showSuccess(String redirectedPage);

    void showError(String error, boolean isRecaptcha, int errorCode);

    void showPostLoading();

    void hidePostLoading();
}
