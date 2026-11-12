package co.eci.c15.chat.application;

import co.eci.c15.common.logging.EventoNegocio;
import co.eci.c15.chat.domain.CanalChat;
import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.ChatRepository;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import co.eci.c15.chat.domain.MensajeChat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Los cuatro chats del juego, todos efimeros:
 * <ul>
 *   <li>global (menu): cualquier usuario; los mensajes caducan a la hora.</li>
 *   <li>sala: los jugadores de la sala, mientras no inicie la partida.</li>
 *   <li>partida: todos los jugadores de la partida.</li>
 *   <li>equipo: solo los de su equipo, que lo decide el back a partir del userId.</li>
 * </ul>
 */
@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    static final Duration CADUCIDAD_GLOBAL = Duration.ofHours(1);

    private final ChatRepository mensajes;
    private final SalaEsperaConsulta salas;
    private final UsuarioConsulta usuarios;
    private final ChatNotifier notifier;
    private final LimiteEnvios limite;
    /** matchId -> equipoId -> jugadores, tal como llegan en PartidaIniciadaEvent. */
    private final EquiposDePartida partidas;
    private final Clock clock;

    @Autowired
    public ChatService(ChatRepository mensajes, SalaEsperaConsulta salas, UsuarioConsulta usuarios, ChatNotifier notifier,
                       LimiteEnvios limite, EquiposDePartida partidas) {
        this(mensajes, salas, usuarios, notifier, limite, partidas, Clock.systemUTC());
    }

    ChatService(ChatRepository mensajes, SalaEsperaConsulta salas, UsuarioConsulta usuarios, ChatNotifier notifier,
                LimiteEnvios limite, EquiposDePartida partidas, Clock clock) {
        this.mensajes = mensajes;
        this.salas = salas;
        this.usuarios = usuarios;
        this.notifier = notifier;
        this.limite = limite;
        this.partidas = partidas;
        this.clock = clock;
    }

    public MensajeChatDto enviarGlobal(String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.GLOBAL.clave(), userId, texto, clock.instant());
        if (!usuarios.existe(userId)) throw new IllegalArgumentException("El usuario no existe");
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialGlobal() {
        Instant limiteCaducidad = clock.instant().minus(CADUCIDAD_GLOBAL);
        return mensajes.historial(CanalChat.GLOBAL.clave()).stream()
                .filter(m -> m.enviadoEn().isAfter(limiteCaducidad))
                .map(MensajeChatDto::from)
                .toList();
    }

    public MensajeChatDto enviarSala(String salaId, String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.sala(salaId).clave(), userId, texto, clock.instant());
        validarSala(salaId, userId);
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialSala(String salaId, String userId) {
        exigirUserId(userId);
        validarSala(salaId, userId);
        return historial(CanalChat.sala(salaId));
    }

    public MensajeChatDto enviarPartida(String matchId, String userId, String texto) {
        MensajeChat mensaje = MensajeChat.crear(CanalChat.partida(matchId).clave(), userId, texto, clock.instant());
        equipoDe(matchId, userId);
        return publicar(mensaje);
    }

    public List<MensajeChatDto> historialPartida(String matchId, String userId) {
        exigirUserId(userId);
        equipoDe(matchId, userId);
        return historial(CanalChat.partida(matchId));
    }

    public MensajeChatDto enviarEquipo(String matchId, String userId, String texto) {
        exigirUserId(userId);
        CanalChat canal = CanalChat.equipo(matchId, equipoDe(matchId, userId));
        return publicar(MensajeChat.crear(canal.clave(), userId, texto, clock.instant()));
    }

    public List<MensajeChatDto> historialEquipo(String matchId, String userId) {
        exigirUserId(userId);
        return historial(CanalChat.equipo(matchId, equipoDe(matchId, userId)));
    }

    /** La sala salio de la espera: su chat se cierra y se olvida. */
    public void cerrarSala(String salaId) {
        mensajes.borrar(CanalChat.sala(salaId).clave());
    }

    public void partidaIniciada(String matchId, Map<String, List<String>> equipos) {
        cerrarSala(matchId);
        partidas.guardar(matchId, equipos);
    }

    public void partidaTerminada(String matchId) {
        partidas.de(matchId).ifPresent(equipos ->
                equipos.keySet().forEach(equipoId -> mensajes.borrar(CanalChat.equipo(matchId, equipoId).clave())));
        partidas.borrar(matchId);
        mensajes.borrar(CanalChat.partida(matchId).clave());
    }

    private MensajeChatDto publicar(MensajeChat mensaje) {
        try {
            limite.registrar(mensaje.userId(), mensaje.enviadoEn());
        } catch (DemasiadosMensajesException e) {
            EventoNegocio.de(log, "chat.limite_excedido").con("canal", mensaje.canal()).con("userId", mensaje.userId()).warn();
            throw e;
        }
        mensajes.guardar(mensaje);
        MensajeChatDto dto = MensajeChatDto.from(mensaje);
        notifier.mensajeNuevo(dto);
        EventoNegocio.de(log, "chat.mensaje_enviado").con("canal", mensaje.canal()).con("userId", mensaje.userId()).info();
        return dto;
    }

    private List<MensajeChatDto> historial(CanalChat canal) {
        return mensajes.historial(canal.clave()).stream().map(MensajeChatDto::from).toList();
    }

    private void validarSala(String salaId, String userId) {
        if (!salas.enEspera(salaId)) {
            cerrarSala(salaId);
            throw new ChatNoDisponibleException(salaId);
        }
        if (!salas.contieneJugador(salaId, userId)) throw new JugadorFueraDeSalaException(salaId, userId);
    }

    private String equipoDe(String matchId, String userId) {
        Map<String, List<String>> equipos = partidas.de(matchId).orElseThrow(() -> new ChatNoDisponibleException(matchId));
        return equipos.entrySet().stream()
                .filter(e -> e.getValue().contains(userId))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElseThrow(() -> new JugadorFueraDeSalaException(matchId, userId));
    }

    private static void exigirUserId(String userId) {
        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("El userId es obligatorio");
    }
}
