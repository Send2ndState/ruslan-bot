package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.TranscribeVoiceToTextService;
import school.sorokin.event.manager.telegrambot.telegram.TelegramFileService;
import school.sorokin.event.manager.telegrambot.telegram.state.UserState;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

@Service
@RequiredArgsConstructor
public class TelegramVoiceHandler {

    private final TelegramFileService telegramFileService;
    private final TranscribeVoiceToTextService transcribeVoiceToTextService;
    private final UserStateService userStateService;
    private final TelegramTextHandler textHandler;

    public SendMessage processVoice(Message message) {
        var chatId = message.getChatId();
        var voice = message.getVoice();
        var userData = userStateService.getUserData(chatId);

        // Если анализ уже завершен, отправляем сообщение о записи на созвон
        if (userData.state() == UserState.ANALYSIS_COMPLETED) {
            return new SendMessage(chatId.toString(), 
                "Анализ завершен, жду вас в @mozibiz, чтобы провести еще один анализ напишите /start");
        }

        var fileId = voice.getFileId();
        var file = telegramFileService.getFile(fileId);
        var text = transcribeVoiceToTextService.transcribe(file);

        // Создаем новое текстовое сообщение с транскрибированным текстом
        message.setText(text);
        
        // Передаем обработку текстовому обработчику
        return textHandler.processTextMessage(message);
    }
}
