package ua.in.quireg.chan.test;

import junit.framework.TestCase;

import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.HttpConnectionParams;
import org.codehaus.jackson.JsonNode;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import ua.in.quireg.chan.common.library.ExtendedObjectMapper;
import ua.in.quireg.chan.interfaces.IUrlBuilder;
import ua.in.quireg.chan.interfaces.IUrlParser;
import ua.in.quireg.chan.interfaces.IWebsite;
import ua.in.quireg.chan.models.domain.PostingCaptcha;
import ua.in.quireg.chan.models.domain.SendPostModel;
import ua.in.quireg.chan.models.domain.SendPostResult;
import ua.in.quireg.chan.services.PostSender;
import ua.in.quireg.chan.services.http.HttpBytesReader;
import ua.in.quireg.chan.services.http.HttpStreamReader;
import ua.in.quireg.chan.services.http.HttpStringReader;

public class PostSenderIntegrationTest extends TestCase {
    private static final String ACK = "{\"num\":256354,\"result\":1}";
    private static final String FOUND = "{\"result\":1,\"post\":{\"num\":256354,\"parent\":251491,\"board\":\"test\"}}";

    public void testHarRequestAndReadBackWithSharedCookies() throws Exception {
        JsonNode fixture;
        try (InputStream input = getClass().getResourceAsStream("/posting/har-success.json")) {
            fixture = new ExtendedObjectMapper().readTree(input);
        }
        try (Server server = new Server(new Reply(200, fixture.get("response").toString()), new Reply(200, FOUND))) {
            SendPostResult result = send(server, model(server.origin()));
            assertEquals(SendPostResult.State.VERIFIED, result.state);
            assertEquals(256354, result.postNumber);
            assertEquals(251491, result.threadNumber);
            assertEquals(2, server.requests.size());
            Request post = server.requests.get(0);
            assertEquals(fixture.get("method").asText() + " " + fixture.get("path").asText() + " HTTP/1.1", post.line);
            assertEquals(fixture.get("headers").get("Accept").asText(), post.headers.get("accept"));
            assertEquals("XMLHttpRequest", post.headers.get("x-requested-with"));
            assertEquals(server.origin(), post.headers.get("origin"));
            assertEquals(server.origin() + "/test/res/251491.html", post.headers.get("referer"));
            assertTrue(post.headers.get("cookie").contains("passcode_auth=fixture-cookie"));
            Map<String, String> form = post.form();
            java.util.Iterator<Map.Entry<String, JsonNode>> fields = fixture.get("fields").getFields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                assertEquals(field.getKey(), field.getValue().asText(), form.get(field.getKey()));
            }
            Request get = server.requests.get(1);
            assertEquals("GET /api/mobile/v2/post/test/256354 HTTP/1.1", get.line);
            assertEquals("no-cache", get.headers.get("cache-control"));
            assertNull(get.headers.get("if-modified-since"));
            assertTrue(get.headers.get("cookie").contains("passcode_auth=fixture-cookie"));
            assertFalse(result.getDiagnostics().contains("fixture-cookie"));
            assertFalse(result.getDiagnostics().contains("fixture-solution"));
            assertFalse(result.getDiagnostics().contains("fixture-comment"));
        }
    }

    public void testRejectsApiErrorWithoutReadBack() throws Exception {
        try (Server server = new Server(new Reply(200, "{\"result\":0,\"error\":{\"code\":-5}}"))) {
            SendPostResult result = send(server, model(server.origin()));
            assertEquals(SendPostResult.State.REJECTED, result.state);
            assertEquals(-5, result.errorCode);
            assertEquals(1, server.requests.size());
        }
    }

    public void testLostPostResponseIsUnknownAndNeverRetried() throws Exception {
        try (Server server = new Server(new Reply(0, ""))) {
            SendPostResult result = send(server, model(server.origin()));
            assertEquals(SendPostResult.State.UNKNOWN, result.state);
            assertFalse(result.isAccepted());
            assertEquals(1, server.requests.size());
            assertTrue(result.error.contains("мог быть создан"));
        }
    }

    public void testRedirectIsNotSuccessAndIsNotFollowed() throws Exception {
        try (Server server = new Server(new Reply(302, ""))) {
            SendPostResult result = send(server, model(server.origin()));
            assertEquals(SendPostResult.State.UNKNOWN, result.state);
            assertEquals(302, result.httpStatus);
            assertEquals(1, server.requests.size());
        }
    }

    public void testUnconfirmedJsonDoesNotCloseComposeAsSuccess() throws Exception {
        try (Server server = new Server(new Reply(200, "{\"result\":0}"))) {
            SendPostResult result = send(server, model(server.origin()));
            assertEquals(SendPostResult.State.UNKNOWN, result.state);
            assertEquals(Integer.valueOf(0), result.apiResult);
            assertEquals(1, server.requests.size());
        }
    }

    public void testReadBackFailureKeepsAcceptedPostNumber() throws Exception {
        for (Reply failure : new Reply[] { new Reply(503, "unavailable"), new Reply(0, ""),
                new Reply(200, "{\"result\":0,\"error\":{\"code\":-31}}"),
                new Reply(200, "{\"result\":1,\"post\":{\"num\":999,\"parent\":251491,\"board\":\"test\"}}") }) {
            try (Server server = new Server(new Reply(200, ACK), failure)) {
                SendPostResult result = send(server, model(server.origin()));
                assertEquals(SendPostResult.State.ACCEPTED, result.state);
                assertEquals(256354, result.postNumber);
                assertEquals(251491, result.threadNumber);
                assertEquals(2, server.requests.size());
            }
        }
    }

    public void testNewThreadOmitsParentAndRetainsFileBytes() throws Exception {
        File file = File.createTempFile("posting-fixture", ".bin");
        try {
            byte[] bytes = new byte[] { 0, 1, 13, 10, (byte) 255 };
            try (FileOutputStream output = new FileOutputStream(file)) { output.write(bytes); }
            try (Server server = new Server(new Reply(200, "{\"result\":1,\"thread\":256354}"),
                    new Reply(200, "{\"result\":1,\"post\":{\"num\":256354,\"parent\":0,\"board\":\"test\"}}"))) {
                SendPostModel model = new SendPostModel(PostingCaptcha.noCaptcha(server.origin()), "текст", false,
                        Collections.singletonList(file), "тема", null, null);
                model.setParentThread("");
                SendPostResult result = send(server, model);
                assertEquals(SendPostResult.State.VERIFIED, result.state);
                assertEquals(256354, result.threadNumber);
                Request request = server.requests.get(0);
                assertNull(request.form().get("thread"));
                assertEquals("nocaptcha", request.form().get("captcha_type"));
                assertEquals("текст", request.form().get("comment"));
                assertFalse(request.form().containsKey("emoji_captcha_id"));
                assertFalse(request.form().containsKey("2ch_challenge"));
                String binary = new String(request.body, StandardCharsets.ISO_8859_1);
                assertTrue(binary.contains("name=\"file[]\"; filename=\"" + file.getName() + "\""));
                assertTrue(binary.contains(new String(bytes, StandardCharsets.ISO_8859_1)));
                assertFalse(result.getDiagnostics().contains(file.getName()));
            }
        } finally {
            if (!file.delete()) file.deleteOnExit();
        }
    }

    public void testDomainChangeDoesNotSendCaptchaToAnotherOrigin() throws Exception {
        try (Server server = new Server(new Reply(200, ACK))) {
            SendPostResult result = send(server, model("https://other.example"));
            assertEquals(SendPostResult.State.REJECTED, result.state);
            assertEquals(0, server.requests.size());
            assertTrue(result.error.contains("Домен изменился"));
        }
    }

    private SendPostModel model(String origin) {
        SendPostModel model = new SendPostModel(PostingCaptcha.emoji("fixture-solution-not-a-real-token", "6904", origin),
                "fixture-comment", false, new ArrayList<File>(), null, null, null);
        model.setParentThread("251491");
        return model;
    }

    private SendPostResult send(Server server, SendPostModel model) {
        DefaultHttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 2000);
        HttpConnectionParams.setSoTimeout(client.getParams(), 2000);
        BasicClientCookie cookie = new BasicClientCookie("passcode_auth", "fixture-cookie");
        cookie.setDomain("127.0.0.1");
        cookie.setPath("/");
        client.getCookieStore().addCookie(cookie);
        HttpStringReader reader = new HttpStringReader(new HttpBytesReader(new HttpStreamReader(client, null), null));
        IUrlBuilder urls = (IUrlBuilder) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IUrlBuilder.class },
                (proxy, method, args) -> {
                    if (method.getName().equals("getPostingUrlApi")) return server.origin() + "/user/posting?nc=1";
                    throw new UnsupportedOperationException(method.getName());
                });
        IWebsite site = new IWebsite() {
            public String name() { return "test"; }
            public IUrlBuilder getUrlBuilder() { return urls; }
            public IUrlParser getUrlParser() { return null; }
        };
        try {
            return new PostSender(client, reader, null).sendPost(site, "test", model);
        } finally {
            client.getConnectionManager().shutdown();
        }
    }

    private static final class Reply {
        final int status;
        final String body;
        Reply(int status, String body) { this.status = status; this.body = body; }
    }

    private static final class Request {
        String line;
        final Map<String, String> headers = new HashMap<>();
        byte[] body;

        Map<String, String> form() {
            Map<String, String> fields = new HashMap<>();
            String boundary = headers.get("content-type").split("boundary=", 2)[1];
            String raw = new String(body, StandardCharsets.UTF_8);
            for (String part : raw.split(java.util.regex.Pattern.quote("--" + boundary))) {
                int keyStart = part.indexOf("name=\"");
                int valueStart = part.indexOf("\r\n\r\n");
                if (keyStart < 0 || valueStart < 0) continue;
                keyStart += 6;
                String name = part.substring(keyStart, part.indexOf('"', keyStart));
                fields.put(name, part.substring(valueStart + 4, part.length() - 2));
            }
            return fields;
        }
    }

    /** Real TCP requests exercise Apache execution, headers, multipart streaming and response reading. */
    private static final class Server implements AutoCloseable {
        final ServerSocket socket = new ServerSocket(0, 10, java.net.InetAddress.getByName("127.0.0.1"));
        final List<Request> requests = new CopyOnWriteArrayList<>();
        final Thread thread;
        volatile Throwable failure;

        Server(Reply... replies) throws IOException {
            thread = new Thread(() -> {
                try {
                    while (!socket.isClosed()) {
                        try (Socket connection = socket.accept()) {
                            connection.setSoTimeout(2000);
                            BufferedInputStream input = new BufferedInputStream(connection.getInputStream());
                            Request request = new Request();
                            request.line = line(input);
                            String header;
                            while (!(header = line(input)).isEmpty()) {
                                int colon = header.indexOf(':');
                                request.headers.put(header.substring(0, colon).toLowerCase(java.util.Locale.US), header.substring(colon + 1).trim());
                            }
                            int size = Integer.parseInt(request.headers.getOrDefault("content-length", "0"));
                            request.body = new byte[size];
                            for (int read = 0; read < size;) {
                                int count = input.read(request.body, read, size - read);
                                if (count < 0) throw new IOException("Truncated request body");
                                read += count;
                            }
                            int index = requests.size();
                            requests.add(request);
                            Reply reply = index < replies.length ? replies[index] : new Reply(500, "unexpected retry");
                            if (reply.status == 0) continue;
                            byte[] body = reply.body.getBytes(StandardCharsets.UTF_8);
                            String headers = "HTTP/1.1 " + reply.status + " Fixture\r\nContent-Type: application/json; charset=UTF-8\r\n"
                                    + "Content-Length: " + body.length + "\r\nConnection: close\r\n"
                                    + (reply.status == 302 ? "Location: " + origin() + "/redirect-target\r\n" : "") + "\r\n";
                            connection.getOutputStream().write(headers.getBytes(StandardCharsets.US_ASCII));
                            connection.getOutputStream().write(body);
                            connection.getOutputStream().flush();
                        }
                    }
                } catch (Throwable e) {
                    if (!socket.isClosed()) failure = e;
                }
            }, "posting-fixture-server");
            thread.setDaemon(true);
            thread.start();
        }

        String origin() { return "http://127.0.0.1:" + socket.getLocalPort(); }

        public void close() throws Exception {
            socket.close();
            thread.join(3000);
            if (thread.isAlive()) throw new AssertionError("Fixture server did not terminate");
            if (failure != null) throw new AssertionError(failure);
        }

        private static String line(InputStream input) throws IOException {
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            for (int b; (b = input.read()) != -1;) {
                if (b == '\n') return new String(output.toByteArray(), StandardCharsets.US_ASCII).replace("\r", "");
                output.write(b);
            }
            throw new IOException("Unexpected EOF in HTTP headers");
        }
    }
}
