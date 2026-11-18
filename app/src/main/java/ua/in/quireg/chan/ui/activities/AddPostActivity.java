package ua.in.quireg.chan.ui.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.text.Editable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.GridLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import timber.log.Timber;
import ua.in.quireg.chan.R;
import ua.in.quireg.chan.asynctasks.EmojiCaptchaTask;
import ua.in.quireg.chan.asynctasks.SendPostTask;
import ua.in.quireg.chan.common.Constants;
import ua.in.quireg.chan.common.Factory;
import ua.in.quireg.chan.common.Websites;
import ua.in.quireg.chan.common.utils.AppearanceUtils;
import ua.in.quireg.chan.common.utils.IoUtils;
import ua.in.quireg.chan.common.utils.StringUtils;
import ua.in.quireg.chan.common.utils.ThreadPostUtils;
import ua.in.quireg.chan.interfaces.ICloudflareCheckListener;
import ua.in.quireg.chan.interfaces.IPostSendView;
import ua.in.quireg.chan.interfaces.IUrlBuilder;
import ua.in.quireg.chan.interfaces.IUrlParser;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.CaptchaStep;
import ua.in.quireg.chan.models.domain.EmojiCaptcha;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;
import ua.in.quireg.chan.models.presentation.AddAttachmentViewBag;
import ua.in.quireg.chan.models.presentation.DraftPostModel;
import ua.in.quireg.chan.models.presentation.FileModel;
import ua.in.quireg.chan.models.presentation.ImageFileModel;
import ua.in.quireg.chan.models.presentation.SerializableFileModel;
import ua.in.quireg.chan.services.CloudflareCheckService;
import ua.in.quireg.chan.services.IconsList;
import ua.in.quireg.chan.services.presentation.DraftPostsStorage;
import ua.in.quireg.chan.settings.ApplicationSettings;

public class AddPostActivity extends Activity implements IPostSendView {

    private final ApplicationSettings mSettings = Factory.resolve(ApplicationSettings.class);
    private final DraftPostsStorage mDraftPostsStorage = Factory.resolve(DraftPostsStorage.class);
    private IUrlParser mUrlParser;
    private IUrlBuilder mUrlBuilder;

    private FileModel[] mAttachedFiles = new FileModel[4];
    private IWebsite mWebsite;
    private String mBoardName;
    private String mThreadNumber;
    private EmojiCaptcha mEmojiCaptcha;
    private CaptchaStep mCaptchaStep;
    private File instantPhotoTempFile;

    private SendPostTask mCurrentPostSendTask;
    private EmojiCaptchaTask mCurrentEmojiCaptchaTask;

    private TextView mCaptchaInfoView;
    private View mCaptchaLoadingView;
    private ImageView mCaptchaImageView;
    private GridLayout mCaptchaKeyboard;
    private CheckBox mSageCheckBox;
    private EditText mCommentView;
    private AddAttachmentViewBag[] mAttachmentViews = new AddAttachmentViewBag[4];
    private ProgressDialog mProgressDialog;
    private Button mSendButton;
    private EditText mSubjectView;
    private Spinner mPoliticsView;
    private boolean isPoliticsBoard;

    // Определяет, нужно ли сохранять пост (если не отправлен) или можно удалить
    // (после успешной отправки)
    private boolean mFinishedSuccessfully;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Парсим название борды и номер треда
        Bundle extras = getIntent().getExtras();
        mWebsite = Websites.fromName(extras.getString(Constants.EXTRA_WEBSITE));
        mBoardName = extras.getString(Constants.EXTRA_BOARD_NAME);
        mThreadNumber = extras.getString(Constants.EXTRA_THREAD_NUMBER);

        mUrlBuilder = mWebsite.getUrlBuilder();
        mUrlParser = mWebsite.getUrlParser();

        resetUI();

        StringBuilder commentBuilder = new StringBuilder();

        DraftPostModel draft = mDraftPostsStorage.getDraft(mBoardName, mThreadNumber);
        if (draft != null) {
            if (!StringUtils.isEmpty(draft.getComment())) {
                commentBuilder.append(draft.getComment()).append("\n");
            }

            for (FileModel file : draft.getAttachedFiles()) {
                setAttachment(file);
            }

            mSageCheckBox.setChecked(draft.isSage());
        }

