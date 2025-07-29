package com.example.service;


import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class AgentService {

    private final ChatModel chatModel;
    private final FileSearchService fileSearchService;
    private final FileDownloadService fileDownloadService;

    public AgentService(ChatModel chatModel, FileSearchService fileSearchService, FileDownloadService fileDownloadService) {
        this.chatModel = chatModel;
        this.fileSearchService = fileSearchService;
        this.fileDownloadService = fileDownloadService;
    }

    /**
     * 智能Agent处理用户请求
     */
    public AgentResponse processRequest(String userMessage) {
        // 检查是否是下载确认
        if (isDownloadConfirmation(userMessage)) {
            return handleDownloadConfirmation(userMessage);
        }
        
        // 使用LLM分析用户意图
        Intent intent = analyzeIntentWithLLM(userMessage);
        
        // 添加调试信息
        System.out.println("DEBUG - User message: " + userMessage);
        System.out.println("DEBUG - Detected intent: " + intent.getType());
        System.out.println("DEBUG - Parameters: " + intent.getParameters());
        
        switch (intent.getType()) {
            case FILE_SEARCH:
                return handleFileSearch(intent, userMessage);
            case FILE_DOWNLOAD:
                return handleFileDownload(intent, userMessage);
            case FILE_READ:
                return handleFileRead(intent, userMessage);
            case SYSTEM_INFO:
                return handleSystemInfo();
            case GENERAL_CHAT:
            default:
                return handleGeneralChat(userMessage);
        }
    }

    // 存储待确认的下载信息
    private static final Map<String, Object> pendingDownload = new HashMap<>();

    /**
     * 检查是否是下载确认消息
     */
    private boolean isDownloadConfirmation(String userMessage) {
        return pendingDownload.containsKey("needConfirmation") && 
               (pendingDownload.get("needConfirmation").equals(true));
    }

    /**
     * 处理下载确认
     */
    private AgentResponse handleDownloadConfirmation(String userMessage) {
        @SuppressWarnings("unchecked")
        List<FileSearchService.FileInfo> files = (List<FileSearchService.FileInfo>) pendingDownload.get("files");
        String targetDirectory = (String) pendingDownload.get("targetDirectory");
        
        if (files == null || files.isEmpty()) {
            pendingDownload.clear();
            return new AgentResponse(
                "file_download_error",
                "下载确认信息已过期，请重新发起下载请求。",
                Map.of()
            );
        }
        
        // 尝试解析用户选择
        String selectedFile = parseFileSelection(userMessage, files);
        
        if (selectedFile != null) {
            // 执行下载
            FileDownloadService.DownloadTask task = fileDownloadService.downloadLocalFile(selectedFile, targetDirectory);
            
            // 清除待确认信息
            pendingDownload.clear();
            
            String aiResponse = String.format(
                "已开始下载文件：%s\n" +
                "任务ID：%s\n" +
                "目标目录：%s\n" +
                "状态：%s",
                selectedFile, task.getTaskId(), targetDirectory, task.getStatus()
            );
            
            return new AgentResponse(
                "file_download",
                aiResponse,
                Map.of("downloadTask", task)
            );
        } else {
            return new AgentResponse(
                "file_download_error",
                "无法识别您的选择，请尝试：\n• 数字：1、2、3...\n• 中文：第一个、第二个...\n• 文件名：pom.xml、README.md...\n• 部分文件名：pom、readme...",
                Map.of("needConfirmation", true)
            );
        }
    }

    /**
     * 解析用户选择的文件
     */
    private String parseFileSelection(String userMessage, List<FileSearchService.FileInfo> files) {
        String message = userMessage.trim().toLowerCase();
        
        // 1. 优先尝试解析数字选择（最简单）
        try {
            int index = Integer.parseInt(message);
            if (index >= 1 && index <= files.size()) {
                return files.get(index - 1).getPath();
            }
        } catch (NumberFormatException e) {
            // 不是数字，继续检查
        }
        
        // 2. 尝试解析中文数字
        int chineseNumber = parseChineseNumber(message);
        if (chineseNumber >= 1 && chineseNumber <= files.size()) {
            return files.get(chineseNumber - 1).getPath();
        }
        
        // 3. 尝试匹配文件名（不区分大小写）
        for (FileSearchService.FileInfo file : files) {
            if (file.getName().toLowerCase().equals(message)) {
                return file.getPath();
            }
        }
        
        // 4. 尝试部分文件名匹配（更宽松）
        for (FileSearchService.FileInfo file : files) {
            if (file.getName().toLowerCase().contains(message)) {
                return file.getPath();
            }
        }
        
        // 5. 尝试路径中的任何部分匹配
        for (FileSearchService.FileInfo file : files) {
            if (file.getPath().toLowerCase().contains(message)) {
                return file.getPath();
            }
        }
        
        // 6. 如果只有一个文件，直接返回
        if (files.size() == 1) {
            return files.get(0).getPath();
        }
        
        // 7. 尝试匹配"第一个"、"第二个"等表达
        if (message.contains("第一个") || message.contains("第一") || message.equals("1")) {
            return files.get(0).getPath();
        }
        if (message.contains("第二个") || message.contains("第二") || message.equals("2")) {
            if (files.size() >= 2) return files.get(1).getPath();
        }
        if (message.contains("第三个") || message.contains("第三") || message.equals("3")) {
            if (files.size() >= 3) return files.get(2).getPath();
        }
        
        return null;
    }

    /**
     * 解析中文数字
     */
    private int parseChineseNumber(String text) {
        if (text.contains("一") || text.contains("1")) return 1;
        if (text.contains("二") || text.contains("2")) return 2;
        if (text.contains("三") || text.contains("3")) return 3;
        if (text.contains("四") || text.contains("4")) return 4;
        if (text.contains("五") || text.contains("5")) return 5;
        if (text.contains("六") || text.contains("6")) return 6;
        if (text.contains("七") || text.contains("7")) return 7;
        if (text.contains("八") || text.contains("8")) return 8;
        if (text.contains("九") || text.contains("9")) return 9;
        if (text.contains("十") || text.contains("10")) return 10;
        return -1;
    }

    /**
     * 处理文件搜索请求
     */
    private AgentResponse handleFileSearch(Intent intent, String userMessage) {
        String query = intent.getParameters().get("query");
        String basePath = intent.getParameters().getOrDefault("basePath", ".");
        
        List<FileSearchService.FileInfo> files = fileSearchService.searchFiles(query, basePath);
        List<FileSearchService.FileContentMatch> contentMatches = fileSearchService.searchFileContent(query, basePath);
        
        // 构建AI回复
        String aiResponse = buildFileSearchResponse(files, contentMatches, query);
        
        return new AgentResponse(
                "file_search",
                aiResponse,
                Map.of(
                        "files", files,
                        "contentMatches", contentMatches,
                        "query", query,
                        "basePath", basePath
                )
        );
    }

    /**
     * 处理文件下载请求
     */
    private AgentResponse handleFileDownload(Intent intent, String userMessage) {
        String url = intent.getParameters().get("url");
        String filePath = intent.getParameters().get("filePath");
        String query = intent.getParameters().get("query");
        String targetDirectory = intent.getParameters().getOrDefault("targetDirectory", "./downloads");
        
        // 如果是URL下载，直接执行
        if (url != null && url.startsWith("http")) {
            FileDownloadService.DownloadTask task = fileDownloadService.downloadFile(url, targetDirectory);
            
            String aiResponse = String.format(
                    "已开始下载文件：%s\n" +
                    "任务ID：%s\n" +
                    "目标目录：%s\n" +
                    "状态：%s",
                    url, task.getTaskId(), targetDirectory, task.getStatus()
            );
            
            return new AgentResponse(
                    "file_download",
                    aiResponse,
                    Map.of("downloadTask", task)
            );
        }
        
        // 如果是具体文件路径，直接下载
        if (filePath != null) {
            FileDownloadService.DownloadTask task = fileDownloadService.downloadLocalFile(filePath, targetDirectory);
            
            String aiResponse = String.format(
                    "已开始下载文件：%s\n" +
                    "任务ID：%s\n" +
                    "目标目录：%s\n" +
                    "状态：%s",
                    filePath, task.getTaskId(), targetDirectory, task.getStatus()
            );
            
            return new AgentResponse(
                    "file_download",
                    aiResponse,
                Map.of("downloadTask", task)
            );
        }
        
        // 如果是模糊查询，先搜索再确认
        if (query != null) {
            return handleDownloadWithSearch(query, targetDirectory);
        }
        
        return new AgentResponse(
                "file_download_error",
                "请提供有效的文件路径、URL或文件名",
                Map.of()
        );
    }

    /**
     * 处理需要搜索的下载请求
     */
    private AgentResponse handleDownloadWithSearch(String query, String targetDirectory) {
        // 先搜索文件
        List<FileSearchService.FileInfo> files = fileSearchService.searchFiles(query, ".");
        
        if (files.isEmpty()) {
            return new AgentResponse(
                    "file_download_error",
                    String.format("未找到匹配的文件：%s\n请尝试更具体的文件名或路径。", query),
                    Map.of("query", query)
            );
        }
        
        // 构建搜索结果供用户选择
        StringBuilder response = new StringBuilder();
        response.append(String.format("找到以下匹配的文件，请确认要下载哪一个：\n\n"));
        
        for (int i = 0; i < Math.min(files.size(), 10); i++) {
            FileSearchService.FileInfo file = files.get(i);
            response.append(String.format("%d. %s (%s)\n", 
                i + 1, file.getName(), formatFileSize(file.getSize())));
        }
        
        if (files.size() > 10) {
            response.append(String.format("... 还有 %d 个文件\n", files.size() - 10));
        }
        
        response.append("\n请回复：");
        response.append("\n• 数字：1、2、3...");
        response.append("\n• 中文：第一个、第二个...");
        response.append("\n• 文件名：pom.xml、README.md...");
        response.append("\n• 部分文件名：pom、readme...");
        
        // 存储待确认信息
        pendingDownload.clear();
        pendingDownload.put("needConfirmation", true);
        pendingDownload.put("files", files);
        pendingDownload.put("targetDirectory", targetDirectory);
        pendingDownload.put("query", query);
        
        return new AgentResponse(
                "file_download_confirm",
                response.toString(),
                Map.of(
                    "query", query,
                    "files", files,
                    "targetDirectory", targetDirectory,
                    "needConfirmation", true
                )
        );
    }

    /**
     * 处理文件读取请求
     */
    private AgentResponse handleFileRead(Intent intent, String userMessage) {
        String filePath = intent.getParameters().get("filePath");
        
        String content = fileSearchService.readFileContent(filePath);
        
        if (content != null) {
            String aiResponse = String.format(
                    "文件内容已读取：%s\n\n内容预览（前500字符）：\n%s",
                    filePath,
                    content.length() > 500 ? content.substring(0, 500) + "..." : content
            );
            
            return new AgentResponse(
                    "file_read",
                    aiResponse,
                    Map.of(
                            "filePath", filePath,
                            "content", content,
                            "contentLength", content.length()
                    )
            );
        } else {
            return new AgentResponse(
                    "file_read_error",
                    "无法读取文件：" + filePath,
                    Map.of("filePath", filePath)
            );
        }
    }

    /**
     * 处理系统信息请求
     */
    private AgentResponse handleSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        List<FileDownloadService.DownloadTask> downloadTasks = fileDownloadService.getAllDownloadTasks();
        long activeDownloads = downloadTasks.stream()
                .filter(task -> task.getStatus() == FileDownloadService.DownloadStatus.DOWNLOADING)
                .count();
        
        String aiResponse = String.format(
                "系统信息：\n" +
                "内存使用：%.1f%% (%.1f MB / %.1f MB)\n" +
                "活跃下载任务：%d\n" +
                "总下载任务：%d",
                (double) usedMemory / totalMemory * 100,
                usedMemory / (1024.0 * 1024.0),
                totalMemory / (1024.0 * 1024.0),
                activeDownloads,
                downloadTasks.size()
        );
        
        return new AgentResponse(
                "system_info",
                aiResponse,
                Map.of(
                        "memory", Map.of(
                                "total", totalMemory,
                                "used", usedMemory,
                                "free", freeMemory
                        ),
                        "downloads", Map.of(
                                "active", activeDownloads,
                                "total", downloadTasks.size()
                        )
                )
        );
    }

    /**
     * 处理一般聊天请求
     */
    private AgentResponse handleGeneralChat(String userMessage) {
        ChatResponse response = chatModel.call(new Prompt(userMessage));
        String aiResponse = response.getResult().getOutput().getText();
        
        return new AgentResponse(
                "general_chat",
                aiResponse,
                Map.of("originalMessage", userMessage)
        );
    }

    /**
     * 使用LLM分析用户意图
     */
    private Intent analyzeIntentWithLLM(String userMessage) {
        String prompt = String.format("""
            请分析以下用户消息的意图，并返回JSON格式的结果。
            
            用户消息: %s
            
            请从以下意图类型中选择：
            1. FILE_SEARCH - 用户想要搜索文件
            2. FILE_DOWNLOAD - 用户想要下载文件
            3. FILE_READ - 用户想要读取文件内容
            4. SYSTEM_INFO - 用户想要查看系统信息
            5. GENERAL_CHAT - 一般聊天对话
            
            下载意图的判断规则：
            - 如果消息包含"下载"、"download"等关键词，且后面跟着文件名，这是下载意图
            - 如果消息直接说"下载XXX"（XXX是文件名），这是下载意图
            - 如果是URL（以http或https开头），使用url字段
            - 如果是具体的文件名或路径，使用filePath字段
            - 如果是模糊的文件名（需要搜索），使用query字段
            
            特别注意：
            - "下载OllamaChatController" 应该识别为下载意图，query为"OllamaChatController"
            - "下载pom.xml" 应该识别为下载意图，query为"pom.xml"
            - "下载 https://example.com/file.txt" 应该识别为下载意图，url为"https://example.com/file.txt"
            
            请返回JSON格式：
            {
                "intent": "意图类型",
                "parameters": {
                    "query": "搜索关键词",
                    "url": "下载URL",
                    "filePath": "文件路径",
                    "targetDirectory": "目标目录"
                }
            }
            
            只返回JSON，不要其他内容。
            """, userMessage);
        
        try {
            ChatResponse response = chatModel.call(new Prompt(prompt));
            String jsonResponse = response.getResult().getOutput().getText().trim();
            
            // 解析JSON响应
            return parseIntentFromJSON(jsonResponse);
        } catch (Exception e) {
            // 如果LLM分析失败，回退到正则表达式分析
            return analyzeIntentWithRegex(userMessage);
        }
    }

    /**
     * 正则表达式分析（作为备用方案）
     */
    private Intent analyzeIntentWithRegex(String userMessage) {
        String lowerMessage = userMessage.toLowerCase();
        
        // 文件搜索模式
        if (lowerMessage.contains("搜索") || lowerMessage.contains("查找") || lowerMessage.contains("find") || lowerMessage.contains("search")) {
            Pattern pattern = Pattern.compile("(搜索|查找|find|search)\\s*[：:]*\\s*(.+)");
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                return new Intent(IntentType.FILE_SEARCH, Map.of("query", matcher.group(2).trim()));
            }
        }
        
        // 文件下载模式
        if (lowerMessage.contains("下载") || lowerMessage.contains("download")) {
            // 匹配URL下载
            Pattern urlPattern = Pattern.compile("(下载|download)\\s*[：:]*\\s*(https?://\\S+)");
            Matcher urlMatcher = urlPattern.matcher(userMessage);
            if (urlMatcher.find()) {
                return new Intent(IntentType.FILE_DOWNLOAD, Map.of("url", urlMatcher.group(2)));
            }
            
            // 匹配本地文件下载 - 更宽松的匹配
            Pattern filePattern = Pattern.compile("(下载|download)\\s*[：:]*\\s*([\\w\\-\\.]+)");
            Matcher fileMatcher = filePattern.matcher(userMessage);
            if (fileMatcher.find()) {
                String fileName = fileMatcher.group(2);
                // 检查是否是文件路径（不是URL）
                if (!fileName.startsWith("http")) {
                    // 对于模糊的文件名，使用query字段进行搜索
                    return new Intent(IntentType.FILE_DOWNLOAD, Map.of("query", fileName));
                }
            }
            
            // 如果没有匹配到具体模式，但包含下载关键词，尝试提取文件名
            if (lowerMessage.contains("下载")) {
                // 提取"下载"后面的内容作为查询
                String[] parts = userMessage.split("下载");
                if (parts.length > 1) {
                    String query = parts[1].trim();
                    if (!query.isEmpty() && !query.startsWith("http")) {
                        return new Intent(IntentType.FILE_DOWNLOAD, Map.of("query", query));
                    }
                }
            }
        }
        
        // 文件读取模式
        if (lowerMessage.contains("读取") || lowerMessage.contains("打开") || lowerMessage.contains("read") || lowerMessage.contains("open")) {
            Pattern pattern = Pattern.compile("(读取|打开|read|open)\\s*[：:]*\\s*(\\S+)");
            Matcher matcher = pattern.matcher(userMessage);
            if (matcher.find()) {
                return new Intent(IntentType.FILE_READ, Map.of("filePath", matcher.group(2)));
            }
        }
        
        // 系统信息模式
        if (lowerMessage.contains("系统") || lowerMessage.contains("内存") || lowerMessage.contains("system") || lowerMessage.contains("memory")) {
            return new Intent(IntentType.SYSTEM_INFO, Map.of());
        }
        
        return new Intent(IntentType.GENERAL_CHAT, Map.of());
    }

    /**
     * 解析LLM返回的JSON意图
     */
    private Intent parseIntentFromJSON(String jsonResponse) {
        try {
            // 简单的JSON解析（实际项目中建议使用Jackson）
            jsonResponse = jsonResponse.replaceAll("```json\\s*|\\s*```", "");
            
            // 提取intent
            Pattern intentPattern = Pattern.compile("\"intent\"\\s*:\\s*\"([^\"]+)\"");
            Matcher intentMatcher = intentPattern.matcher(jsonResponse);
            if (!intentMatcher.find()) {
                return new Intent(IntentType.GENERAL_CHAT, Map.of());
            }
            
            String intentStr = intentMatcher.group(1);
            IntentType intentType = IntentType.valueOf(intentStr);
            
            // 提取parameters
            Map<String, String> parameters = new HashMap<>();
            
            // 提取query
            Pattern queryPattern = Pattern.compile("\"query\"\\s*:\\s*\"([^\"]+)\"");
            Matcher queryMatcher = queryPattern.matcher(jsonResponse);
            if (queryMatcher.find()) {
                parameters.put("query", queryMatcher.group(1));
            }
            
            // 提取url
            Pattern urlPattern = Pattern.compile("\"url\"\\s*:\\s*\"([^\"]+)\"");
            Matcher urlMatcher = urlPattern.matcher(jsonResponse);
            if (urlMatcher.find()) {
                parameters.put("url", urlMatcher.group(1));
            }
            
            // 提取filePath
            Pattern filePathPattern = Pattern.compile("\"filePath\"\\s*:\\s*\"([^\"]+)\"");
            Matcher filePathMatcher = filePathPattern.matcher(jsonResponse);
            if (filePathMatcher.find()) {
                parameters.put("filePath", filePathMatcher.group(1));
            }
            
            // 提取targetDirectory
            Pattern targetDirPattern = Pattern.compile("\"targetDirectory\"\\s*:\\s*\"([^\"]+)\"");
            Matcher targetDirMatcher = targetDirPattern.matcher(jsonResponse);
            if (targetDirMatcher.find()) {
                parameters.put("targetDirectory", targetDirMatcher.group(1));
            }
            
            return new Intent(intentType, parameters);
        } catch (Exception e) {
            return new Intent(IntentType.GENERAL_CHAT, Map.of());
        }
    }

    /**
     * 构建文件搜索的AI回复
     */
    private String buildFileSearchResponse(List<FileSearchService.FileInfo> files, List<FileSearchService.FileContentMatch> contentMatches, String query) {
        StringBuilder response = new StringBuilder();
        response.append("搜索结果：\n\n");
        
        if (!files.isEmpty()) {
            response.append("找到的文件：\n");
            for (int i = 0; i < Math.min(files.size(), 10); i++) {
                FileSearchService.FileInfo file = files.get(i);
                response.append(String.format("%d. %s (%s)\n", 
                    i + 1, file.getName(), formatFileSize(file.getSize())));
            }
            if (files.size() > 10) {
                response.append(String.format("... 还有 %d 个文件\n", files.size() - 10));
            }
        }
        
        if (!contentMatches.isEmpty()) {
            response.append("\n内容匹配：\n");
            for (int i = 0; i < Math.min(contentMatches.size(), 5); i++) {
                FileSearchService.FileContentMatch match = contentMatches.get(i);
                response.append(String.format("%d. %s (%d 处匹配)\n", 
                    i + 1, match.getFilePath(), match.getMatches().size()));
            }
            if (contentMatches.size() > 5) {
                response.append(String.format("... 还有 %d 个文件包含匹配内容\n", contentMatches.size() - 5));
            }
        }
        
        if (files.isEmpty() && contentMatches.isEmpty()) {
            response.append("未找到匹配的文件或内容。");
        }
        
        return response.toString();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // 意图类型枚举
    public enum IntentType {
        FILE_SEARCH, FILE_DOWNLOAD, FILE_READ, SYSTEM_INFO, GENERAL_CHAT
    }

    // 意图类
    public static class Intent {
        private final IntentType type;
        private final Map<String, String> parameters;

        public Intent(IntentType type, Map<String, String> parameters) {
            this.type = type;
            this.parameters = parameters;
        }

        public IntentType getType() { return type; }
        public Map<String, String> getParameters() { return parameters; }
    }

    // Agent响应类
    public static class AgentResponse {
        private final String action;
        private final String message;
        private final Map<String, Object> data;

        public AgentResponse(String action, String message, Map<String, Object> data) {
            this.action = action;
            this.message = message;
            this.data = data;
        }

        public String getAction() { return action; }
        public String getMessage() { return message; }
        public Map<String, Object> getData() { return data; }
    }
} 