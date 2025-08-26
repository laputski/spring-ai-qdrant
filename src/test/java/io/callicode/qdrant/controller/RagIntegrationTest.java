package io.callicode.qdrant.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class RagIntegrationTest {
    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void chatRag_shouldLearnNewConcept() throws Exception {
        String uniqueFact = "Yetii is neighbour of Alex";
        String question = "What is Yetii?";
        String expectedAnswerFragment = "neighbour of Alex";

        // 1. Initial chat-rag request (unknown concept)
        Map<String, Object> chatRequest = Map.of(
            "model", "phi3",
            "messages", List.of(
                Map.of("role", "system", "content", "Use provided context to answer. Questions will be about X. Answer in one sentence in format: X is ..."),
                Map.of("role", "user", "content", question)
            ),
            "temperature", 0.1,
            "max_tokens", 512,
            "stream", false
        );
        ResultActions initialChat = mockMvc.perform(MockMvcRequestBuilders.post("/api/ai/chat-rag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk());
        String initialResponse = initialChat.andReturn().getResponse().getContentAsString();
        assertThat(initialResponse.toLowerCase()).doesNotContain(expectedAnswerFragment);

        // 2. Save new concept to RAG
        List<String> textsRequest = List.of(uniqueFact);
        mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/save-text")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(textsRequest)))
                .andExpect(status().isOk());

        // 3. Check in RAG
        Map<String, Object> searchRequest = Map.of(
            "query", question,
            "topK", 1
        );
        ResultActions searchRag = mockMvc.perform(MockMvcRequestBuilders.post("/api/rag/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(searchRequest)))
                .andExpect(status().isOk());
        String searchResponse = searchRag.andReturn().getResponse().getContentAsString();
        assertThat(searchResponse).contains(uniqueFact);

        // 4. Chat-rag request again (should know the concept)
        ResultActions afterSaveChat = mockMvc.perform(MockMvcRequestBuilders.post("/api/ai/chat-rag")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(chatRequest)))
                .andExpect(status().isOk());
        String afterSaveResponse = afterSaveChat.andReturn().getResponse().getContentAsString();
        String answerLower = afterSaveResponse.toLowerCase();
        assertThat(answerLower).satisfiesAnyOf(
            s -> assertThat(s).contains("neighbor of alex"),
            s -> assertThat(s).contains("neighbour of alex")
        );
        assertThat(afterSaveResponse).isNotEqualTo(initialResponse);
    }
}
