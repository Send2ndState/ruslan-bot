package school.sorokin.event.manager.telegrambot.openai.api;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ChatCompletionRequest(
    @JsonProperty("model") String model,
    @JsonProperty("messages") List<Message> messages,
    @JsonProperty("max_tokens") Integer maxTokens,
    @JsonProperty("temperature") Double temperature,
    @JsonProperty("presence_penalty") Double presencePenalty
) {}
