package school.sorokin.event.manager.telegrambot.openai.api;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

import java.util.List;

@Builder
public record ChatCompletionRequest(
    @JsonProperty("model") String model,
    @JsonProperty("messages") List<Message> messages,
    @JsonProperty("max_tokens") Integer maxTokens
) {}
