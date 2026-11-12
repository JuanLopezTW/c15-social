package co.eci.c15.auth.domain;

public class UsernameInvalidoException extends RuntimeException {
    public UsernameInvalidoException(String message) {
        super(message);
    }
}
