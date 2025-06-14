package school.sorokin.event.manager.telegrambot.telegram.message;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import school.sorokin.event.manager.telegrambot.openai.ChatGptService;

@Service
@AllArgsConstructor
public class FirstMessageHandler {

    private final ChatGptService gptService;

    public SendMessage handleFirstMessage(Message message) {
        var text = message.getText();
        var chatId = message.getChatId();

        String prompt = """
            Ты эксперт на стыке психологии, коучинга и образного анализа. К тебе обращается %s, который(ая) устал(а) не понимать, кто он(а), в чём его(её) сила и куда ему(ей) идти. Он(а) чувствует, что не реализован(а), как будто живёт не своей жизнью. Он(а) хочет понять, в чём его(её) талант, какова его(её) настоящая природа и предназначение.

            Твоя задача — глядя на фото его(её) ладоней:
            1. Увидеть в них историю. Что заложено с рождения? Какие черты характера, сильные и слабые стороны, скрытые дары?
            2. Описать его(её) глубинный потенциал. Не эзотерика, а вдохновляющий психологический анализ.
            3. Подсказать, куда двигаться. В чём может быть реализация? Какой путь может быть ему(ей) ближе всего по складу личности?
            4. Сделай это тепло, точно, с сочувствием и уважением. Как будто ты — мудрый наставник, который помог тысячам людей найти своё место.
            5. Не используй шаблоны вроде «успех», «предназначение», «миссия» — говори образно, честно, как о человеке, который давно ждёт, чтобы его поняли.

            Стиль: разговорный, вдохновляющий, будто ты открыл для него(неё) глаза на то, что в нем(ней) всегда было, но он(а) не знал(а), как это назвать. Ты не предсказываешь будущее — ты показываешь глубинную правду.

            Ввод: фото левой и правой ладоней. %s. Напиши: кто он(а)? В чём его(её) сила? Чего ему(ей) давно пора перестать бояться? И где его(её) нереализованный потенциал?
            """.formatted(text, text);

        var gptGeneratedText = gptService.getResponseChatForUserWithImages(chatId, prompt, null);
        return new SendMessage(chatId.toString(), gptGeneratedText);
    }
} 