package homebrew.secondstate.telegrambot.telegram;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
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
            log.info("Received update: {}", update);
            var method = telegramUpdateMessageHandler.handleMessage(update);
            if (method != null) {
                log.info("Sending response: {}", method);
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
