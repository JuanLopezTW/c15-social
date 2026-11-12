package co.eci.c15.chat.infrastructure.web;

import co.eci.c15.chat.application.ChatService;
import co.eci.c15.chat.domain.ChatNoDisponibleException;
import co.eci.c15.chat.domain.DemasiadosMensajesException;
import co.eci.c15.chat.domain.JugadorFueraDeSalaException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;
import java.util.function.Supplier;

/** GET historial y POST mensaje de cada canal; los mensajes nuevos llegan por STOMP en /topic/chat/{canal}. */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private final ChatService chat;

    public ChatController(ChatService chat) {
        this.chat = chat;
    }

    @GetMapping("/global")
    public ResponseEntity<?> historialGlobal() {
        return responder(HttpStatus.OK, chat::historialGlobal);
    }

    @PostMapping("/global")
    public ResponseEntity<?> enviarGlobal(@RequestBody Map<String, String> body, Principal yo) {
        return responder(HttpStatus.CREATED, () -> chat.enviarGlobal(yo.getName(), body.get("texto")));
    }

    @GetMapping("/salas/{salaId}")
    public ResponseEntity<?> historialSala(@PathVariable("salaId") String salaId, Principal yo) {
        return responder(HttpStatus.OK, () -> chat.historialSala(salaId, yo.getName()));
    }

    @PostMapping("/salas/{salaId}")
    public ResponseEntity<?> enviarSala(@PathVariable("salaId") String salaId, @RequestBody Map<String, String> body,
                                           Principal yo) {
        return responder(HttpStatus.CREATED, () -> chat.enviarSala(salaId, yo.getName(), body.get("texto")));
    }

    @GetMapping("/partidas/{matchId}")
    public ResponseEntity<?> historialPartida(@PathVariable("matchId") String matchId, Principal yo) {
        return responder(HttpStatus.OK, () -> chat.historialPartida(matchId, yo.getName()));
    }

    @PostMapping("/partidas/{matchId}")
    public ResponseEntity<?> enviarPartida(@PathVariable("matchId") String matchId, @RequestBody Map<String, String> body,
                                           Principal yo) {
        return responder(HttpStatus.CREATED, () -> chat.enviarPartida(matchId, yo.getName(), body.get("texto")));
    }

    @GetMapping("/partidas/{matchId}/equipo")
    public ResponseEntity<?> historialEquipo(@PathVariable("matchId") String matchId, Principal yo) {
        return responder(HttpStatus.OK, () -> chat.historialEquipo(matchId, yo.getName()));
    }

    @PostMapping("/partidas/{matchId}/equipo")
    public ResponseEntity<?> enviarEquipo(@PathVariable("matchId") String matchId, @RequestBody Map<String, String> body,
                                           Principal yo) {
        return responder(HttpStatus.CREATED, () -> chat.enviarEquipo(matchId, yo.getName(), body.get("texto")));
    }

    private static ResponseEntity<?> responder(HttpStatus ok, Supplier<Object> accion) {
        try {
            return ResponseEntity.status(ok).body(accion.get());
        } catch (ChatNoDisponibleException e) {
            return error(HttpStatus.CONFLICT, e.getMessage());
        } catch (JugadorFueraDeSalaException e) {
            return error(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (DemasiadosMensajesException e) {
            return error(HttpStatus.TOO_MANY_REQUESTS, e.getMessage());
        } catch (IllegalArgumentException e) {
            return error(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private static ResponseEntity<?> error(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(Map.of("error", mensaje));
    }
}
