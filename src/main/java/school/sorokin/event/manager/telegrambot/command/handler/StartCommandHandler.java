package school.sorokin.event.manager.telegrambot.command.handler;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.BotApiMethod;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import school.sorokin.event.manager.telegrambot.command.TelegramCommandHandler;
import school.sorokin.event.manager.telegrambot.command.TelegramCommands;
import school.sorokin.event.manager.telegrambot.telegram.state.UserState;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
@AllArgsConstructor
public class StartCommandHandler implements TelegramCommandHandler {

    private final UserStateService userStateService;

    private final String ARTICLE_TEXT = "ЭТО ТЕКСТ СТАТЬИ.";

    @Override
    public BotApiMethod<?> processCommand(Message message) {
        var chatId = message.getChatId();
        var userData = userStateService.getUserData(chatId);

        // Проверяем, был ли анализ сегодня
        if (userData.lastAnalysisTime() != null) {
            var hoursSinceLastAnalysis = ChronoUnit.HOURS.between(userData.lastAnalysisTime(), LocalDateTime.now());
            if (hoursSinceLastAnalysis < 24) {
                return new SendMessage(chatId.toString(), 
                    "Анализ можно пройти не чаще раза в сутки. Возвращайтесь завтра или попробуйте с другого аккаунта");
            }
        }

        userStateService.updateUserData(chatId, userData.withState(UserState.WAITING_BIRTH_DATE));
        return new SendMessage(chatId.toString(), "Добро пожаловать! Для начала анализа, пожалуйста, введите вашу дату рождения в формате ДД.ММ.ГГГГ");
    }

    @Override
    public TelegramCommands getSupportedCommand() {
        return TelegramCommands.START_COMMAND;
    }
}
