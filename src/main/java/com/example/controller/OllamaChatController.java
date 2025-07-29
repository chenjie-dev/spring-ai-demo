package com.example.controller;

import com.example.service.AgentService;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
@CrossOrigin(origins = "*")
public class OllamaChatController {

    private final ChatModel ollamaChatModel;
    private final AgentService agentService;
    private static final String DEFAULT_PROMPT = "你好，介绍下你自己吧。请用中文回答。";

    public OllamaChatController(ChatModel chatModel, AgentService agentService) {
        this.ollamaChatModel = chatModel;
        this.agentService = agentService;
    }

    /**
     * 简单聊天接口
     */
    @GetMapping("/simple")
    public String simpleChat() {
        return ollamaChatModel.call(new Prompt(DEFAULT_PROMPT))
                .getResult()
                .getOutput()
                .getText();
    }

    /**
     * 发送消息并获取回复
     */
    @PostMapping("/message")
    public Map<String, Object> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = DEFAULT_PROMPT;
        }

        // 使用Agent服务处理请求
        AgentService.AgentResponse agentResponse = agentService.processRequest(message);
        
        return Map.of(
                "message", message,
                "reply", agentResponse.getMessage(),
                "action", agentResponse.getAction(),
                "data", agentResponse.getData()
        );
    }

    /**
     * 流式聊天接口
     */
    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> streamChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = DEFAULT_PROMPT;
        }

        return ollamaChatModel.stream(new Prompt(message))
                .map(response -> response.getResult().getOutput().getText());
    }

    /**
     * 多轮对话接口
     */
    @PostMapping("/conversation")
    public Map<String, Object> conversation(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) request.get("messages");
        String newMessage = (String) request.get("newMessage");

        if (newMessage == null || newMessage.trim().isEmpty()) {
            return Map.of("error", "New message is required");
        }

        // 构建对话历史
        List<? extends AbstractMessage> conversationHistory = messages.stream()
                .map(msg -> {
                    String role = msg.get("role");
                    String content = msg.get("content");
                    return "user".equals(role) ? new UserMessage(content) : new AssistantMessage(content);
                })
                .toList();

        // 添加新消息
        conversationHistory = List.of(
                new UserMessage(newMessage)
        );

        ChatResponse response = ollamaChatModel.call(new Prompt((Message) conversationHistory));
        String reply = response.getResult().getOutput().getText();

        return Map.of(
                "newMessage", newMessage,
                "reply", reply,
                "conversationHistory", conversationHistory
        );
    }

    /**
     * 健康检查接口
     */
    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "OK", "message", "Ollama chat service with Agent is running");
    }

    /**
     * Agent智能处理接口
     */
    @PostMapping("/agent")
    public AgentService.AgentResponse agentChat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            message = DEFAULT_PROMPT;
        }
        
        return agentService.processRequest(message);
    }

    /**
     * 调试接口 - 显示意图分析结果
     */
    @PostMapping("/debug/intent")
    public Map<String, Object> debugIntent(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        if (message == null || message.trim().isEmpty()) {
            return Map.of("error", "Message is required");
        }
        
        // 这里可以添加意图分析的调试信息
        return Map.of(
            "message", message,
            "timestamp", System.currentTimeMillis(),
            "note", "Intent analysis debug info would be shown here"
        );
    }
} 