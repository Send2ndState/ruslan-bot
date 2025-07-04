package homebrew.secondstate.telegrambot.telegram;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.bots.DefaultAbsSender;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.objects.File;

import java.io.*;
import java.net.URI;
import java.net.URL;

@Slf4j
@Service
public class TelegramFileService {

    private final DefaultAbsSender telegramSender;
    private final String botToken;

    public TelegramFileService(
            @Lazy DefaultAbsSender telegramSender,
            @Value("${bot.token}") String botToken
    ) {
        this.telegramSender = telegramSender;
        this.botToken = botToken;
    }

    @SneakyThrows
    public java.io.File getFile(String fileId) {
        File file = telegramSender.execute(GetFile.builder()
                .fileId(fileId)
                .build());
        var urlToDownloadFile = file.getFileUrl(botToken);
        return getFileFromUrl(urlToDownloadFile, getFileExtension(fileId));
    }

    @SneakyThrows
    private java.io.File getFileFromUrl(String urlToDownloadFile, String extension) {
        URL url = new URI(urlToDownloadFile).toURL();
        var fileTemp = java.io.File.createTempFile("telegram", extension);

        try (InputStream inputStream = url.openStream();
             FileOutputStream fileOutputStream = new FileOutputStream(fileTemp)
        ) {
            IOUtils.copy(inputStream, fileOutputStream);
        } catch (IOException e) {
            log.error("Error while file copying from url to temp file", e);
            throw new RuntimeException("Error while downloading file", e);
        }
        return fileTemp;
    }

    private String getFileExtension(String fileId) {
        // Telegram file IDs for images start with different prefixes
        // AgAC - for photos
        // AQAD - for documents
        // BQAD - for videos
        if (fileId.startsWith("AgAC")) {
            return ".jpg";
        } else if (fileId.startsWith("AQAD")) {
            // Check if it's a JPEG document
            return ".jpeg";
        } else if (fileId.startsWith("BQAD")) {
            return ".mp4";
        }
        // Default to .ogg for voice messages
        return ".ogg";
    }
}
