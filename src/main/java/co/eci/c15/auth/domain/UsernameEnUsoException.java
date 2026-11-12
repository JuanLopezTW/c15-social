package co.eci.c15.auth.domain;

public class UsernameEnUsoException extends RuntimeException {
    public UsernameEnUsoException(String username) {
        super("El nombre de usuario '" + username + "' ya está en uso");
    }
}
