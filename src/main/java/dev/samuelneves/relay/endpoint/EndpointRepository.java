package dev.samuelneves.relay.endpoint;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface EndpointRepository extends JpaRepository<Endpoint, UUID> {

    /**
     * Endpoints ativos que assinam o tipo de evento.
     * O operador @> ("contem") consegue usar o indice GIN em event_types.
     */
    @Query(value = """
            SELECT * FROM endpoints
            WHERE active
              AND event_types @> ARRAY[CAST(:eventType AS text)]
            """, nativeQuery = true)
    List<Endpoint> findActiveSubscribedTo(@Param("eventType") String eventType);
}
