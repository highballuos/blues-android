package com.highballuos.blues.tfml.sentencepiece;

public class SentencePieceException extends RuntimeException {

    public SentencePieceException() {
        super();
    }

    public SentencePieceException(String message) {
        super(message);
    }

    public SentencePieceException(String message, Throwable cause) {
        super(message, cause);
    }
}
