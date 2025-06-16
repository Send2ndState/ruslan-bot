package homebrew.secondstate.telegrambot.openai.api;

import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

@Slf4j
@AllArgsConstructor
public class OpenAIClient {

    private final String token;
    private final RestTemplate restTemplate;

    public ChatCompletionResponse createChatCompletion(
            ChatCompletionRequest request
    ) {
        String url = "https://api.openai.com/v1/chat/completions";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "Bearer " + token);
        httpHeaders.set("Content-type", "application/json");

        HttpEntity<ChatCompletionRequest> httpEntity = new HttpEntity<>(request, httpHeaders);

        try {
            ResponseEntity<ChatCompletionResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, httpEntity, ChatCompletionResponse.class
            );
            return responseEntity.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("OpenAI API authentication failed. Please check your API token.");
            throw new RuntimeException("Ошибка авторизации в OpenAI API. Пожалуйста, проверьте настройки API токена.", e);
        } catch (HttpClientErrorException e) {
            log.error("OpenAI API request failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка при обращении к OpenAI API: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error while calling OpenAI API", e);
            throw new RuntimeException("Неожиданная ошибка при обращении к OpenAI API", e);
        }
    }

    @SneakyThrows
    public TranscriptionResponse createTranscription(
            CreateTranscriptionRequest request
    ) {
        String url = "https://api.openai.com/v1/audio/transcriptions";

        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.set("Authorization", "Bearer " + token);
        httpHeaders.set("Content-type", "multipart/form-data");

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", new FileSystemResource(request.audioFile()));
        body.add("model", request.model());

        var httpEntity = new HttpEntity<>(body, httpHeaders);

        try {
            ResponseEntity<TranscriptionResponse> responseEntity = restTemplate.exchange(
                    url, HttpMethod.POST, httpEntity, TranscriptionResponse.class
            );
            return responseEntity.getBody();
        } catch (HttpClientErrorException.Unauthorized e) {
            log.error("OpenAI API authentication failed. Please check your API token.");
            throw new RuntimeException("Ошибка авторизации в OpenAI API. Пожалуйста, проверьте настройки API токена.", e);
        } catch (HttpClientErrorException e) {
            log.error("OpenAI API request failed with status {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Ошибка при обращении к OpenAI API: " + e.getStatusCode(), e);
        } catch (Exception e) {
            log.error("Unexpected error while calling OpenAI API", e);
            throw new RuntimeException("Неожиданная ошибка при обращении к OpenAI API", e);
        }
    }
}