        String postNumber = extras.getString(Constants.EXTRA_POST_NUMBER);
        String postComment = extras.getString(Constants.EXTRA_POST_COMMENT);
        if (postNumber != null) {
            commentBuilder.append(">>").append(postNumber).append("\n");
        }

        if (!StringUtils.isEmpty(postComment)) {
            postComment = ThreadPostUtils.removeLinksFromComment(postComment);

            postComment = postComment.replaceAll("(\n+)", "$1>");
            commentBuilder.append(">").append(postComment).append("\n");
        }

        // Сохраняем коммент
        mCommentView.setText(commentBuilder.toString());
        // Ставим курсор в конце текстового поля
        mCommentView.setSelection(commentBuilder.length());

        refreshCaptcha();

        if (StringUtils.isEmpty(mThreadNumber)) {
            setTitle(String.format(getString(R.string.data_add_thread_title), mBoardName));
            mSubjectView.setVisibility(View.VISIBLE);
        } else {
            setTitle(String.format(getString(R.string.data_add_post_title), mBoardName, mThreadNumber));
        }
    }

    @Override
    protected void onDestroy() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
        }

        super.onDestroy();
    }

    @Override
    protected void onPause() {
        Timber.v("save state");
        if (!mFinishedSuccessfully) {
            DraftPostModel draft = new DraftPostModel(mCommentView.getText().toString(), getAttachments(), mSageCheckBox.isChecked());

            mDraftPostsStorage.saveDraft(mBoardName, mThreadNumber, draft);
        } else {
            mDraftPostsStorage.clearDraft(mBoardName, mThreadNumber);
        }

        super.onPause();
    }

    private void resetUI() {
        setTheme(mSettings.getTheme());
        setContentView(R.layout.add_post_view);

        mCaptchaImageView = (ImageView) findViewById(R.id.addpost_captcha_image);
        mCaptchaLoadingView = findViewById(R.id.addpost_captcha_loading);
        mCaptchaInfoView = (TextView) findViewById(R.id.addpost_captcha_info_message);
        mCaptchaKeyboard = (GridLayout) findViewById(R.id.addpost_captcha_keyboard);
        mCommentView = (EditText) findViewById(R.id.addpost_comment_input);
        mSageCheckBox = (CheckBox) findViewById(R.id.addpost_sage_checkbox);
        mAttachmentViews[0] = AddAttachmentViewBag.fromView(findViewById(R.id.addpost_attachment_view_1));
        mAttachmentViews[1] = AddAttachmentViewBag.fromView(findViewById(R.id.addpost_attachment_view_2));
        mAttachmentViews[2] = AddAttachmentViewBag.fromView(findViewById(R.id.addpost_attachment_view_3));
        mAttachmentViews[3] = AddAttachmentViewBag.fromView(findViewById(R.id.addpost_attachment_view_4));
        mSendButton = (Button) findViewById(R.id.addpost_send_button);
        mSubjectView = (EditText) findViewById(R.id.addpost_subject);
        mPoliticsView = (Spinner) findViewById(R.id.addpost_politics);

        final ImageButton refreshCaptchaButton = (ImageButton) findViewById(R.id.addpost_refresh_button);
        final LinearLayout textFormatView = (LinearLayout) findViewById(R.id.addpost_textformat_view);

        try {
            if (Factory.resolve(IconsList.class).getData(mBoardName) != null) {
                ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, Factory.resolve(IconsList.class).getData(mBoardName));
                mPoliticsView.setAdapter(adapter);
                mPoliticsView.setVisibility(View.VISIBLE);
                isPoliticsBoard = true;
            }
        } catch (Exception e) {
            Timber.e(e);
        }

        View.OnClickListener formatButtonListener = v -> {
            switch (v.getId()) {
                case R.id.addpost_textformat_b:
                    AddPostActivity.this.formatSelectedText("b");
                    break;
                case R.id.addpost_textformat_i:
                    AddPostActivity.this.formatSelectedText("i");
                    break;
                case R.id.addpost_textformat_s:
                    AddPostActivity.this.formatSelectedText("s");
                    break;
                case R.id.addpost_textformat_spoiler:
                    AddPostActivity.this.formatSelectedText("spoiler");
                    break;
                case R.id.addpost_textformat_u:
                    AddPostActivity.this.formatSelectedText("u");
                    break;
                case R.id.addpost_textformat_quote:
                    AddPostActivity.this.formatQuote();
                    break;
            }
        };

        for (int i = 0; i < textFormatView.getChildCount(); i++) {
            ImageButton b = (ImageButton) textFormatView.getChildAt(i);
            if (b != null) {
                b.setOnClickListener(formatButtonListener);
            }
        }

        // Обрабатываем нажатие на кнопку "Отправить"
        mSendButton.setOnClickListener(v -> AddPostActivity.this.onSend());

        // Удаляем прикрепленный файл
        for (int i = 0; i < mAttachmentViews.length; i++) {
            final int index = i;
            mAttachmentViews[i].removeButton.setOnClickListener(v -> AddPostActivity.this.removeAttachment(index));
        }
        // Обновляем нажатие кнопки Refresh для капчи
        refreshCaptchaButton.setOnClickListener(v -> AddPostActivity.this.refreshCaptcha());
    }

    private void onSend() {
        PostingCaptcha captcha = getPostingCaptcha();
        if (captcha == null) {
            AppearanceUtils.showLongToast(this, getString(R.string.addpost_captcha_solve_emoji));
            return;
        }

        String comment = mCommentView.getText().toString();
        if (StringUtils.isEmpty(comment) && !hasAttachments()) {
            AppearanceUtils.showLongToast(this, getString(R.string.warning_write_comment));
            return;
        }

        if (StringUtils.isEmpty(mThreadNumber) && !hasAttachments() && !mBoardName.equals("d")) {
            AppearanceUtils.showLongToast(this, getString(R.string.warning_attach_file_new_thread));
            return;
        }

        String subject = StringUtils.nullIfEmpty(mSubjectView.getText().toString());
        String politics = null;
        if (isPoliticsBoard) {
            int itemIndex = mPoliticsView.getSelectedItemPosition();
            politics = Math.max(itemIndex, 0) + "";
        }

        SendPostModel post = new SendPostModel(captcha, comment, mSageCheckBox.isChecked(), getAttachedFiles(), subject, politics, mSettings.getName());
        post.setParentThread(mThreadNumber);
        sendPost(post);
    }


    private void sendPost(SendPostModel post) {
        if (mCurrentPostSendTask != null) {
            mCurrentPostSendTask.cancel(true);
        }
        mCurrentPostSendTask = new SendPostTask(this, mWebsite, mBoardName, mThreadNumber, post);
        mCurrentPostSendTask.execute();
    }

    @Override
    public void showSuccess(String redirectedPage) {
        AppearanceUtils.showLongToast(this, getString(R.string.notification_send_post_success));
        // return back to the list of posts
        String redirectedThreadNumber = null;
        if (redirectedPage != null) {
            redirectedThreadNumber = mUrlParser.getThreadNumber(Uri.parse(redirectedPage));
        }

        mFinishedSuccessfully = true;

        // Завершаем с успешным результатом
        Intent intent = new Intent();
        intent.putExtra(Constants.EXTRA_REDIRECTED_THREAD_NUMBER, redirectedThreadNumber);
        setResult(RESULT_OK, intent);
        finish();
    }

    @Override
    public void showError(String error, boolean isRecaptcha, int errorCode) {
        error = error != null ? error : getString(R.string.error_send_post);
        AppearanceUtils.showLongToast(this, error);

        if (errorCode == -5) {
            refreshCaptcha();
        }

        if (isRecaptcha || error.startsWith("503")) {
            startCloudflareCheck();
        }
    }

    private void startCloudflareCheck() {
        new CloudflareCheckService(mUrlBuilder.getPostingUrlHtml(), this, new ICloudflareCheckListener() {
            public void onSuccess() {
                refreshCaptcha();
                AppearanceUtils.showLongToast(AddPostActivity.this, getString(R.string.notification_cloudflare_check_finished));
            }

            public void onStart() {
                AppearanceUtils.showLongToast(AddPostActivity.this, getString(R.string.notification_cloudflare_check_started));
            }

            public void onTimeout() {
                AppearanceUtils.showLongToast(AddPostActivity.this, getString(R.string.error_cloudflare_check_timeout));
            }
        }).start();
    }

    @Override
    public void showPostLoading() {
        mSendButton.setEnabled(false);
        mProgressDialog = new ProgressDialog(this);
        mProgressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        mProgressDialog.setMessage(getString(R.string.loading));
        mProgressDialog.setCancelable(true);
        mProgressDialog.setCanceledOnTouchOutside(false);
        mProgressDialog.show();
    }

    @Override
    public void hidePostLoading() {
        mSendButton.setEnabled(true);
        mProgressDialog.dismiss();
        mCurrentPostSendTask = null;
    }

    private void showCaptchaLoading() {
        mCaptchaStep = null;
        mEmojiCaptcha = null;
        mCaptchaImageView.setVisibility(View.GONE);
        mCaptchaKeyboard.setVisibility(View.GONE);
        mCaptchaInfoView.setVisibility(View.GONE);
        mCaptchaLoadingView.setVisibility(View.VISIBLE);
    }

    private void showCaptchaStep(CaptchaStep step) {
        mCurrentEmojiCaptchaTask = null;
        mCaptchaStep = step;
        mCaptchaLoadingView.setVisibility(View.GONE);

        if (step instanceof CaptchaStep.EmojiChallenge) {
            showEmojiChallenge(((CaptchaStep.EmojiChallenge) step).getCaptcha());
        } else if (step instanceof CaptchaStep.Ready) {
            showCaptchaReady(((CaptchaStep.Ready) step).getCaptcha());
        } else {
            showCaptchaFailure(((CaptchaStep.Error) step).getMessage());
        }
    }

    private void showEmojiChallenge(EmojiCaptcha captcha) {
        mEmojiCaptcha = captcha;
        mCaptchaImageView.setImageBitmap(captcha.getImage());
        mCaptchaImageView.setVisibility(View.VISIBLE);
        mCaptchaInfoView.setVisibility(View.GONE);
        mCaptchaKeyboard.removeAllViews();
        for (int i = 0; i < captcha.getKeyboard().size(); i++) {
            final int emojiNumber = i;
            ImageButton button = new ImageButton(this);
            button.setImageBitmap(captcha.getKeyboard().get(i));
            button.setAdjustViewBounds(true);
            button.setContentDescription(getString(R.string.addpost_captcha_emoji_number, i + 1));
            button.setOnClickListener(v -> selectEmoji(emojiNumber));

            GridLayout.LayoutParams layoutParams = new GridLayout.LayoutParams(
                    GridLayout.spec(GridLayout.UNDEFINED),
                    GridLayout.spec(GridLayout.UNDEFINED, 1f));
            layoutParams.width = 0;
            layoutParams.height = GridLayout.LayoutParams.WRAP_CONTENT;
            button.setLayoutParams(layoutParams);
            mCaptchaKeyboard.addView(button);
        }
        mCaptchaKeyboard.setVisibility(View.VISIBLE);
    }

    private void showCaptchaReady(PostingCaptcha captcha) {
        mEmojiCaptcha = null;
        mCaptchaImageView.setVisibility(View.GONE);
        mCaptchaKeyboard.setVisibility(View.GONE);
        int message;
        switch (captcha.getType()) {
            case EMOJI_CAPTCHA:
                message = R.string.addpost_captcha_solved;
                break;
            case PASSCODE:
                message = R.string.addpost_captcha_can_skip_passcode;
                break;
            default:
                message = R.string.addpost_captcha_can_skip;
                break;
        }
        mCaptchaInfoView.setText(message);
        mCaptchaInfoView.setVisibility(View.VISIBLE);
    }

    private void showCaptchaFailure(String message) {
        mEmojiCaptcha = null;
        mCaptchaImageView.setVisibility(View.GONE);
        mCaptchaKeyboard.setVisibility(View.GONE);
        mCaptchaInfoView.setText(!StringUtils.isEmpty(message) ? message : getString(R.string.error_read_response));
        mCaptchaInfoView.setVisibility(View.VISIBLE);
    }

    private void selectEmoji(int emojiNumber) {
        if (mEmojiCaptcha == null) {
            return;
        }
        for (int i = 0; i < mCaptchaKeyboard.getChildCount(); i++) {
            mCaptchaKeyboard.getChildAt(i).setEnabled(false);
        }
        if (mCurrentEmojiCaptchaTask != null) {
            mCurrentEmojiCaptchaTask.cancel(true);
        }
        mCurrentEmojiCaptchaTask = new EmojiCaptchaTask(mWebsite, mEmojiCaptcha.getTokenId(), emojiNumber, this::showCaptchaStep);
        mCurrentEmojiCaptchaTask.execute();
    }

    private PostingCaptcha getPostingCaptcha() {
        return mCaptchaStep instanceof CaptchaStep.Ready
                ? ((CaptchaStep.Ready) mCaptchaStep).getCaptcha()
                : null;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.addpost, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_attach_file_id:
                if (getAttachments().size() >= ThreadPostUtils.getMaximumAttachments(mBoardName)) {
                    AppearanceUtils.showLongToast(this, getString(R.string.warning_maximum_attachments));
                    break;
                }

                Intent intent = new Intent(this, FilesListActivity.class);
                if (hasAttachments()) {
                    intent.putExtra(FilesListActivity.EXTRA_CURRENT_FILE, getAttachments().get(0).file.getAbsolutePath());
                }
                startActivityForResult(intent, Constants.REQUEST_CODE_FILE_LIST_ACTIVITY);
                break;
            case R.id.menu_gallery_id:
                if (getAttachments().size() >= ThreadPostUtils.getMaximumAttachments(mBoardName)) {
                    AppearanceUtils.showLongToast(this, getString(R.string.warning_maximum_attachments));
                    break;
                }

                Intent i = new Intent(Intent.ACTION_GET_CONTENT);
                i.setType("image/*");
                startActivityForResult(i, Constants.REQUEST_CODE_GALLERY);
                break;
            case R.id.menu_attach_instant_photo:
                if (getAttachments().size() >= ThreadPostUtils.getMaximumAttachments(mBoardName)) {
                    AppearanceUtils.showLongToast(this, getString(R.string.warning_maximum_attachments));
                    break;
                }

                Intent instantPhoto = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                if (instantPhoto.resolveActivity(getPackageManager()) != null) {
                    // Create the File where the photo should go
                    try {
                        instantPhotoTempFile = createImageFile();
                    } catch (IOException ex) {
                        // Error occurred while creating the File
                        Timber.e(ex);
                    }
                    // Continue only if the File was successfully created
                    if (instantPhotoTempFile != null) {
                        Uri photoURI = FileProvider.getUriForFile(this,
                                "ua.in.quireg.chan.fileprovider",
                                instantPhotoTempFile);
                        instantPhoto.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                        startActivityForResult(instantPhoto, Constants.REQUEST_CODE_INSTANT_PHOTO);
                    }
                }
                break;
            case R.id.menu_attach_video_id:
                if (getAttachments().size() >= ThreadPostUtils.getMaximumAttachments(mBoardName)) {
                    AppearanceUtils.showLongToast(this, getString(R.string.warning_maximum_attachments));
                    break;
                }

                Intent videoIntent = new Intent(Intent.ACTION_GET_CONTENT);
                videoIntent.setType("video/*");
                startActivityForResult(videoIntent, Constants.REQUEST_CODE_VIDEO_FILE);
                break;
        }

        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case Constants.REQUEST_CODE_FILE_LIST_ACTIVITY:

                    SerializableFileModel fileModel = data.getParcelableExtra(getPackageName() + Constants.EXTRA_SELECTED_FILE);

                    setAttachment(fileModel);

                    break;
                case Constants.REQUEST_CODE_GALLERY:
                    Uri imageUri = data.getData();
                    File imageFile = IoUtils.getFile(this, imageUri);
                    if (imageFile != null) {
                        ImageFileModel image = new ImageFileModel(imageFile);
                        setAttachment(image);
                    } else {
                        AppearanceUtils.showLongToast(this, getString(R.string.error_image_cannot_be_attached));
                    }

                    break;
                case Constants.REQUEST_CODE_INSTANT_PHOTO:
                    if (instantPhotoTempFile != null) {
                        //TODO Remove exif from image file.
                        ImageFileModel image = new ImageFileModel(instantPhotoTempFile);
                        setAttachment(image);
                    } else {
                        AppearanceUtils.showLongToast(this, getString(R.string.error_image_cannot_be_attached));
                    }

                    break;
                case Constants.REQUEST_CODE_VIDEO_FILE:
                    Uri videoUri = data.getData();
                    File videoFile = IoUtils.getFile(this, videoUri);
                    if (videoFile != null) {
                        FileModel attachmentModel = new FileModel(videoFile);
                        setAttachment(attachmentModel);
                    } else {
                        AppearanceUtils.showLongToast(this, getString(R.string.error_image_cannot_be_attached));
                    }
            }
        }
    }

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";


        File storageDir = getApplication().getApplicationContext().getExternalFilesDir(
                Environment.DIRECTORY_PICTURES);
        instantPhotoTempFile = File.createTempFile(
                imageFileName,  // prefix
                ".jpg",         // suffix
                storageDir      // directory
        );
        return instantPhotoTempFile;
    }

    private void setAttachment(FileModel fileModel) {
        int index = -1;
        for (int i = 0; i < mAttachedFiles.length; i++) {
            if (mAttachedFiles[i] == null) {
                index = i;
                break;
            }
        }

        if (index == -1) {
            AppearanceUtils.showLongToast(this, getString(R.string.error_image_cannot_be_attached));
            return;
        }

        mAttachedFiles[index] = fileModel;
        mAttachmentViews[index].show(fileModel, getResources());
    }

    private void removeAttachment(int index) {
        mAttachedFiles[index] = null;
        mAttachmentViews[index].hide();
    }

    private boolean hasAttachments() {
        return getAttachments().size() > 0;
    }

    private List<FileModel> getAttachments() {
        ArrayList<FileModel> attachments = new ArrayList<FileModel>(4);
        for (FileModel file : mAttachedFiles) {
            if (file != null) {
                attachments.add(file);
            }
        }

        return attachments;
    }

    private List<File> getAttachedFiles() {
        ArrayList<File> files = new ArrayList<File>(4);
        for (FileModel file : getAttachments()) {
            files.add(file.file);
        }

        return files;
    }

    private void refreshCaptcha() {
        if (mCurrentEmojiCaptchaTask != null) {
            mCurrentEmojiCaptchaTask.cancel(true);
        }
        showCaptchaLoading();
        mCurrentEmojiCaptchaTask = new EmojiCaptchaTask(mWebsite, mBoardName, mThreadNumber, this::showCaptchaStep);
        mCurrentEmojiCaptchaTask.execute();
    }



    private void formatSelectedText(String code) {
        Editable editable = mCommentView.getEditableText();
        String text = editable.toString();

        String startTag = "[" + code + "]";
        String endTag = "[/" + code + "]";

        int selectionStart = Math.max(0, mCommentView.getSelectionStart());
        int selectionEnd = Math.min(text.length(), mCommentView.getSelectionEnd());
        if (selectionStart < 0 || selectionEnd > text.length() || selectionStart > selectionEnd) {
            return;
        }

        String selectedText = text.substring(selectionStart, selectionEnd);

        // Проверяем текст на краях выделенной области, на случай если уже была
        // добавлена разметка
        String textBeforeSelection = text.substring(Math.max(0, selectionStart - startTag.length()), selectionStart);
        String textAfterSelection = text.substring(selectionEnd, Math.min(text.length(), selectionEnd + endTag.length()));

        // Удаляем теги форматирования если есть, добавляем если нет
        if (textBeforeSelection.equalsIgnoreCase(startTag) && textAfterSelection.equalsIgnoreCase(endTag)) {
            editable.replace(selectionStart - startTag.length(), selectionEnd + endTag.length(), selectedText);
            mCommentView.setSelection(selectionStart - startTag.length(), selectionEnd - startTag.length());
        } else {
            editable.replace(selectionStart, selectionEnd, startTag + selectedText + endTag);
            mCommentView.setSelection(selectionStart + startTag.length(), selectionEnd + startTag.length());
        }
    }

    private void formatQuote() {
        Editable editable = mCommentView.getEditableText();
        String text = editable.toString();

        int selectionStart = mCommentView.getSelectionStart();
        int selectionEnd = mCommentView.getSelectionEnd();
        String selectedText = text.substring(selectionStart, selectionEnd);
        String oneSymbolBefore = text.substring(Math.max(selectionStart - 1, 0), selectionStart);

        if (selectedText.startsWith(">")) {
            String unQuotedText = selectedText.replaceFirst(">", "").replaceAll("(\n+)>", "$1");
            int diff = selectedText.length() - unQuotedText.length();

            editable.replace(selectionStart, selectionEnd, unQuotedText);
            mCommentView.setSelection(selectionStart, selectionEnd - diff);
        } else {
            String firstSymbol = oneSymbolBefore.length() == 0 || oneSymbolBefore.equals("\n") ? "" : "\n";
            String quotedText = firstSymbol + ">" + selectedText.replaceAll("(\n+)", "$1>");
            int diff = quotedText.length() - selectedText.length();

            editable.replace(selectionStart, selectionEnd, quotedText);
            mCommentView.setSelection(selectionStart + firstSymbol.length(), selectionEnd + diff);
        }
    }

}
