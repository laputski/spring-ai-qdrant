package io.callicode.qdrant.controller;

import io.callicode.qdrant.service.ChatService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/ai")
public class ChatController {

  private final ChatService chatService;

  public ChatController(ChatService chatService) {
    this.chatService = chatService;
  }

  @PostMapping("/chat")
  public ResponseEntity<String> chat(@Valid @RequestBody String prompt) {
    if (prompt == null || prompt.isBlank()) {
      log.warn("Prompt is blank");
      return ResponseEntity.badRequest().body("Prompt must not be blank");
    }
    return ResponseEntity.ok(chatService.ask(prompt));
  }

  @PostMapping("/chat-rag")
  public ResponseEntity<String> chatRag(@Valid @RequestBody String prompt) {
    if (prompt == null || prompt.isBlank()) {
      log.warn("Prompt for RAG is blank");
      return ResponseEntity.badRequest().body("Prompt must not be blank");
    }
    return ResponseEntity.ok(chatService.askRag(prompt));
  }

}
