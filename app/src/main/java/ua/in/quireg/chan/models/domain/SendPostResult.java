package ua.in.quireg.chan.models.domain;

public class SendPostResult {
    public enum State {
        UNKNOWN, REJECTED, ACCEPTED, VERIFIED
    }

    public State state = State.UNKNOWN;
    public String board;
    public int threadNumber;
    public int postNumber;
    public Integer apiResult;
    public int errorCode;
    public String error;
    public boolean requiresCloudflare;
    public String postingEndpoint;
    public int httpStatus;
    public String verificationEndpoint;
    public int verificationHttpStatus;
    public Integer verificationResult;
    public int verificationErrorCode;

    public boolean isAccepted() {
        return state == State.ACCEPTED || state == State.VERIFIED;
    }

    /** Only protocol metadata: never include bodies, cookies, captcha tokens or file names. */
    public String getDiagnostics() {
        return "state=" + state
                + "\nPOST " + postingEndpoint + "\nHTTP=" + httpStatus
                + "\nboard=" + board + ", thread=" + threadNumber + ", num=" + postNumber
                + "\nresult=" + apiResult + ", error.code=" + errorCode
                + "\nGET " + verificationEndpoint + "\nHTTP=" + verificationHttpStatus
                + "\nresult=" + verificationResult + ", error.code=" + verificationErrorCode;
    }
}
