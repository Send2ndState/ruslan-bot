package school.sorokin.event.manager.telegrambot.telegram.state;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class UserImageState {
    private static final long ONE_HOUR_IN_SECONDS = 3600;
    
    private final Map<Long, Integer> userImageCount = new ConcurrentHashMap<>();
    private final Map<Long, Instant> lastImageTimestamp = new ConcurrentHashMap<>();

    public void incrementImageCount(Long chatId) {
        userImageCount.merge(chatId, 1, Integer::sum);
        lastImageTimestamp.put(chatId, Instant.now());
    }

    public boolean canSendMoreImages(Long chatId) {
        checkAndResetIfNeeded(chatId);
        return userImageCount.getOrDefault(chatId, 0) < 2;
    }

    private void checkAndResetIfNeeded(Long chatId) {
        Instant lastTimestamp = lastImageTimestamp.get(chatId);
        if (lastTimestamp != null) {
            long secondsSinceLastImage = Instant.now().getEpochSecond() - lastTimestamp.getEpochSecond();
            if (secondsSinceLastImage >= ONE_HOUR_IN_SECONDS) {
                resetImageCount(chatId);
            }
        }
    }

    public void resetImageCount(Long chatId) {
        userImageCount.remove(chatId);
        lastImageTimestamp.remove(chatId);
    }
} 