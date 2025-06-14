package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;
import school.sorokin.event.manager.telegrambot.telegram.state.CommandStateService;

@Service
@AllArgsConstructor
public class TelegramTextHandler {

    private final ChatGptService gptService;
    private final CommandStateService commandStateService;
    private final FirstMessageHandler firstMessageHandler;

    public SendMessage processTextMessage(Message message) {
        var text = message.getText();
        var chatId = message.getChatId();

        // Проверяем, активна ли команда /start
//        if (commandStateService.isStartCommandActive(chatId)) {
//            // Сбрасываем состояние команды /start
//            commandStateService.clearStartCommandState(chatId);
//            // Обрабатываем первое сообщение
//            return firstMessageHandler.handleFirstMessage(message);
//        }

        // Обычная обработка сообщения
        var gptGeneratedText = gptService.getResponseChatForUserWithImages(chatId, text, null);
        return new SendMessage(chatId.toString(), gptGeneratedText);
    }
}
