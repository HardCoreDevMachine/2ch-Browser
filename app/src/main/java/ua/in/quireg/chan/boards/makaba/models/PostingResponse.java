package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class PostingResponse {
    @JsonProperty("result")
    public int result;
    @JsonProperty("error")
    public ApiError error;
    @JsonProperty("thread")
    public int thread;
    @JsonProperty("num")
    public int num;
}
