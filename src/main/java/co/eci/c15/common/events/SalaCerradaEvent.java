package co.eci.c15.common.events;

/** La sala dejo de existir (el anfitrion salio del lobby o termino su partida). */
public record SalaCerradaEvent(String salaId) {
}
