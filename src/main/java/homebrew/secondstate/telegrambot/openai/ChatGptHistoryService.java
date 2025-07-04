package homebrew.secondstate.telegrambot.openai;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import homebrew.secondstate.telegrambot.openai.api.Message;

import java.util.ArrayList;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class ChatGptHistoryService {

    private final Map<Long, ChatHistory> chatHistoryMap = new ConcurrentHashMap<>();

    public Optional<ChatHistory> getUserHistory(Long userId) {
        return Optional.ofNullable(chatHistoryMap.get(userId));
    }

    public void createHistory(Long userId) {
        chatHistoryMap.put(userId, new ChatHistory(new ArrayList<>(), new ArrayList<>()));
    }

    public void clearHistory(Long userId) {
        chatHistoryMap.remove(userId);
    }

    public ChatHistory addMessageToHistory(Long userId, Message message) {
        var chatHistory = chatHistoryMap.get(userId);
        if (chatHistory == null) {
            throw new IllegalStateException("History not exists for user =%s".formatted(userId));
        }
        var newHistory = chatHistory.addMessage(message);
        chatHistoryMap.put(userId, newHistory);
        return newHistory;
    }

    public ChatHistory addImageToHistory(Long userId, Map<String, Object> image) {
        var chatHistory = chatHistoryMap.get(userId);
        if (chatHistory == null) {
            throw new IllegalStateException("History not exists for user =%s".formatted(userId));
        }
        var newHistory = chatHistory.addImage(image);
        chatHistoryMap.put(userId, newHistory);
        return newHistory;
    }

    public void createHistoryIfNotExist(Long userId) {
        if (!chatHistoryMap.containsKey(userId)) {
            createHistory(userId);
        }
    }

    public void clearImages(Long userId) {
        var chatHistory = chatHistoryMap.get(userId);
        if (chatHistory != null) {
            chatHistoryMap.put(userId, chatHistory.clearImages());
        }
    }
}
