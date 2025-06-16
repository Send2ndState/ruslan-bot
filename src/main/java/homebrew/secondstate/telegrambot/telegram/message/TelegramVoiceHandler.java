package homebrew.secondstate.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import homebrew.secondstate.telegrambot.openai.TranscribeVoiceToTextService;
import homebrew.secondstate.telegrambot.telegram.TelegramFileService;
import homebrew.secondstate.telegrambot.telegram.state.UserState;
import homebrew.secondstate.telegrambot.telegram.state.UserStateService;

@Service
@RequiredArgsConstructor
public class TelegramVoiceHandler {

    private final TelegramFileService telegramFileService;
    private final TranscribeVoiceToTextService transcribeVoiceToTextService;
    private final UserStateService userStateService;
    private final ApplicationContext context;

    public TelegramTextHandler getTelegramTextHandler() {
        return context.getBean(TelegramTextHandler.class);
    }

    public SendMessage processVoice(Message message) {
        var chatId = message.getChatId();
        var userData = userStateService.getUserData(chatId);

        if (userData.state() == UserState.ANALYSIS_COMPLETED) {
            return new SendMessage(chatId.toString(),
                    "Анализ завершен, жду вас в @mozibiz, чтобы провести еще один анализ напишите /start");
        }

        var voice = message.getVoice();
        var audio = message.getAudio();

        if (voice == null && audio == null) {
            return new SendMessage(chatId.toString(), "Не удалось распознать голосовое сообщение 🙁");
        }

        String fileId = voice != null ? voice.getFileId() : audio.getFileId();
        var file = telegramFileService.getFile(fileId);
        var text = transcribeVoiceToTextService.transcribe(file);

        message.setText(text);
        return getTelegramTextHandler().processTextMessage(message);
    }
}
