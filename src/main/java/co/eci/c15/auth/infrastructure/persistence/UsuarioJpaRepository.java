package co.eci.c15.auth.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

interface UsuarioJpaRepository extends JpaRepository<UsuarioJpa, String> {
    boolean existsByUsername(String username);
    java.util.Optional<UsuarioJpa> findByUsername(String username);
}
