package dev.samuelneves.relay.delivery;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, UUID> {

    // O Spring Data monta a consulta a partir do nome: event.id = ? ORDER BY attemptedAt
    List<DeliveryAttempt> findByEvent_IdOrderByAttemptedAtAsc(UUID eventId);
}
