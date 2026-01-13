package com.lyncalor.calyra.llm;

public class LlmRequestException extends RuntimeException {

    private final String responseBody;

    public LlmRequestException(String message, String responseBody) {
        super(message);
        this.responseBody = responseBody;
    }

    public String getResponseBody() {
        return responseBody;
    }
}
