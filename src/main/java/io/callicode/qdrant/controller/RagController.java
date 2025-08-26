package io.callicode.qdrant.controller;

import io.callicode.qdrant.service.RagService;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/rag")
public class RagController {

  private final RagService ragService;

  public RagController(RagService ragService) {
    this.ragService = ragService;
  }

  @PostMapping("/save-text")
  public ResponseEntity<Integer> saveToRag(@Valid @RequestBody List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      log.warn("Texts list is empty");
      return ResponseEntity.badRequest().body(0);
    }
    int saved = ragService.saveText(texts);
    return ResponseEntity.ok(saved);
  }

  @PostMapping("/save-doc")
  public ResponseEntity<String> saveDoc(@RequestParam("file") MultipartFile file) {
    try {
      ragService.saveDoc(file);
      return ResponseEntity.ok("Document indexed successfully");
    } catch (Exception e) {
      log.error("Error indexing document", e);
      return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
    }
  }

  @PostMapping("/search")
  public ResponseEntity<List<Document>> searchRag(@RequestBody SearchRequest request) {
    List<Document> results = ragService.search(request);
    return ResponseEntity.ok(results);
  }

  @GetMapping("/count")
  public ResponseEntity<Long> count() {
    long count = ragService.count();
    return ResponseEntity.ok(count);
  }
}
