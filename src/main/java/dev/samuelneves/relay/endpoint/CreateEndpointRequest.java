package dev.samuelneves.relay.endpoint;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import org.hibernate.validator.constraints.URL;

import java.util.List;

/** Corpo do POST /endpoints. */
public record CreateEndpointRequest(
        @NotBlank @URL String url,
        @NotEmpty List<@NotBlank String> eventTypes
) {
}
