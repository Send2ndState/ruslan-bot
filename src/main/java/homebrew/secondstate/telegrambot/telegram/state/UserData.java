package homebrew.secondstate.telegrambot.telegram.state;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public record UserData(
    UserState state,
    String birthDate,
    String gender,
    boolean wantsDetailedAnalysis,
    List<String> questionAnswers,
    LocalDateTime lastAnalysisTime
) {
    public UserData {
        if (questionAnswers == null) {
            questionAnswers = new ArrayList<>();
        }
    }

    public UserData withState(UserState newState) {
        return new UserData(newState, birthDate, gender, wantsDetailedAnalysis, questionAnswers, lastAnalysisTime);
    }

    public UserData withBirthDate(String newBirthDate) {
        return new UserData(state, newBirthDate, gender, wantsDetailedAnalysis, questionAnswers, lastAnalysisTime);
    }

    public UserData withGender(String newGender) {
        return new UserData(state, birthDate, newGender, wantsDetailedAnalysis, questionAnswers, lastAnalysisTime);
    }

    public UserData withWantsDetailedAnalysis(boolean newWantsDetailedAnalysis) {
        return new UserData(state, birthDate, gender, newWantsDetailedAnalysis, questionAnswers, lastAnalysisTime);
    }

    public UserData withQuestionAnswer(String answer) {
        var newAnswers = new ArrayList<>(questionAnswers);
        newAnswers.add(answer);
        return new UserData(state, birthDate, gender, wantsDetailedAnalysis, newAnswers, lastAnalysisTime);
    }

    public UserData withLastAnalysisTime(LocalDateTime time) {
        return new UserData(state, birthDate, gender, wantsDetailedAnalysis, questionAnswers, time);
    }

    public static UserData initial() {
        return new UserData(UserState.INITIAL, null, null, false, new ArrayList<>(), null);
    }
} 