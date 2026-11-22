package dev.samuelneves.relay.delivery;

public enum DeliveryStatus {
    /** Aguardando a proxima tentativa (a primeira ou um retry). */
    PENDING,
    /** Reservada por um worker ate locked_until. */
    IN_PROGRESS,
    /** O destino respondeu 2xx. */
    SUCCEEDED,
    /** Esgotou as tentativas. */
    DEAD,
    /** O endpoint foi desativado antes da entrega. */
    CANCELLED
}
