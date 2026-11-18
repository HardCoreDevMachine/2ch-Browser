package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class CaptchaApiResponse {
    @JsonProperty("result")
    public int result;
    @JsonProperty("error")
    public ApiError error;
    @JsonProperty("type")
    public String type;
    @JsonProperty("id")
    public String id;
    @JsonProperty("challenge")
    public Challenge challenge;

    public static class Challenge {
        @JsonProperty("hash")
        public String hash;
        @JsonProperty("limit")
        public int limit;
        @JsonProperty("template")
        public String template;
    }
}
