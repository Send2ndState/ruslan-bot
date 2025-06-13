package school.sorokin.event.manager.telegrambot.openai.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Message(
    @JsonProperty("role") String role,
    @JsonProperty("content") Object content
) {}
