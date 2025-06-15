package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.ParseMode;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import school.sorokin.event.manager.telegrambot.command.TelegramCommandsDispatcher;
import school.sorokin.event.manager.telegrambot.telegram.TelegramAsyncMessageSender;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateMessageHandler {

    private final TelegramCommandsDispatcher telegramCommandsDispatcher;
    private final TelegramAsyncMessageSender telegramAsyncMessageSender;
    private final TelegramTextHandler telegramTextHandler;
    private final TelegramVoiceHandler telegramVoiceHandler;
    private final TelegramImageHandler telegramImageHandler;
    private final TelegramVideoHandler telegramVideoHandler;
    private final CallbackQueryHandler callbackQueryHandler;

    public BotApiMethod<?> handleMessage(Update update) {
        log.info("Start message processing: update={}", update);
        
        if (update.hasCallbackQuery()) {
            log.info("Processing callback query: {}", update.getCallbackQuery().getData());
            var callbackQuery = update.getCallbackQuery();
            var response = callbackQueryHandler.processCallbackQuery(callbackQuery);
            callbackQueryHandler.answerCallbackQuery(callbackQuery);
            return response;
        }

        if (!update.hasMessage()) {
            return null;
        }

        var message = update.getMessage();
        if (telegramCommandsDispatcher.isCommand(message)) {
            return telegramCommandsDispatcher.processCommand(message);
        }

        var chatId = message.getChatId().toString();

        if (message.hasVoice() || message.hasText() || message.hasPhoto() || message.hasVideo() || 
            (message.hasDocument() && isImageDocument(message))) {
            telegramAsyncMessageSender.sendMessageAsync(
                    chatId,
                    () -> (SendMessage) handleMessageAsync(message),
                    (throwable) -> getErrorMessage(throwable, chatId)
            );
        }
        return null;
    }

    private BotApiMethod<?> handleMessageAsync(Message message) {
        if (message.hasVoice()) {
            return telegramVoiceHandler.processVoice(message);
        } else if (message.hasText()) {
            return telegramTextHandler.processTextMessage(message);
        } else if (message.hasPhoto() || (message.hasDocument() && isImageDocument(message))) {
            return telegramImageHandler.processImage(message);
        } else if (message.hasVideo()) {
            return telegramVideoHandler.processVideo(message);
        }
        return null;
    }

    private SendMessage getErrorMessage(Throwable throwable, String chatId) {
        log.error("Error while processing message", throwable);
        return SendMessage.builder()
                .chatId(chatId)
                .text("Произошла ошибка при обработке сообщения. Пожалуйста, попробуйте еще раз.")
                .build();
    }

    private boolean isImageDocument(Message message) {
        var document = message.getDocument();
        if (document == null) return false;
        var mimeType = document.getMimeType();
        return mimeType != null && mimeType.startsWith("image/");
    }
}
