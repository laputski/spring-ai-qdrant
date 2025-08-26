package io.callicode.qdrant.service;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface RagService {

  int saveText(List<String> texts);

  void saveDoc(MultipartFile file);

  List<Document> search(SearchRequest request);

  List<String> search(String prompt);

  long count();
}
