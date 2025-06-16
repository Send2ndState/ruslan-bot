package homebrew.secondstate.telegrambot.openai.api;

import com.fasterxml.jackson.annotation.JsonProperty;

public record Message(
    @JsonProperty("role") String role,
    @JsonProperty("content") Object content
) {}
