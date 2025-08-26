package io.callicode.qdrant.service;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.prompt.SystemPromptTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
public class ChatServiceImpl implements ChatService {

  private final RagService ragService;
  private final ChatClient chatClient;
  @Value("classpath:system.pmt")
  private Resource systemPromptResource;

  public ChatServiceImpl(RagService ragService, ChatClient.Builder chatClientBuilder) {
    this.ragService = ragService;
    this.chatClient = chatClientBuilder.build();
  }

  @Override
  public String ask(String prompt) {
    if (prompt == null || prompt.isBlank()) {
      log.error("Prompt is null or blank");
      throw new IllegalArgumentException("Prompt must not be blank");
    }
    log.info("Question: {}", prompt);
    return chatClient.prompt(prompt).call().content();
  }

  @Override
  public String askRag(String query) {
    if (query == null || query.isBlank()) {
      log.error("Query is null or blank");
      throw new IllegalArgumentException("Query must not be blank");
    }
    Prompt prompt = new Prompt(List.of(getRelevantDocs(query), new UserMessage(query)));
    ChatResponse response = chatClient.prompt(prompt).call().chatResponse();
    String answer = Objects.requireNonNull(response).getResult().getOutput().getText();
    log.info("AI model response: {}", answer);
    return answer;
  }

  private Message getRelevantDocs(String prompt) {
    String knowledge = String.join("\n", ragService.search(prompt));
    SystemPromptTemplate systemPromptTemplate = new SystemPromptTemplate(systemPromptResource);
    return systemPromptTemplate.createMessage(Map.of("knowledge", knowledge));
  }

  public void monitoredChat(String prompt) {
    Timer.Sample sample = Timer.start();
    String response = chatClient.prompt(prompt).call().content();
    sample.stop(Metrics.globalRegistry.timer("spring.ai.chat.duration"));
    log.info(response);
  }

}
