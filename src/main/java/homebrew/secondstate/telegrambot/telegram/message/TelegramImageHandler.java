package homebrew.secondstate.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import homebrew.secondstate.telegrambot.telegram.TelegramFileService;
import homebrew.secondstate.telegrambot.telegram.state.UserState;
import homebrew.secondstate.telegrambot.telegram.state.UserStateService;

import java.util.Base64;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@AllArgsConstructor
public class TelegramImageHandler {

    private final TelegramFileService telegramFileService;
    private final UserStateService userStateService;

    // Временное хранилище для изображений текущего анализа
    private final Map<Long, List<String>> currentAnalysisImages = new ConcurrentHashMap<>();

    public SendMessage processImage(Message message) {
        var chatId = message.getChatId();
        var userData = userStateService.getUserData(chatId);

        // Если анализ уже завершен, отправляем сообщение о записи на созвон
        if (userData.state() == UserState.ANALYSIS_COMPLETED) {
            return new SendMessage(chatId.toString(), 
                "Анализ завершен, жду вас в @mozibiz, чтобы провести еще один анализ напишите /start");
        }

        String fileId;
        if (message.hasPhoto()) {
            var photos = message.getPhoto();
            var photo = photos.get(photos.size() / 2); // Take middle quality photo
            fileId = photo.getFileId();
        } else if (message.hasDocument()) {
            fileId = message.getDocument().getFileId();
        } else {
            return new SendMessage(chatId.toString(),
                    "Прошу прощения, не могли бы вы отправить это фото еще раз?");
        }

        var file = telegramFileService.getFile(fileId);
        if (file == null) {
            return new SendMessage(chatId.toString(),
                    "Прошу прощения, не могли бы вы отправить это фото еще раз?");
        }

        try {
            byte[] fileContent = Files.readAllBytes(file.toPath());
            String base64Image = Base64.getEncoder().encodeToString(fileContent);

            // Добавляем изображение во временное хранилище
            currentAnalysisImages.computeIfAbsent(chatId, k -> new ArrayList<>()).add(base64Image);

            // Если это первое изображение, запрашиваем второе
            if (currentAnalysisImages.get(chatId).size() == 1) {
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("✨ Замечательно! Теперь, пожалуйста, пришлите фотографию второй ладони — тоже внутреннюю сторону, чтобы хорошо были видны линии \uD83D\uDE4C")
                        .build();
            }
            // Если это второе изображение, переходим к запросу даты рождения
            else if (currentAnalysisImages.get(chatId).size() == 2) {
                userStateService.updateUserData(chatId, userData.withState(UserState.WAITING_BIRTH_DATE));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("\uD83C\uDF1F Спасибо! А теперь напишите, пожалуйста, вашу дату рождения — в формате дд.мм.гггг (например, 14.06.1995).")
                        .build();
            }
            // Если изображений больше двух, игнорируем
            else {
                return new SendMessage(chatId.toString(),
                        "Пожалуйста, следуйте инструкциям бота.");
            }
        } catch (Exception e) {
            log.error("Error processing image", e);
            return new SendMessage(chatId.toString(),
                    "Прошу прощения, не могли бы вы отправить это фото еще раз?");
        }
    }

    // Метод для получения изображений текущего анализа
    public List<String> getCurrentAnalysisImages(Long chatId) {
        return currentAnalysisImages.getOrDefault(chatId, new ArrayList<>());
    }

    // Метод для очистки изображений после завершения анализа
    public void clearCurrentAnalysisImages(Long chatId) {
        currentAnalysisImages.remove(chatId);
    }
} 