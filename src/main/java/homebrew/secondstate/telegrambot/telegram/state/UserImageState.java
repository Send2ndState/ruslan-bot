package homebrew.secondstate.telegrambot.telegram.state;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserImageState {
    private static final int MAX_IMAGES_PER_DAY = 4;
    
    private final Map<Long, Integer> userImageCount = new ConcurrentHashMap<>();
    private final Map<Long, LocalDateTime> lastResetDate = new ConcurrentHashMap<>();

    public void incrementImageCount(Long chatId) {
        checkAndResetIfNeeded(chatId);
        userImageCount.merge(chatId, 1, Integer::sum);
    }

    public boolean canSendMoreImages(Long chatId) {
        checkAndResetIfNeeded(chatId);
        return userImageCount.getOrDefault(chatId, 0) < MAX_IMAGES_PER_DAY;
    }

    private void checkAndResetIfNeeded(Long chatId) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime lastReset = lastResetDate.get(chatId);
        
        if (lastReset == null || !lastReset.toLocalDate().equals(now.toLocalDate())) {
            resetImageCount(chatId);
            lastResetDate.put(chatId, now);
        }
    }

    public void resetImageCount(Long chatId) {
        userImageCount.remove(chatId);
    }
} 