package school.sorokin.event.manager.telegrambot.openai;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.sorokin.event.manager.telegrambot.config.GptConfig;
import school.sorokin.event.manager.telegrambot.openai.api.ChatCompletionRequest;
import school.sorokin.event.manager.telegrambot.openai.api.Message;
import school.sorokin.event.manager.telegrambot.openai.api.OpenAIClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@AllArgsConstructor
public class ChatGptService {

    private final OpenAIClient openAIClient;
    private final ChatGptHistoryService chatGptHistoryService;
    private final GptConfig gptConfig;

    @Nonnull
    public String getResponseChatForUser(
            Long userId,
            String userTextInput
    ) {
        return getResponseChatForUserWithImages(userId, userTextInput, null);
    }

    @Nonnull
    public String getResponseChatForUserWithSingleImage(
            Long userId,
            String userTextInput,
            String base64Image
    ) {
        List<Map<String, Object>> images = null;
        if (base64Image != null) {
            images = List.of(
                Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
                )
            );
        }
        return getResponseChatForUserWithImages(userId, userTextInput, images);
    }

    @Nonnull
    public String getResponseChatForUserWithImages(
            Long userId,
            String userTextInput,
            List<Map<String, Object>> images
    ) {
        log.info("Starting getResponseChatForUser for userId: {}, text: {}, images count: {}", 
            userId, userTextInput, images != null ? images.size() : 0);
            
        chatGptHistoryService.createHistoryIfNotExist(userId);
        
        // Add system prompt if it's a new chat
        if (chatGptHistoryService.getUserHistory(userId).map(history -> history.chatMessages().isEmpty()).orElse(true)) {
            log.info("Adding system prompt for new chat");
            var systemMessage = Message.builder()
                    .role("system")
                    .content(gptConfig.getSystemPrompt())
                    .build();
            chatGptHistoryService.addMessageToHistory(userId, systemMessage);
        }
        
        Message userMessage;
        if (images != null && !images.isEmpty()) {
            log.info("Creating message with {} images for user {}", images.size(), userId);
            // Create a message with image content
            List<Object> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", userTextInput));
            content.addAll(images);
            
            log.info("Message content structure: {}", content);
            
            userMessage = Message.builder()
                    .role("user")
                    .content(content)
                    .build();
        } else {
            log.info("Creating text message for user {}", userId);
            userMessage = Message.builder()
                    .content(userTextInput)
                    .role("user")
                    .build();
        }

        var history = chatGptHistoryService.addMessageToHistory(userId, userMessage);
        log.info("Current chat history size: {}", history.chatMessages().size());

        var request = ChatCompletionRequest.builder()
                .model(gptConfig.getModel())
                .messages(history.chatMessages())
                .maxTokens(1000)
                .build();
        
        log.info("Sending request to GPT with model: {}", request.model());
        log.info("Request messages: {}", request.messages());
        
        var response = openAIClient.createChatCompletion(request);
        log.info("Received response from GPT: {}", response);

        var messageFromGpt = response.choices().get(0).message();
        log.info("GPT response content type: {}", messageFromGpt.content().getClass().getName());
        log.info("GPT response content: {}", messageFromGpt.content());

        // Convert the response to a text message to maintain chat history
        Message textMessage = Message.builder()
                .role("assistant")
                .content(messageFromGpt.content().toString())
                .build();

        chatGptHistoryService.addMessageToHistory(userId, textMessage);

        return messageFromGpt.content().toString();
    }
}
