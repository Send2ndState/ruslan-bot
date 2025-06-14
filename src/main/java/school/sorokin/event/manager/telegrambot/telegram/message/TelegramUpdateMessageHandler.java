package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.command.TelegramCommandsDispatcher;
import school.sorokin.event.manager.telegrambot.telegram.TelegramAsyncMessageSender;

@Slf4j
@Service
@AllArgsConstructor
public class TelegramUpdateMessageHandler {

    private final TelegramCommandsDispatcher telegramCommandsDispatcher;
    private final TelegramAsyncMessageSender telegramAsyncMessageSender;
    private final TelegramTextHandler telegramTextHandler;
    private final TelegramVoiceHandler telegramVoiceHandler;
    private final TelegramImageHandler telegramImageHandler;
    private final TelegramVideoHandler telegramVideoHandler;

    public BotApiMethod<?> handleMessage(Message message) {
        log.info("Start message processing: message={}", message);
        if (telegramCommandsDispatcher.isCommand(message)) {
            return telegramCommandsDispatcher.processCommand(message);
        }
        var chatId = message.getChatId().toString();

        if (message.hasVoice() || message.hasText() || message.hasPhoto() || message.hasVideo() || 
            (message.hasDocument() && isImageDocument(message))) {
            telegramAsyncMessageSender.sendMessageAsync(
                    chatId,
                    () -> handleMessageAsync(message),
                    (throwable) -> getErrorMessage(throwable, chatId)
            );
        }
        return null;
    }

    private boolean isImageDocument(Message message) {
        if (!message.hasDocument()) {
            return false;
        }
        String mimeType = message.getDocument().getMimeType();
        return mimeType != null && (
            mimeType.startsWith("image/jpeg") || 
            mimeType.startsWith("image/jpg") || 
            mimeType.startsWith("image/png") || 
            mimeType.startsWith("image/gif")
        );
    }

    private SendMessage handleMessageAsync(Message message) {
        SendMessage result;
        if (message.hasVoice()) {
            result = telegramVoiceHandler.processVoice(message);
        } else if (message.hasPhoto() || (message.hasDocument() && isImageDocument(message))) {
            result = telegramImageHandler.processImage(message);
        } else if (message.hasVideo()) {
            result = telegramVideoHandler.processVideo(message);
        } else {
            result = telegramTextHandler.processTextMessage(message);
        }

        result.setParseMode(ParseMode.MARKDOWNV2);
        return result;
    }

    private SendMessage getErrorMessage(Throwable throwable, String chatId) {
        log.error("Произошла ошибка, chatId={}", chatId, throwable);
        return SendMessage.builder()
                .chatId(chatId)
                .text("Произошла ошибка, попробуйте еще раз")
                .build();
    }

}
