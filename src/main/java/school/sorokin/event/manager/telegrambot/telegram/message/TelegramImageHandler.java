package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;
import school.sorokin.event.manager.telegrambot.telegram.TelegramFileService;

import java.util.Base64;
import java.nio.file.Files;
import java.util.Map;
import java.util.List;

@Slf4j
@Service
@AllArgsConstructor
public class TelegramImageHandler {

    private final ChatGptService gptService;
    private final TelegramFileService telegramFileService;

    public SendMessage processImage(Message message) {
        var chatId = message.getChatId();
        var photo = message.getPhoto().get(message.getPhoto().size() - 1); // Get the highest quality photo

        var fileId = photo.getFileId();
        log.info("Processing image with fileId: {}", fileId);
        
        var file = telegramFileService.getFile(fileId);
        log.info("Downloaded file to: {}", file.getAbsolutePath());
        
        try {
            // Convert image to base64
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);
            log.info("Image converted to base64, length: {}", base64Image.length());
            
            // Отправляем фото в GPT
            var gptGeneratedText = gptService.getResponseChatForUserWithImages(
                chatId, 
                "Проанализируй это фото и опиши человека.",
                List.of(
                    Map.of(
                        "type", "image_url",
                        "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
                    )
                )
            );
            
            return new SendMessage(chatId.toString(), gptGeneratedText);
            
        } catch (Exception e) {
            log.error("Error processing image", e);
            return new SendMessage(chatId.toString(), "Sorry, I couldn't process the image. Error: " + e.getMessage());
        } finally {
            // Clean up the temporary file
            if (file != null && file.exists()) {
                file.delete();
                log.info("Temporary file deleted");
            }
        }
    }
} 