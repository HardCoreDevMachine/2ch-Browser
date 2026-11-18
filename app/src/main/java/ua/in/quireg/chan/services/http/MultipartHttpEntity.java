package ua.in.quireg.chan.services.http;

import org.apache.http.entity.AbstractHttpEntity;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.MultipartBody;
import okio.Buffer;
import okio.BufferedSink;
import okio.Okio;

/**
 * Exposes an OkHttp multipart body to the platform Apache HTTP client.
 * httpmime cannot be used: it requires httpcore 4.3+, while Android resolves
 * org.apache.http classes from the legacy 4.0 framework library.
 */
public class MultipartHttpEntity extends AbstractHttpEntity {
    private final MultipartBody mBody;

    public MultipartHttpEntity(MultipartBody body) {
        mBody = body;
        setContentType(body.contentType().toString());
    }

    @Override
    public boolean isRepeatable() {
        return true;
    }

    @Override
    public long getContentLength() {
        try {
            return mBody.contentLength();
        } catch (IOException e) {
            return -1;
        }
    }

    @Override
    public InputStream getContent() throws IOException {
        Buffer buffer = new Buffer();
        mBody.writeTo(buffer);
        return buffer.inputStream();
    }

    @Override
    public void writeTo(OutputStream outputStream) throws IOException {
        BufferedSink sink = Okio.buffer(Okio.sink(outputStream));
        mBody.writeTo(sink);
        sink.flush();
    }

    @Override
    public boolean isStreaming() {
        return false;
    }
}
