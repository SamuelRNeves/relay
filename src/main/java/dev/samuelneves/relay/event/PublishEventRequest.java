package dev.samuelneves.relay.event;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;

/** Corpo do POST /events. O payload e livre: o Relay so guarda e repassa. */
public record PublishEventRequest(
        @NotBlank @Size(max = 100) String type,
        @NotNull Map<String, Object> payload
) {
}
