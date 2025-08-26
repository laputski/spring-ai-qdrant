package io.callicode.qdrant.service;

public interface ChatService {

  String ask(String query);

  String askRag(String query);

}
