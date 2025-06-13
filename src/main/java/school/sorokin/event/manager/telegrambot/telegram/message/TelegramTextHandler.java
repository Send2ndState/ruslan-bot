package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;

@Service
@AllArgsConstructor
public class TelegramTextHandler {

    private final ChatGptService gptService;

    public SendMessage processTextMessage(Message message) {
        var text = message.getText();
        var chatId = message.getChatId();

        // Отправляем текст напрямую в GPT
        var gptGeneratedText = gptService.getResponseChatForUserWithImages(chatId, text, null);
        return new SendMessage(chatId.toString(), gptGeneratedText);
    }
}
