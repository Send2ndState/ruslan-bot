package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.AnswerCallbackQuery;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import school.sorokin.event.manager.telegrambot.telegram.state.UserData;
import school.sorokin.event.manager.telegrambot.telegram.state.UserState;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CallbackQueryHandler {

    private final UserStateService userStateService;

    public SendMessage processCallbackQuery(CallbackQuery callbackQuery) {
        var chatId = callbackQuery.getMessage().getChatId();
        var data = callbackQuery.getData();
        var userData = userStateService.getUserData(chatId);

        switch (data) {
            case "continue_after_article":
                userStateService.updateUserData(chatId, userData.withState(UserState.WAITING_FIRST_PALM));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Отлично! Теперь отправьте фотографию первой ладони")
                        .build();

            case "gender_male":
                userStateService.updateUserData(chatId, userData.withGender("Мужчина").withState(UserState.WAITING_QUESTIONS_CHOICE));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Для более точного анализа, нужно ответить еще на ряд коротких вопросов, это поможет мне составить более подробный анализ")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of(
                                        List.of(InlineKeyboardButton.builder()
                                                .text("Ответить на вопросы (отвечать можно голосовыми)")
                                                .callbackData("answer_questions")
                                                .build()),
                                        List.of(InlineKeyboardButton.builder()
                                                .text("Не хочу вопросы - готов получить приблизительный анализ")
                                                .callbackData("skip_questions")
                                                .build())
                                ))
                                .build())
                        .build();

            case "gender_female":
                userStateService.updateUserData(chatId, userData.withGender("Женщина").withState(UserState.WAITING_QUESTIONS_CHOICE));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Для более точного анализа, нужно ответить еще на ряд коротких вопросов, это поможет мне составить более подробный анализ")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of(
                                        List.of(InlineKeyboardButton.builder()
                                                .text("Ответить на вопросы (отвечать можно голосовыми)")
                                                .callbackData("answer_questions")
                                                .build()),
                                        List.of(InlineKeyboardButton.builder()
                                                .text("Не хочу вопросы - готов получить приблизительный анализ")
                                                .callbackData("skip_questions")
                                                .build())
                                ))
                                .build())
                        .build();

            case "answer_questions":
                userStateService.updateUserData(chatId, userData.withWantsDetailedAnalysis(true).withState(UserState.WAITING_QUESTIONS_ANSWERS));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text(UserStateService.QUESTIONS[0])
                        .build();

            case "skip_questions":
                userStateService.updateUserData(chatId, userData.withWantsDetailedAnalysis(false).withState(UserState.WAITING_QUESTIONS_ANSWERS));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Как скажете. Провожу анализ, минутку...")
                        .build();

            default:
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Пожалуйста, используйте кнопки для навигации.")
                        .build();
        }
    }

    public AnswerCallbackQuery answerCallbackQuery(CallbackQuery callbackQuery) {
        return AnswerCallbackQuery.builder()
                .callbackQueryId(callbackQuery.getId())
                .build();
    }
} 