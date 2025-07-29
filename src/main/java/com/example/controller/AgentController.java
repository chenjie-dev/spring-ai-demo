package com.example.controller;

import com.example.service.FileDownloadService;
import com.example.service.FileSearchService;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
@CrossOrigin(origins = "*")
public class AgentController {

    private final FileSearchService fileSearchService;
    private final FileDownloadService fileDownloadService;

    public AgentController(FileSearchService fileSearchService, FileDownloadService fileDownloadService) {
        this.fileSearchService = fileSearchService;
        this.fileDownloadService = fileDownloadService;
    }

    // ==================== 文件搜索相关接口 ====================

    /**
     * 搜索文件
     */
    @GetMapping("/search/files")
    public List<FileSearchService.FileInfo> searchFiles(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = ".") String basePath) {
        return fileSearchService.searchFiles(query, basePath);
    }

    /**
     * 搜索文件内容
     */
    @GetMapping("/search/content")
    public List<FileSearchService.FileContentMatch> searchFileContent(
            @RequestParam String query,
            @RequestParam(required = false, defaultValue = ".") String basePath) {
        return fileSearchService.searchFileContent(query, basePath);
    }

    /**
     * 列出目录文件
     */
    @GetMapping("/files/list")
    public List<FileSearchService.FileInfo> listFiles(
            @RequestParam(required = false, defaultValue = ".") String directory) {
        return fileSearchService.listFiles(directory);
    }

    /**
     * 读取文件内容
     */
    @GetMapping("/files/content")
    public Map<String, Object> readFileContent(@RequestParam String filePath) {
        String content = fileSearchService.readFileContent(filePath);
        if (content != null) {
            return Map.of(
                    "success", true,
                    "filePath", filePath,
                    "content", content
            );
        } else {
            return Map.of(
                    "success", false,
                    "error", "无法读取文件内容"
            );
        }
    }

    // ==================== 文件下载相关接口 ====================

    /**
     * 开始下载文件
     */
    @PostMapping("/download/start")
    public FileDownloadService.DownloadTask startDownload(@RequestBody Map<String, String> request) {
        String url = request.get("url");
        String targetDirectory = request.getOrDefault("targetDirectory", "./downloads");
        
        if (url == null || url.trim().isEmpty()) {
            throw new IllegalArgumentException("URL is required");
        }
        
        return fileDownloadService.downloadFile(url, targetDirectory);
    }

    /**
     * 下载本地文件
     */
    @PostMapping("/download/local")
    public FileDownloadService.DownloadTask downloadLocalFile(@RequestBody Map<String, String> request) {
        String filePath = request.get("filePath");
        String targetDirectory = request.getOrDefault("targetDirectory", "./downloads");
        
        if (filePath == null || filePath.trim().isEmpty()) {
            throw new IllegalArgumentException("File path is required");
        }
        
        return fileDownloadService.downloadLocalFile(filePath, targetDirectory);
    }

    /**
     * 获取下载任务状态
     */
    @GetMapping("/download/status/{taskId}")
    public FileDownloadService.DownloadTask getDownloadStatus(@PathVariable String taskId) {
        FileDownloadService.DownloadTask task = fileDownloadService.getDownloadTask(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Download task not found: " + taskId);
        }
        return task;
    }

    /**
     * 获取所有下载任务
     */
    @GetMapping("/download/tasks")
    public List<FileDownloadService.DownloadTask> getAllDownloadTasks() {
        return fileDownloadService.getAllDownloadTasks();
    }

    /**
     * 取消下载任务
     */
    @PostMapping("/download/cancel/{taskId}")
    public Map<String, Object> cancelDownload(@PathVariable String taskId) {
        boolean cancelled = fileDownloadService.cancelDownload(taskId);
        return Map.of(
                "success", cancelled,
                "message", cancelled ? "下载已取消" : "无法取消下载"
        );
    }

    /**
     * 删除下载任务
     */
    @DeleteMapping("/download/task/{taskId}")
    public Map<String, Object> deleteDownloadTask(@PathVariable String taskId) {
        boolean deleted = fileDownloadService.deleteDownloadTask(taskId);
        return Map.of(
                "success", deleted,
                "message", deleted ? "任务已删除" : "任务不存在"
        );
    }

    /**
     * 下载本地文件
     */
    @GetMapping("/download/local")
    public ResponseEntity<Resource> downloadLocalFile(@RequestParam String filePath) {
        return fileDownloadService.downloadLocalFile(filePath);
    }

    // ==================== 综合Agent接口 ====================

    /**
     * 智能文件搜索（结合AI分析）
     */
    @PostMapping("/smart-search")
    public Map<String, Object> smartSearch(@RequestBody Map<String, String> request) {
        String query = request.get("query");
        String basePath = request.getOrDefault("basePath", ".");
        
        if (query == null || query.trim().isEmpty()) {
            return Map.of("error", "查询内容不能为空");
        }

        // 执行文件搜索
        List<FileSearchService.FileInfo> files = fileSearchService.searchFiles(query, basePath);
        List<FileSearchService.FileContentMatch> contentMatches = fileSearchService.searchFileContent(query, basePath);

        return Map.of(
                "query", query,
                "basePath", basePath,
                "files", files,
                "contentMatches", contentMatches,
                "summary", Map.of(
                        "totalFiles", files.size(),
                        "totalContentMatches", contentMatches.size()
                )
        );
    }

    /**
     * 批量下载文件
     */
    @PostMapping("/batch-download")
    public List<FileDownloadService.DownloadTask> batchDownload(@RequestBody Map<String, Object> request) {
        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) request.get("urls");
        String targetDirectory = (String) request.getOrDefault("targetDirectory", "./downloads");
        
        if (urls == null || urls.isEmpty()) {
            throw new IllegalArgumentException("URLs list is required");
        }

        return urls.stream()
                .map(url -> fileDownloadService.downloadFile(url, targetDirectory))
                .toList();
    }

    /**
     * 获取系统信息
     */
    @GetMapping("/system/info")
    public Map<String, Object> getSystemInfo() {
        Runtime runtime = Runtime.getRuntime();
        long totalMemory = runtime.totalMemory();
        long freeMemory = runtime.freeMemory();
        long usedMemory = totalMemory - freeMemory;
        
        return Map.of(
                "memory", Map.of(
                        "total", formatBytes(totalMemory),
                        "used", formatBytes(usedMemory),
                        "free", formatBytes(freeMemory),
                        "usagePercent", String.format("%.1f%%", (double) usedMemory / totalMemory * 100)
                ),
                "downloads", Map.of(
                        "activeTasks", fileDownloadService.getAllDownloadTasks().stream()
                                .filter(task -> task.getStatus() == FileDownloadService.DownloadStatus.DOWNLOADING)
                                .count(),
                        "totalTasks", fileDownloadService.getAllDownloadTasks().size()
                ),
                "timestamp", System.currentTimeMillis()
        );
    }

    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
} 