package school.sorokin.event.manager.telegrambot.telegram.state;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    public static final String[] QUESTIONS = {
        "Что вы чувствуете сейчас? (внутри: опустошение, тревога, напряжение, спокойствие, радость, раздражение, злость, тоска и т.д.)",
        "Есть ли у вас ощущение, что вы \"не на своём месте\"?",
        "Считает ли вы себя больше чувствующим или рациональным?",
        "Считает ли вы себя лидером, одиночкой или тенью для кого-то?",
        "Что для вас важнее: стабильность, свобода, развитие, признание?",
        "Что вы любите делать, даже если это не приносит денег?",
        "Что вы делаете «естественно» — как будто умеете от рождения?",
        "Часто ли вы чувствуете одиночество?",
        "Чувствует ли вы, что «пришли в этот мир зачем-то»?",
        "Чего вы боитесь сильнее всего в переменах?",
        "Есть ли ощущение, что \"поздно начинать что-то новое\"?"
    };

    private final Map<Long, UserData> userStates = new ConcurrentHashMap<>();

    public UserData getUserData(Long chatId) {
        return userStates.computeIfAbsent(chatId, k -> UserData.initial());
    }

    public void updateUserData(Long chatId, UserData userData) {
        userStates.put(chatId, userData);
    }

    public void resetUserData(Long chatId) {
        userStates.put(chatId, UserData.initial());
    }
} 