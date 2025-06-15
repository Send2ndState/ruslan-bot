package school.sorokin.event.manager.telegrambot.openai;

import jakarta.annotation.Nonnull;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import school.sorokin.event.manager.telegrambot.config.GptConfig;
import school.sorokin.event.manager.telegrambot.openai.api.ChatCompletionRequest;
import school.sorokin.event.manager.telegrambot.openai.api.Message;
import school.sorokin.event.manager.telegrambot.openai.api.OpenAIClient;
import school.sorokin.event.manager.telegrambot.telegram.message.TelegramTextHandler;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatGptService {

    private final OpenAIClient openAIClient;
    private final ChatGptHistoryService chatGptHistoryService;
    private final GptConfig gptConfig;

    @Nonnull
    public String getResponseChatForUser(
            Long userId,
            String userTextInput
    ) {
        return getResponseChatForUserWithImages(userId, userTextInput, null,null);
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
        return getResponseChatForUserWithImages(userId, userTextInput, null, images);
    }

    @Nonnull
    public String getResponseChatForUserWithImages(
            Long userId,
            String userTextInput,
            String prompt,
            List<Map<String, Object>> newImages
    ) {
        log.info("Starting getResponseChatForUser for userId: {}, text: {}, images count: {}", 
            userId, userTextInput, newImages != null ? newImages.size() : 0);
            
        chatGptHistoryService.createHistoryIfNotExist(userId);
        
        // Add system prompt if it's a new chat
        if (chatGptHistoryService.getUserHistory(userId).map(history -> history.chatMessages().isEmpty()).orElse(true)) {
            log.info("Adding system prompt for new chat");
            var systemMessage = new Message(
                    "system",
                    prompt
            );
            chatGptHistoryService.addMessageToHistory(userId, systemMessage);
        }
        
        Message userMessage;
        List<Map<String, Object>> allImages = new ArrayList<>();
        
        // Добавляем изображения из истории
        chatGptHistoryService.getUserHistory(userId).ifPresent(history -> {
            allImages.addAll(history.images());
        });
        
        // Добавляем новые изображения
        if (newImages != null) {
            allImages.addAll(newImages);
        }
        
        if (!allImages.isEmpty()) {
            log.info("Creating message with {} images for user {}", allImages.size(), userId);
            // Create a message with image content
            List<Object> content = new ArrayList<>();
            content.add(Map.of("type", "text", "text", userTextInput));
            
            // Добавляем изображения с указанием типа
            for (Map<String, Object> image : allImages) {
                @SuppressWarnings("unchecked")
                Map<String, String> imageUrl = (Map<String, String>) image.get("image_url");
                if (imageUrl != null && imageUrl.get("url") != null) {
                    content.add(Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", imageUrl.get("url"))
                    ));
                }
            }
            
            log.info("Message content structure: {}", content);
            
            userMessage = new Message(
                    "user",
                    content
            );
        } else {
            log.info("Creating text message for user {}", userId);
            userMessage = new Message(
                    "user",
                    userTextInput
            );
        }

        var history = chatGptHistoryService.addMessageToHistory(userId, userMessage);
        log.info("Current chat history size: {}", history.chatMessages().size());

        var request = new ChatCompletionRequest(
                gptConfig.getModel(),
                history.chatMessages(),
                4000,
                1.0,
                1.0
        );
        
        log.info("Sending request to GPT with model: {}", request.model());
        log.info("Request messages: {}", request.messages());
        
        var response = openAIClient.createChatCompletion(request);
        log.info("Received response from GPT: {}", response);

        var messageFromGpt = response.choices().get(0).message();
        log.info("GPT response content type: {}", messageFromGpt.content().getClass().getName());
        log.info("GPT response content: {}", messageFromGpt.content());

        // Convert the response to a text message to maintain chat history
        Message textMessage = new Message(
                "assistant",
                messageFromGpt.content().toString()
        );

        chatGptHistoryService.addMessageToHistory(userId, textMessage);

        return messageFromGpt.content().toString();
    }
}
