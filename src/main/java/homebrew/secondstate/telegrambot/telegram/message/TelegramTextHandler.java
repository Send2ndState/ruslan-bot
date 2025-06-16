package homebrew.secondstate.telegrambot.telegram.message;

import homebrew.secondstate.telegrambot.telegram.TelegramBot;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import homebrew.secondstate.telegrambot.openai.ChatGptService;
import homebrew.secondstate.telegrambot.telegram.state.UserData;
import homebrew.secondstate.telegrambot.telegram.state.UserState;
import homebrew.secondstate.telegrambot.telegram.state.UserStateService;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class TelegramTextHandler {

    private final ChatGptService gptService;
    private final UserStateService userStateService;
    private final TelegramImageHandler imageHandler;
    @Lazy
    private final TelegramBot telegramBot;

    @Value("${gpt.system-prompt-questions}")
    private String systemPromptWithQuestions;

    @Value("${gpt.system-prompt-no-questions}")
    private String systemPromptWithoutQuestions;

    @Value("${gpt.user-text-input}")
    private String userTextInput;

    public SendMessage processTextMessage(Message message) {
        var text = message.getText();
        var chatId = message.getChatId();
        var userData = userStateService.getUserData(chatId);

        // Если анализ уже завершен, отправляем сообщение о записи на созвон
        if (userData.state() == UserState.ANALYSIS_COMPLETED) {
            return new SendMessage(chatId.toString(), 
                "Анализ завершен, жду вас в @mozibiz, чтобы провести еще один анализ напишите /start");
        }

        switch (userData.state()) {
            case WAITING_BIRTH_DATE:
                return handleBirthDate(chatId, text);
            case WAITING_GENDER:
                return handleGender(chatId, userData, text);
            case WAITING_QUESTIONS_ANSWERS:
                return handleQuestionAnswer(chatId, userData, text);
            default:
                return new SendMessage(chatId.toString(), "Пожалуйста, следуйте инструкциям бота.");
        }
    }

    private SendMessage handleBirthDate(Long chatId, String text) {
        userStateService.updateUserData(chatId, userStateService.getUserData(chatId).withBirthDate(text).withState(UserState.WAITING_GENDER));

        var keyboard = InlineKeyboardMarkup.builder()
                .keyboard(List.of(List.of(
                        InlineKeyboardButton.builder()
                                .text("\uD83E\uDDD1 Мужчина")
                                .callbackData("gender_male")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("\uD83D\uDC69 Женщина")
                                .callbackData("gender_female")
                                .build()
                )))
                .build();

        return SendMessage.builder()
                .chatId(chatId)
                .text("⬇️ Скажите, вы -")
                .replyMarkup(keyboard)
                .build();
    }

    private SendMessage handleGender(Long chatId, UserData userData, String text) {
        userStateService.updateUserData(chatId, userData.withGender(text).withState(UserState.WAITING_QUESTIONS_CHOICE));
        return SendMessage.builder()
                .chatId(chatId)
                .text("\uD83D\uDCDD Чтобы сделать анализ точнее и глубже, мне нужно задать вам несколько коротких вопросов.\n" +
                        "Они помогут лучше почувствовать вашу историю и собрать важные штрихи к портрету.")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(
                                List.of(InlineKeyboardButton.builder()
                                        .text("✅ Ответить на вопросы (отвечать можно голосовыми)")
                                        .callbackData("answer_questions")
                                        .build()),
                                List.of(InlineKeyboardButton.builder()
                                        .text("Не хочу вопросы - готов получить приблизительный анализ")
                                        .callbackData("skip_questions")
                                        .build())
                        ))
                        .build())
                .build();
    }

    private SendMessage handleQuestionAnswer(Long chatId, UserData userData, String text) {
        var newUserData = userData.withQuestionAnswer(text);
        userStateService.updateUserData(chatId, newUserData);

        if (newUserData.questionAnswers().size() < UserStateService.QUESTIONS.length) {
            return SendMessage.builder()
                    .chatId(chatId)
                    .text(UserStateService.QUESTIONS[newUserData.questionAnswers().size()])
                    .build();
        } else {
            // Все вопросы отвечены, отправляем анализ
            try {
                telegramBot.execute(new SendMessage(chatId.toString(), "\uD83D\uDD0D Провожу анализ… минутку"));
            } catch (TelegramApiException e) {
                log.error("Ошибка при отправке ожидающего сообщения (после вопросов)", e);
            }
            String prompt = buildPrompt(newUserData);
            var images = imageHandler.getCurrentAnalysisImages(chatId);
            var gptResponse = gptService.getResponseChatForUserWithImages(chatId, userTextInput, prompt,
                images.stream()
                    .map(imageUrl -> Map.<String, Object>of("url", imageUrl))
                    .toList());
            
            // Обновляем время последнего анализа и состояние
            userStateService.updateUserData(chatId, 
                newUserData.withLastAnalysisTime(LocalDateTime.now())
                          .withState(UserState.ANALYSIS_COMPLETED));

            // Очищаем изображения текущего анализа
            imageHandler.clearCurrentAnalysisImages(chatId);

            // Добавляем финальное сообщение
            String finalMessage = gptResponse + "\n\n" +
                "Если хотите больше ясности и конкретных шагов — приглашаю вас на созвон, " +
                "на котором помогу вам встроить смыслы в ваше дело и применить их в маркетинге и продажах\n\n" +
                "Для того, чтобы записаться на созвон, пишите в личные сообщения в @mozibiz слово «смысл»";

            return SendMessage.builder()
                    .chatId(chatId)
                    .text(finalMessage)
                    .build();
        }
    }

    private String buildPrompt(UserData userData) {
        String basePrompt = userData.wantsDetailedAnalysis() ? systemPromptWithQuestions : systemPromptWithoutQuestions;
        
        if (userData.wantsDetailedAnalysis()) {
            return basePrompt.formatted(
                userData.gender(),
                userData.birthDate(),
                userData.questionAnswers().get(0),
                userData.questionAnswers().get(1),
                userData.questionAnswers().get(2),
                userData.questionAnswers().get(3),
                userData.questionAnswers().get(4),
                userData.questionAnswers().get(5),
                userData.questionAnswers().get(6),
                userData.questionAnswers().get(7),
                userData.questionAnswers().get(8),
                userData.questionAnswers().get(9),
                userData.questionAnswers().get(10)
            );
        } else {
            return basePrompt.formatted(userData.gender(), userData.birthDate());
        }
    }

    public SendMessage processCallbackQuery(String callbackData, Long chatId, UserData userData) {
        switch (callbackData) {
            case "continue_after_article":
                userStateService.updateUserData(chatId, userData.withState(UserState.WAITING_FIRST_PALM));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("\uD83D\uDE0A Отлично! Тогда, пожалуйста, пришлите фотографии ваших ладоней с внутренней стороны. Сперва одну, потом вторую.\n" +
                                "Сделайте снимок так, чтобы были хорошо видны линии \uD83D\uDE4C")
                        .build();

            case "gender_male":
            case "gender_female":
                String gender = callbackData.equals("gender_male") ? "Мужчина" : "Женщина";
                userStateService.updateUserData(chatId, userData.withGender(gender).withState(UserState.WAITING_QUESTIONS_CHOICE));
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("\uD83D\uDCDD Чтобы сделать анализ точнее и глубже, мне нужно задать вам несколько коротких вопросов. Они помогут лучше почувствовать вашу историю и собрать важные штрихи к портрету.")
                        .replyMarkup(InlineKeyboardMarkup.builder()
                                .keyboard(List.of(
                                        List.of(InlineKeyboardButton.builder()
                                                .text("✅ Ответить на вопросы (отвечать можно голосовыми)")
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
                try {
                    telegramBot.execute(new SendMessage(chatId.toString(), "\uD83D\uDD0D Провожу анализ… минутку"));
                } catch (TelegramApiException e) {
                    log.error("Ошибка при отправке ожидающего сообщения (скип вопросов)", e);
                }
                userStateService.updateUserData(chatId, userData.withWantsDetailedAnalysis(false).withState(UserState.COMPLETED));
                String prompt = buildPrompt(userData);
                var images = imageHandler.getCurrentAnalysisImages(chatId);
                var gptResponse = gptService.getResponseChatForUserWithImages(chatId, userTextInput, prompt,
                    images.stream()
                        .map(imageUrl -> Map.<String, Object>of("url", imageUrl))
                        .toList());
                
                userStateService.updateUserData(chatId, 
                    userData.withLastAnalysisTime(LocalDateTime.now())
                          .withState(UserState.ANALYSIS_COMPLETED));

                imageHandler.clearCurrentAnalysisImages(chatId);

                String finalMessage = gptResponse + "\n\n" +
                    "Если хотите больше ясности и конкретных шагов — приглашаю вас на созвон, " +
                    "на котором помогу вам встроить смыслы в ваше дело и применить их в маркетинге и продажах\n\n" +
                    "Для того, чтобы записаться на созвон, пишите в личные сообщения в @mozibiz слово «смысл»";

                return SendMessage.builder()
                        .chatId(chatId)
                        .text(finalMessage)
                        .build();
            default:
                return SendMessage.builder()
                        .chatId(chatId)
                        .text("Пожалуйста, следуйте инструкциям бота.")
                        .build();
        }
    }
}
