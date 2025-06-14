package school.sorokin.event.manager.telegrambot.command.handler;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.command.TelegramCommandHandler;
import school.sorokin.event.manager.telegrambot.command.TelegramCommands;
import school.sorokin.event.manager.telegrambot.telegram.state.CommandStateService;

@Component
@AllArgsConstructor
public class StartCommandHandler implements TelegramCommandHandler {

    private final CommandStateService commandStateService;

    private final String HELLO_MESSAGE = """
            Привет %s,
            Для старта напишите свой пол и возраст, например: "Мужчина, 38 лет"
            Конфиденциальность переписки гарантированна.
            Общаться можно как текстом, так и голосом.
            Каждое сообщение запоминается для контекста.
            Очистить контекст можно с помощью команды /clear
            """;

    @Override
    public BotApiMethod<?> processCommand(Message message) {
        // Устанавливаем состояние команды /start в true
        commandStateService.setStartCommandState(message.getChatId(), true);
        
        return SendMessage.builder()
                .chatId(message.getChatId())
                .text(HELLO_MESSAGE.formatted(
                        message.getChat().getFirstName()
                ))
                .build();
    }

    @Override
    public TelegramCommands getSupportedCommand() {
        return TelegramCommands.START_COMMAND;
    }
}
