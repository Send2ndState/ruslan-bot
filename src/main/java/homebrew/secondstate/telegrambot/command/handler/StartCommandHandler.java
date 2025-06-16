package homebrew.secondstate.telegrambot.command.handler;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import homebrew.secondstate.telegrambot.command.TelegramCommandHandler;
import homebrew.secondstate.telegrambot.command.TelegramCommands;
import homebrew.secondstate.telegrambot.telegram.state.UserStateService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@RequiredArgsConstructor
public class StartCommandHandler implements TelegramCommandHandler {

    private final UserStateService userStateService;

    @Value("${tg.article-text}")
    private String articleText;

    @Override
    public BotApiMethod<?> processCommand(Message message) {
        var chatId = message.getChatId();
        var userData = userStateService.getUserData(chatId);

        // Проверяем, был ли анализ сегодня
        if (userData.lastAnalysisTime() != null) {
            var hoursSinceLastAnalysis = ChronoUnit.HOURS.between(userData.lastAnalysisTime(), LocalDateTime.now());
            if (hoursSinceLastAnalysis < 24) {
                return SendMessage.builder()
                    .chatId(chatId.toString())
                    .text("Анализ можно пройти не чаще раза в сутки. Возвращайтесь завтра или попробуйте с другого аккаунта")
                    .build();
            }
        }

        // Создаем клавиатуру с кнопкой "Продолжить"
        var keyboard = InlineKeyboardMarkup.builder()
            .keyboard(List.of(
                List.of(InlineKeyboardButton.builder()
                    .text("Продолжить \uD83E\uDDD8\u200D♂\uFE0F")
                    .callbackData("continue_after_article")
                    .build())
            ))
            .build();

        // Отправляем статью с кнопкой
        return SendMessage.builder()
            .chatId(chatId.toString())
            .text(articleText)
            .replyMarkup(keyboard)
            .build();
    }

    @Override
    public TelegramCommands getSupportedCommand() {
        return TelegramCommands.START_COMMAND;
    }
}
