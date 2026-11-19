package ua.in.quireg.chan.asynctasks;

import android.os.AsyncTask;

import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.interfaces.IPostSendView;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.SendPostModel;
import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.PostSender;

public class SendPostTask extends AsyncTask<Void, Long, SendPostResult> {

    private final PostSender mPostSender = Factory.resolve(PostSender.class);
    private final IPostSendView mView;

    private final IWebsite mWebsite;
    private final String mBoardName;
    private final SendPostModel mEntity;

    public SendPostTask(IPostSendView view, IWebsite website, String boardName, SendPostModel entity) {
        this.mView = view;
        this.mWebsite = website;
        this.mBoardName = boardName;
        this.mEntity = entity;
    }

    @Override
    protected SendPostResult doInBackground(Void... args) {
        return this.mPostSender.sendPost(this.mWebsite, this.mBoardName, this.mEntity);
    }

    @Override
    public void onPreExecute() {
        this.mView.showPostLoading();
    }

    @Override
    protected void onPostExecute(final SendPostResult result) {
        this.mView.hidePostLoading();
        this.mView.showPostResult(result);
    }
}
