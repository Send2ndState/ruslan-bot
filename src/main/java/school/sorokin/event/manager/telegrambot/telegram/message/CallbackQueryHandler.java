package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final UserStateService userStateService;
    private final TelegramTextHandler telegramTextHandler;

    public SendMessage processCallbackQuery(CallbackQuery callbackQuery) {
        var chatId = callbackQuery.getMessage().getChatId();
        var data = callbackQuery.getData();
        var userData = userStateService.getUserData(chatId);

        return telegramTextHandler.processCallbackQuery(data, chatId, userData);
    }

    public AnswerCallbackQuery answerCallbackQuery(CallbackQuery callbackQuery) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .build();
    }
} 