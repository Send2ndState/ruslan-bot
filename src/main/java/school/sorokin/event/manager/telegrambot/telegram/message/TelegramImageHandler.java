package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;
import school.sorokin.event.manager.telegrambot.openai.ChatGptHistoryService;
import school.sorokin.event.manager.telegrambot.telegram.TelegramFileService;
import school.sorokin.event.manager.telegrambot.telegram.state.UserImageState;

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
    private final UserImageState userImageState;
    private final ChatGptHistoryService chatGptHistoryService;

    public SendMessage processImage(Message message) {
        var chatId = message.getChatId();

        if (!userImageState.canSendMoreImages(chatId)) {
            return new SendMessage(chatId.toString(),
                    "Вы уже отправили максимальное количество изображений (4) за сегодня. Попробуйте завтра или отправьте текстовое или голосовое сообщение.");
        }

        String fileId;
        if (message.hasPhoto()) {
            var photo = message.getPhoto().get(message.getPhoto().size() - 1);
            fileId = photo.getFileId();
        } else if (message.hasDocument()) {
            fileId = message.getDocument().getFileId();
        } else {
            return new SendMessage(chatId.toString(),
                    "Не удалось обработать изображение. Пожалуйста, попробуйте отправить изображение еще раз.");
        }

        log.info("Processing image with fileId: {}", fileId);

        var file = telegramFileService.getFile(fileId);
        if (file == null) {
            log.error("Failed to download file for fileId: {}", fileId);
            return new SendMessage(chatId.toString(),
                    "Не удалось загрузить изображение. Пожалуйста, попробуйте отправить изображение еще раз.");
        }

        log.info("Downloaded file to: {}", file.getAbsolutePath());

        try {
            // Convert image to base64
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);
            log.info("Image converted to base64, length: {}", base64Image.length());

            userImageState.incrementImageCount(chatId);

            // Создаем объект изображения для истории
            Map<String, Object> imageObject = Map.of(
                "type", "image_url",
                "image_url", Map.of("url", "data:image/jpeg;base64," + base64Image)
            );

            // Добавляем изображение в историю
            chatGptHistoryService.addImageToHistory(chatId, imageObject);

            // Отправляем фото в GPT вместе с историей
            var gptGeneratedText = gptService.getResponseChatForUserWithImages(
                    chatId,
                    message.getCaption() != null ? message.getCaption() : "",
                    List.of(imageObject)
            );

            if (gptGeneratedText == null || gptGeneratedText.trim().isEmpty()) {
                log.error("Empty response from GPT for chatId: {}", chatId);
                return new SendMessage(chatId.toString(),
                        "Не удалось получить ответ от сервиса. Пожалуйста, попробуйте еще раз.");
            }

            return new SendMessage(chatId.toString(), gptGeneratedText);

        } catch (Exception e) {
            log.error("Error processing image", e);
            return new SendMessage(chatId.toString(),
                    "Произошла ошибка при обработке изображения. Пожалуйста, попробуйте еще раз.");
        }
    }
} 