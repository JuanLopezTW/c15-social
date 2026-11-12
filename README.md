# c15-social

Servicio **social** del EscapeRoom C15 (ARSW, equipo C15-PortalSuba): perfil del jugador, chat (4 canales) y
ranking/estadísticas. Sale del monolito [ScapeRoomC15](https://github.com/JuanLopezTW/ScapeRoomC15), que se
queda desplegado como respaldo.

## Qué hace

| Parte | Paquete | API |
|---|---|---|
| Perfil | `co.eci.c15.auth` | `GET/POST /api/perfil`, `GET /api/usuarios?ids=` |
| Chat | `co.eci.c15.chat` | `/api/chat/global`, `/api/chat/salas/{id}`, `/api/chat/partidas/{m}`, `/api/chat/partidas/{m}/equipo` |
| Estadísticas | `co.eci.c15.estadisticas` | `GET /api/estadisticas/ranking?limite=`, `GET /api/estadisticas/jugadores/{userId}` |

- **Escucha** de Kafka: `c15.partida-iniciada` (abre los chats de la partida), `c15.partida-terminada` (cierra
  esos chats y suma al ranking) y `c15.sala-cerrada` (olvida el chat de la sala).
- **Publica** en los topics STOMP `/topic/chat/...` y `/topic/estadisticas/ranking` por RabbitMQ. No tiene
  WebSocket propio: los jugadores se conectan a `c15-broker`.
- **Llama** a `c15-salas` (`GET /api/salas/{id}`, con el token del jugador) para el chat de la sala de espera.
- Valida el JWT de Auth0 en toda la API salvo `/api/version`. El `userId` es el `sub` del token.

`co.eci.c15.plataforma` (seguridad, observabilidad, Kafka, STOMP) es una copia de lo que tienen todos los
servicios. Si se cambia aquí, hay que cambiarlo también en los demás.

## Correr

```bash
mvn verify                  # pruebas + cobertura (minimo 80%); las de Kafka y Redis necesitan Docker
mvn spring-boot:run         # necesita Postgres, Kafka, RabbitMQ (y Redis si ESTADO_REDIS=true): ver .env.example
```

Para levantar todo el sistema junto, usa el compose de `c15-gateway`.

## Admin (solo rol `admin`)

Todo sale de los logs centralizados (Log Analytics), así que los números están consolidados entre réplicas. Un jugador recibe **403**; fuera de Azure la respuesta es **503** con el motivo.

| Método y ruta | Qué devuelve |
|---|---|
| `GET /api/admin/tablero/negocio?minutos=60` | `totales` (jugadoresActivos, sesionesIniciadas, jugadoresNuevos, salasCreadas, partidasIniciadas, partidasTerminadas, acertijosResueltos, objetosRecolectados, retosJugados, interacciones), `masUsadas` [{funcion, usos}] y `jugadoresEnElTiempo` [{momento, jugadores}] |
| `GET /api/admin/tablero/tecnico?minutos=15` | `replicas` [{servicio, replica, peticiones, p50, p95, porcentajeErrores, excepciones, ultimaSenal, degradada, sinSenal}] y `usuariosConcurrentes` (últimos 5 min) |
| `GET /api/admin/errores/{codigo}?dias=1` | Lo que dejó la petición con ese código (el que el front muestra al jugador): [{hora, servicio, replica, nivel, mensaje, traceId, ruta, estado, excepcion, detalle, pila}] |

`minutos` va de 5 a 1440; `dias` de 1 a 7. `degradada` = p95 > 300 ms o errores > 2 %; `sinSenal` = sin logs en 2 min (escenario O2). En el front, `useEsAdmin()` dice si mostrar estas pantallas.
