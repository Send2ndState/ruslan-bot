package homebrew.secondstate.telegrambot.openai;

import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import homebrew.secondstate.telegrambot.openai.api.CreateTranscriptionRequest;
import homebrew.secondstate.telegrambot.openai.api.OpenAIClient;

import java.io.File;

@Service
@AllArgsConstructor
public class TranscribeVoiceToTextService {

    private final OpenAIClient openAIClient;

    public String transcribe(File audioFile) {
        var request = new CreateTranscriptionRequest(audioFile, "whisper-1");
        var response = openAIClient.createTranscription(request);
        return response.text();
    }

}
