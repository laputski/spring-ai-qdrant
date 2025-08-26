package io.callicode.qdrant.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.document.DocumentReader;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.ai.reader.JsonReader;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.reader.pdf.config.PdfDocumentReaderConfig;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public class DocUtils {
  private static final Logger log = LoggerFactory.getLogger(DocUtils.class);

  public static DocumentReader readDocument(MultipartFile file) {
    String filename = file.getOriginalFilename();
    if (filename == null) {
      log.error("Document filename is null");
      return null;
    }
    try {
      Resource resource = new InputStreamResource(file.getInputStream());
      if (filename.endsWith(".pdf")) {
        return new PagePdfDocumentReader(resource,
                PdfDocumentReaderConfig.builder()
                        .withPageExtractedTextFormatter(ExtractedTextFormatter.builder()
                                .withNumberOfBottomTextLinesToDelete(3)
                                .withNumberOfTopPagesToSkipBeforeDelete(3)
                                .build())
                        .withPagesPerDocument(3)
                        .build());
      } else if (filename.endsWith(".txt")) {
        return new TextReader(resource);
      } else if (filename.endsWith(".json")) {
        return new JsonReader(resource);
      }
    } catch (Exception e) {
      log.error("Error reading document from MultipartFile", e);
      return null;
    }
    log.error("Unsupported document format: {}", filename);
    return null;
  }
}
