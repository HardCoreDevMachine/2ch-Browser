package ua.in.quireg.chan.models.presentation;

import java.util.List;
public class DraftPostModel {
    private String mComment;
    private List<FileModel> mAttachedFiles;
    private boolean mIsSage;

    public DraftPostModel(String comment, List<FileModel> attachedFiles, boolean isSage) {
        mComment = comment;
        mAttachedFiles = attachedFiles;
        mIsSage = isSage;
    }

    public String getComment() {
        return this.mComment;
    }

    public List<FileModel> getAttachedFiles() {
        return this.mAttachedFiles;
    }

    public boolean isSage() {
        return this.mIsSage;
    }

}
