package dev.samuelneves.relay.delivery;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {

    /**
     * Entregas prontas para tentar: pendentes e vencidas, ou reservadas por um worker
     * cujo prazo expirou (provavelmente ele caiu no meio).
     *
     * FOR UPDATE trava as linhas ate o fim da transacao; SKIP LOCKED faz outro worker
     * pular as linhas ja travadas em vez de esperar. Assim varios workers dividem a fila
     * sem disputa e sem pegar a mesma entrega.
     */
    @Query(value = """
            SELECT * FROM deliveries
            WHERE (status = 'PENDING' AND next_attempt_at <= :now)
               OR (status = 'IN_PROGRESS' AND locked_until < :now)
            ORDER BY next_attempt_at
            LIMIT :limit
            FOR UPDATE SKIP LOCKED
            """, nativeQuery = true)
    List<Delivery> findDueForUpdate(@Param("now") Instant now, @Param("limit") int limit);

    List<Delivery> findByEvent_IdOrderByCreatedAtAsc(UUID eventId);
}
