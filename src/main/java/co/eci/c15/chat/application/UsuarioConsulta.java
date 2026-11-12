package co.eci.c15.chat.application;

/** Lo que el chat global necesita saber de los usuarios; la implementacion vive en app, como {@link SalaEsperaConsulta}. */
public interface UsuarioConsulta {

    boolean existe(String userId);
}
