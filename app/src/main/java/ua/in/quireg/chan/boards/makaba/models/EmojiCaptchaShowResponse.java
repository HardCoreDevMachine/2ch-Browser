package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class EmojiCaptchaShowResponse {
    @JsonProperty("image")
    public String image;
    @JsonProperty("keyboard")
    public String[] keyboard;
    @JsonProperty("success")
    public String success;
}
