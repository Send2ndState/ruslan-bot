package school.sorokin.event.manager.telegrambot.telegram.state;

public enum UserState {
    INITIAL,
    WAITING_FIRST_PALM,
    WAITING_SECOND_PALM,
    WAITING_BIRTH_DATE,
    WAITING_GENDER,
    WAITING_QUESTIONS_CHOICE,
    WAITING_QUESTIONS_ANSWERS,
    COMPLETED,
    ANALYSIS_COMPLETED
} 