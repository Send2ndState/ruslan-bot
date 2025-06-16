package homebrew.secondstate.telegrambot.telegram;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import homebrew.secondstate.telegrambot.telegram.message.TelegramUpdateMessageHandler;

@Slf4j
@Component
public class TelegramBot extends TelegramLongPollingBot {

    private final TelegramUpdateMessageHandler telegramUpdateMessageHandler;

    public TelegramBot(
            @Value("${bot.token}") String botToken,
            TelegramUpdateMessageHandler telegramUpdateMessageHandler
    ) {
        super(new DefaultBotOptions(), botToken);
        this.telegramUpdateMessageHandler = telegramUpdateMessageHandler;
    }

    @SneakyThrows
    @Override
    public void onUpdateReceived(Update update) {
        try {
            if (update.hasMessage() && update.getMessage() != null) {
                var message = update.getMessage();

                Long chatId = message.getChatId();
                String username = message.getFrom() != null ? message.getFrom().getUserName() : "unknown";
                String text = message.getText();
                var voice = message.getVoice() != null ? "Contains" : null;
                var photo = message.getPhoto() != null ? "Contains" : null;
                var document = message.getDocument() != null ? "Contains" : null;
                if (text != null && text.startsWith("/")) {
                    log.info("Получена команда. ChatId: {}, Username: {}, Text: {}", chatId, username, text);
                } else {
                    log.info("Получено сообщение. ChatId: {}, Username: {}, Text: {}, Voice: {}, Photo: {}, Document: {}",
                            chatId, username, text, voice, photo, document);
                }
            } else if (update.hasCallbackQuery() && update.getCallbackQuery() != null) {
                var callback = update.getCallbackQuery();
                var message = callback.getMessage();

                Long chatId = message != null ? message.getChatId() : null;
                String username = callback.getFrom() != null ? callback.getFrom().getUserName() : "unknown";
                String data = callback.getData();

                log.info("Нажали на кнопку. ChatId: {}, Username: {}, CallbackData: {}",
                        chatId, username, data);
            }

            var method = telegramUpdateMessageHandler.handleMessage(update);
            if (method != null) {
                if (method instanceof SendMessage sendMessage) {
                    log.info("Ответ от бота -> chatId: {}, text: {}", sendMessage.getChatId(), sendMessage.getText());
                } else if (method instanceof EditMessageText editMessage) {
                    log.info("Ответ от бота (edit) -> chatId: {}, text: {}", editMessage.getChatId(), editMessage.getText());
                } else {
                    log.info("Ответ от бота: {}", method);
                }
                sendApiMethod(method);
            }
        } catch (Exception e) {
            log.error("Error while processing update", e);
            if (update.hasMessage()) {
                sendUserErrorMessage(update.getMessage().getChatId());
            } else if (update.hasCallbackQuery()) {
                sendUserErrorMessage(update.getCallbackQuery().getMessage().getChatId());
            }
        }
    }

    private void sendUserErrorMessage(Long chatId) {
        try {
            execute(SendMessage.builder()
                    .chatId(chatId)
                    .text("Произошла ошибка при обработке сообщения. Пожалуйста, попробуйте еще раз.")
                    .build());
        } catch (Exception e) {
            log.error("Error while sending error message", e);
        }
    }

    @Override
    public String getBotUsername() {
        return "maminoi_podrugi_bot";
    }
}
