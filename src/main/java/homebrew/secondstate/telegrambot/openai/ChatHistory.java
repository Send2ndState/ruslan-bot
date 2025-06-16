package homebrew.secondstate.telegrambot.openai;

import homebrew.secondstate.telegrambot.openai.api.Message;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public record ChatHistory(
    List<Message> chatMessages,
    List<Map<String, Object>> images
) {
    public ChatHistory {
        if (chatMessages == null) {
            chatMessages = new ArrayList<>();
        }
        if (images == null) {
            images = new ArrayList<>();
        }
    }

    public ChatHistory addMessage(Message message) {
        List<Message> newMessages = new ArrayList<>(chatMessages);
        newMessages.add(message);
        return new ChatHistory(newMessages, images);
    }

    public ChatHistory addImage(Map<String, Object> image) {
        List<Map<String, Object>> newImages = new ArrayList<>(images);
        newImages.add(image);
        return new ChatHistory(chatMessages, newImages);
    }

    public ChatHistory clearImages() {
        return new ChatHistory(chatMessages, new ArrayList<>());
    }
}
