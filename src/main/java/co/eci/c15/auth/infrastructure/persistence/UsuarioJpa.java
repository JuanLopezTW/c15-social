package co.eci.c15.auth.infrastructure.persistence;

import jakarta.persistence.*;

@Entity
@Table(name = "usuarios")
class UsuarioJpa {

    @Id
    private String id;

    @Column(unique = true, nullable = false, length = 20)
    private String username;

    protected UsuarioJpa() {}

    UsuarioJpa(String id, String username) {
        this.id = id;
        this.username = username;
    }

    String getId() { return id; }
    String getUsername() { return username; }
}
