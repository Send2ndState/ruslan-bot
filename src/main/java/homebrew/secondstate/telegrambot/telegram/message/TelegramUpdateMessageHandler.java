package homebrew.secondstate.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import homebrew.secondstate.telegrambot.command.TelegramCommandsDispatcher;
import homebrew.secondstate.telegrambot.telegram.TelegramAsyncMessageSender;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramUpdateMessageHandler {

    private final TelegramCommandsDispatcher telegramCommandsDispatcher;
    private final TelegramAsyncMessageSender telegramAsyncMessageSender;
    private final TelegramVoiceHandler telegramVoiceHandler;
    private final TelegramImageHandler telegramImageHandler;
    private final TelegramVideoHandler telegramVideoHandler;
    private final ApplicationContext context;

    public TelegramTextHandler getTelegramTextHandler() {
        return context.getBean(TelegramTextHandler.class);
    }

    public CallbackQueryHandler getCallbackQueryHandler() {
        return context.getBean(CallbackQueryHandler.class);
    }

    public BotApiMethod<?> handleMessage(Update update) {
        if (update.hasCallbackQuery()) {
            var callbackQuery = update.getCallbackQuery();
            var response = getCallbackQueryHandler().processCallbackQuery(callbackQuery);
            getCallbackQueryHandler().answerCallbackQuery(callbackQuery);
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
            return getTelegramTextHandler().processTextMessage(message);
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
