package net.mortalsilence.dandroid.comm;

public class UnexpectedResponseValueException extends Exception {

    public UnexpectedResponseValueException(String message) {
        super(message);
    }

    public UnexpectedResponseValueException(String message, Throwable cause) {
        super(message, cause);
    }
}
