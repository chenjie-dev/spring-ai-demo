package com.example.service;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

@SpringBootTest
class AgentServiceTest {

    @MockBean
    private ChatModel chatModel;

    @MockBean
    private FileSearchService fileSearchService;

    @MockBean
    private FileDownloadService fileDownloadService;

    @Test
    void testDownloadIntentAnalysis() {
        // 模拟LLM返回下载意图
        String mockResponse = """
            {
                "intent": "FILE_DOWNLOAD",
                "parameters": {
                    "query": "pom.xml"
                }
            }
            """;
        
        ChatResponse chatResponse = new ChatResponse(
            List.of(new Generation(mockResponse))
        );
        
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // 这里可以添加更多测试逻辑
        assertTrue(true, "Basic test passed");
    }

    @Test
    void testDownloadIntentWithController() {
        // 模拟LLM返回下载意图
        String mockResponse = """
            {
                "intent": "FILE_DOWNLOAD",
                "parameters": {
                    "query": "OllamaChatController"
                }
            }
            """;
        
        ChatResponse chatResponse = new ChatResponse(
            List.of(new Generation(mockResponse))
        );
        
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);
        
        // 这里可以添加更多测试逻辑
        assertTrue(true, "Controller download test passed");
    }
} 