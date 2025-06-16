package homebrew.secondstate.telegrambot.telegram;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;
import java.util.function.Supplier;

@Slf4j
@Service
public class TelegramAsyncMessageSender {

    private final DefaultAbsSender defaultAbsSender;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    public TelegramAsyncMessageSender(@Lazy DefaultAbsSender defaultAbsSender) {
        this.defaultAbsSender = defaultAbsSender;
    }

    @SneakyThrows
    public void sendMessageAsync(
            String chatId,
            Supplier<SendMessage> action,
            Function<Throwable, SendMessage> onErrorHandler
    ) {
        CompletableFuture.supplyAsync(action, executorService)
                .exceptionally(onErrorHandler)
                .thenAccept(sendMessage -> {
                    try {
                        List<String> parts = splitMessage(sendMessage.getText(), 4000);
                        for (String part : parts) {
                            SendMessage messagePart = SendMessage.builder()
                                    .chatId(chatId)
                                    .text(part)
                                    .parseMode(sendMessage.getParseMode())
                                    .replyMarkup(sendMessage.getReplyMarkup())
                                    .build();
                            defaultAbsSender.execute(messagePart);
                        }
                    } catch (TelegramApiException e) {
                        log.error("Error while sending message", e);
                    }
                });
    }

    private List<String> splitMessage(String message, int maxLength) {
        List<String> parts = new ArrayList<>();
        int start = 0;
        while (start < message.length()) {
            int end = Math.min(start + maxLength, message.length());
            parts.add(message.substring(start, end));
            start = end;
        }
        return parts;
    }
}
