package dev.samuelneves.relay.delivery;

/** O resultado de uma chamada HTTP: um status, ou um erro quando nao houve resposta. */
public record SendResult(Integer statusCode, String error, long durationMs) {

    public static SendResult response(int statusCode, long durationMs) {
        return new SendResult(statusCode, null, durationMs);
    }

    public static SendResult failure(String error, long durationMs) {
        return new SendResult(null, error, durationMs);
    }

    public boolean success() {
        return statusCode != null && statusCode >= 200 && statusCode < 300;
    }
}
