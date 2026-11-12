package co.eci.c15.auth.domain;

import java.util.Objects;

public final class Usuario {

    private final String id;
    private final String username;

    private Usuario(String id, String username) {
        this.id = id;
        this.username = username;
    }

    /** Usuario nuevo: su id es el identificador que le da el proveedor de identidad. */
    public static Usuario registrar(String id, String username) {
        validarUsername(username);
        return new Usuario(Objects.requireNonNull(id), username);
    }

    /** Reconstruye un usuario ya existente (desde persistencia) conservando su id. */
    public static Usuario reconstituir(String id, String username) {
        return new Usuario(Objects.requireNonNull(id), Objects.requireNonNull(username));
    }

    public static void validarUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new UsernameInvalidoException("El nombre de usuario no puede estar vacío");
        }
        if (!username.matches("[a-zA-Z0-9_]{3,20}")) {
            throw new UsernameInvalidoException("El nombre de usuario debe tener entre 3 y 20 caracteres alfanuméricos");
        }
    }

    public String getId() { return id; }
    public String getUsername() { return username; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario u)) return false;
        return Objects.equals(id, u.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
