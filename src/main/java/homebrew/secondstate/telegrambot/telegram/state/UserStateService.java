package homebrew.secondstate.telegrambot.telegram.state;

import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserStateService {
    public static final String[] QUESTIONS = {
        "Что вы чувствуете сейчас? \uD83C\uDF27\uFE0F\uD83C\uDF24\uFE0F\n" +
                "(внутри: опустошение, тревога, напряжение, спокойствие, радость, раздражение, злость, тоска и т.д.)",
        "Есть ли у вас ощущение, что вы \"не на своём месте\"? \uD83E\uDDED\n" +
                "(Если да — как это проявляется в жизни?)",
        "Считаете ли вы себя больше чувствующим или рациональным? \uD83E\uDDE0❤\uFE0F\n" +
                "(Что преобладает в ваших решениях и восприятии?)",
        "Считаете ли вы себя лидером, одиночкой или тенью для кого-то? \uD83E\uDDCD\u200D♂\uFE0F\uD83C\uDF1F\n" +
                "(Что ближе вам по ощущению в отношениях с другими?)",
        "Что для вас важнее: стабильность, свобода, развитие или признание? ⚖\uFE0F\uD83C\uDF31\n" +
                "(Выберите то, что сейчас отзывается сильнее всего.)",
        "Что вы любите делать, даже если это не приносит денег? \uD83C\uDFA8\uD83D\uDCAD\n" +
                "(Вспомните занятия, в которых вы теряете счёт времени.)",
        "Что вы делаете «естественно» — как будто умеете от рождения? ✨\n" +
                "(То, что получается легко, будто само собой — без обучения и усилий.)",
        "Часто ли вы чувствуете одиночество? \uD83C\uDF19\uD83D\uDCAD\n" +
                "(Бывает ли, что чувствуете себя непонятым или отдалённым от других?)",
        "Чувствуете ли вы, что «пришли в этот мир зачем-то»? \uD83C\uDF0C✨\n" +
                "(Есть ли ощущение, что у вашей жизни есть глубокий смысл или предназначение?)",
        "Чего вы боитесь сильнее всего в переменах? \uD83D\uDE28\uD83D\uDD04\n" +
                "(Какие страхи или сомнения останавливают вас при попытке что-то изменить?)",
        "Есть ли ощущение, что \"поздно начинать что-то новое\"? ⏳\uD83C\uDF31\n" +
                "(Если да — в каких сферах это особенно чувствуется?)"
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