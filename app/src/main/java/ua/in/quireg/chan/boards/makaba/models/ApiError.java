package ua.in.quireg.chan.boards.makaba.models;

import org.codehaus.jackson.annotate.JsonProperty;

public class ApiError {
    @JsonProperty("code")
    public int code;
    @JsonProperty("message")
    public String message;
}
