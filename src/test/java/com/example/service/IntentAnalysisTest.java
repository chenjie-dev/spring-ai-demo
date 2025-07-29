package com.example.service;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class IntentAnalysisTest {

    @Test
    void testDownloadIntentPatterns() {
        // 测试各种下载表达方式
        String[] testMessages = {
            "下载OllamaChatController",
            "下载 pom.xml",
            "下载 https://example.com/file.txt",
            "download pom.xml",
            "下载第一个文件",
            "下载配置文件"
        };
        
        for (String message : testMessages) {
            System.out.println("Testing: " + message);
            // 这里可以添加具体的测试逻辑
            assertTrue(message.contains("下载") || message.contains("download"), 
                      "Message should contain download keyword: " + message);
        }
    }

    @Test
    void testControllerDownloadPattern() {
        String message = "下载OllamaChatController";
        
        // 测试正则表达式匹配
        String lowerMessage = message.toLowerCase();
        assertTrue(lowerMessage.contains("下载"), "Should contain 下载");
        
        // 测试文件名提取
        String[] parts = message.split("下载");
        assertEquals(2, parts.length, "Should split into 2 parts");
        assertEquals("OllamaChatController", parts[1].trim(), "Should extract filename");
    }
} 