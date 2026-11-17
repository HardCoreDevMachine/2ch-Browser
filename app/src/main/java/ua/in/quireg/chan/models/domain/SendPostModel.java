package ua.in.quireg.chan.models.domain;

import java.io.File;
import java.util.List;

public class SendPostModel {
    private final PostingCaptcha mCaptcha;
    private String mComment;
    private boolean mIsSage;
    private List<File> mAttachedFiles;
    private String mSubject;
    private String mPolitics;
    private String mName;
    private String mVideo;
    private String mParentThread;

    public SendPostModel(PostingCaptcha captcha, String comment, boolean isSage, List<File> attachedFiles, String subject, String politics, String name) {
        mCaptcha = captcha;
        mComment = comment;
        mIsSage = isSage;
        mAttachedFiles = attachedFiles;
        mSubject = subject;
        mPolitics = politics;
        mName = name;
    }

    public PostingCaptcha getCaptcha() {
        return mCaptcha;
    }

    public void setComment(String comment) {
        this.mComment = comment;
    }

    public String getComment() {
        return this.mComment;
    }

    public void setSage(boolean isSage) {
        this.mIsSage = isSage;
    }

    public boolean isSage() {
        return this.mIsSage;
    }

    public List<File> getAttachedFiles() {
        return this.mAttachedFiles;
    }

    public void setSubject(String subject) {
        this.mSubject = subject;
    }

    public String getSubject() {
        return this.mSubject;
    }

    public String getPolitics() {
        return this.mPolitics;
    }

    public void setPolitics(String politics) {
        this.mPolitics = politics;
    }

    public String getName() {
        return this.mName;
    }

    public void setName(String name) {
        this.mName = name;
    }

    public String getParentThread() {
        return mParentThread;
    }

    public void setParentThread(String parentThread) {
        this.mParentThread = parentThread;
    }
}
