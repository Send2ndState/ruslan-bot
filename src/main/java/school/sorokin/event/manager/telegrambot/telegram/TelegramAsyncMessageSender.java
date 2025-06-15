package school.sorokin.event.manager.telegrambot.telegram;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

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
                        defaultAbsSender.execute(sendMessage);
                    } catch (TelegramApiException e) {
                        log.error("Error while sending message", e);
                    }
                });
    }
}
