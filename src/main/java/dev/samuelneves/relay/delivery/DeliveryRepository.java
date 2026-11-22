package dev.samuelneves.relay.delivery;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
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

    List<Delivery> findByStatusOrderByUpdatedAtDesc(DeliveryStatus status, Pageable pageable);

    List<Delivery> findByStatusAndEndpoint_IdOrderByUpdatedAtDesc(DeliveryStatus status, UUID endpointId, Pageable pageable);

    /**
     * Devolve para a fila, num unico UPDATE, todas as entregas mortas de um endpoint.
     * Em JPQL (nomes de entidade e atributo, nao de tabela). Mais eficiente que carregar
     * milhares de entidades so para mudar um campo em cada uma.
     */
    @Modifying
    @Query("""
            UPDATE Delivery d
            SET d.status = dev.samuelneves.relay.delivery.DeliveryStatus.PENDING,
                d.attemptCount = 0,
                d.nextAttemptAt = :now,
                d.lockedUntil = null,
                d.updatedAt = :now
            WHERE d.endpoint.id = :endpointId
              AND d.status = dev.samuelneves.relay.delivery.DeliveryStatus.DEAD
            """)
    int requeueDeadByEndpoint(@Param("endpointId") UUID endpointId, @Param("now") Instant now);
}
