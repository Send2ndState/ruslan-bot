package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;
import school.sorokin.event.manager.telegrambot.telegram.state.UserData;
import school.sorokin.event.manager.telegrambot.telegram.state.UserState;
import school.sorokin.event.manager.telegrambot.telegram.state.UserStateService;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TelegramTextHandler {

    private final ChatGptService gptService;
    private final UserStateService userStateService;
    private final TelegramImageHandler imageHandler;

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
                                .text("Мужчина")
                                .callbackData("gender_male")
                                .build(),
                        InlineKeyboardButton.builder()
                                .text("Женщина")
                                .callbackData("gender_female")
                                .build()
                )))
                .build();

        return SendMessage.builder()
                .chatId(chatId)
                .text("Скажите, вы-")
                .replyMarkup(keyboard)
                .build();
    }

    private SendMessage handleGender(Long chatId, UserData userData, String text) {
        userStateService.updateUserData(chatId, userData.withGender(text).withState(UserState.WAITING_QUESTIONS_CHOICE));
        return SendMessage.builder()
                .chatId(chatId)
                .text("Для более точного анализа, нужно ответить еще на ряд вопросов, это поможет мне составить более подробный анализ")
                .replyMarkup(InlineKeyboardMarkup.builder()
                        .keyboard(List.of(
                                List.of(InlineKeyboardButton.builder()
                                        .text("Ответить на вопросы")
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
                "на котором помогу вас встроить смыслы в ваше дело и применить их в маркетинге и продажах\n\n" +
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
}
