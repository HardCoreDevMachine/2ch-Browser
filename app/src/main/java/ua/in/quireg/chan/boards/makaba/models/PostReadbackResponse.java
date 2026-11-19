package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

/** Response of GET /api/mobile/v2/post/{board}/{num} (api.yml MobilePost). */
public class PostReadbackResponse {
    @JsonProperty("result")
    public int result;
    @JsonProperty("error")
    public ApiError error;
    @JsonProperty("post")
    public Post post;

    public static class Post {
        // Boxed so a missing field stays null and never matches a requested id.
        @JsonProperty("num")
        public Integer num;
        @JsonProperty("parent")
        public Integer parent;
        @JsonProperty("board")
        public String board;
    }
}
