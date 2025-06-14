package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;

@Service
@AllArgsConstructor
public class TelegramVideoHandler {

    public SendMessage processVideo(Message message) {
        var chatId = message.getChatId();
        return new SendMessage(chatId.toString(), 
            "Извините, я пока не умею обрабатывать видео. Пожалуйста, отправьте текстовое сообщение, изображение или голосовое сообщение.");
    }
} 