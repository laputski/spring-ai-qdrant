package io.callicode.qdrant.service;

import io.callicode.qdrant.util.DocUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
public class RagServiceImpl implements RagService {

  private final QdrantVectorStore qdrantVectorStore;

  public RagServiceImpl(QdrantVectorStore qdrantVectorStore) {
    this.qdrantVectorStore = qdrantVectorStore;
  }

  @Override
  public int saveText(List<String> texts) {
    if (texts == null || texts.isEmpty()) {
      log.error("Texts list is null or empty");
      throw new IllegalArgumentException("Texts list must not be empty");
    }
    List<Document> docs = texts.stream().map(Document::new).collect(Collectors.toList());
    qdrantVectorStore.add(docs);
    log.info("Saved {} phrases to Qdrant.", texts.size());
    return texts.size();
  }

  @Override
  public List<Document> search(SearchRequest request) {
    //SearchRequest.builder().query(query).topK(topK).similarityThreshold(0.5).build()
    List<Document> results = qdrantVectorStore.similaritySearch(request);
    log.info("Found {} documents with topK={}", Objects.requireNonNull(results).size(), request.getTopK());
    return results;
  }

  @Override
  public List<String> search(String prompt) {
    List<Document> results = qdrantVectorStore.similaritySearch(prompt);
    log.info("Found {} documents via search.", Objects.requireNonNull(results).size());
    return Objects.requireNonNull(results).stream().map(Document::getText).collect(Collectors.toList());
  }

  @Override
  public void saveDoc(MultipartFile file) {
    try {
      DocumentReader documentReader = DocUtils.readDocument(file);
      if (documentReader == null) throw new RuntimeException("Document for qdrant loading is missing or invalid.");
      var docs = documentReader.get();
      if (docs == null || docs.isEmpty()) {
        log.error("No documents extracted from PDF: {}", file.getOriginalFilename());
        throw new RuntimeException("PDF contains no readable text or is empty.");
      }
      var textSplitter = new TokenTextSplitter();
      log.info("Loading {} to qdrant, extracted {} docs", file.getOriginalFilename(), docs.size());
      qdrantVectorStore.accept(textSplitter.apply(docs));
      log.info("Loaded {} to qdrant", file.getOriginalFilename());
    } catch (Exception e) {
      log.error("Error loading document to qdrant", e);
      throw new RuntimeException("Failed to process document: " + e.getMessage(), e);
    }
  }

  @Override
  public long count() {
    return Objects.requireNonNull(qdrantVectorStore.similaritySearch("*")).size();
  }

}
