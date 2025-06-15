package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;
import school.sorokin.event.manager.telegrambot.telegram.TelegramFileService;
import school.sorokin.event.manager.telegrambot.telegram.state.UserState;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

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

    private final ChatGptService gptService;
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
                    "Не удалось обработать изображение. Пожалуйста, попробуйте отправить изображение еще раз.");
        }

        var file = telegramFileService.getFile(fileId);
        if (file == null) {
            return new SendMessage(chatId.toString(),
                    "Не удалось загрузить изображение. Пожалуйста, попробуйте отправить изображение еще раз.");
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
                        .text("Отлично! Теперь отправьте фотографию второй ладони")
                        .build();
            }
            // Если это второе изображение, переходим к запросу даты рождения
            else if (currentAnalysisImages.get(chatId).size() == 2) {
                userStateService.updateUserData(chatId, userData.withState(UserState.WAITING_BIRTH_DATE));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Спасибо! Теперь, пожалуйста, напишите вашу дату рождения в формате ДД.ММ.ГГГГ")
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
                    "Произошла ошибка при обработке изображения. Пожалуйста, попробуйте еще раз.");
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