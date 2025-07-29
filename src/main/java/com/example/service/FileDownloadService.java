package com.example.service;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FileDownloadService {

    private final ConcurrentHashMap<String, DownloadTask> downloadTasks = new ConcurrentHashMap<>();
    private final AtomicLong taskIdCounter = new AtomicLong(0);

    /**
     * 下载文件（从URL）
     */
    public DownloadTask downloadFile(String url, String targetDirectory) {
        String taskId = "task_" + taskIdCounter.incrementAndGet();
        
        DownloadTask task = new DownloadTask(taskId, url, targetDirectory);
        downloadTasks.put(taskId, task);
        
        CompletableFuture.runAsync(() -> {
            try {
                performDownload(task);
            } catch (Exception e) {
                task.setStatus(DownloadStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
        });
        
        return task;
    }

    /**
     * 下载本地文件（复制到指定目录）
     */
    public DownloadTask downloadLocalFile(String sourceFilePath, String targetDirectory) {
        String taskId = "task_" + taskIdCounter.incrementAndGet();
        
        DownloadTask task = new DownloadTask(taskId, sourceFilePath, targetDirectory);
        task.setLocalPath(sourceFilePath);
        downloadTasks.put(taskId, task);
        
        CompletableFuture.runAsync(() -> {
            try {
                performLocalFileCopy(task);
            } catch (Exception e) {
                task.setStatus(DownloadStatus.FAILED);
                task.setErrorMessage(e.getMessage());
            }
        });
        
        return task;
    }

    /**
     * 获取下载任务状态
     */
    public DownloadTask getDownloadTask(String taskId) {
        return downloadTasks.get(taskId);
    }

    /**
     * 获取所有下载任务
     */
    public java.util.List<DownloadTask> getAllDownloadTasks() {
        return downloadTasks.values().stream().toList();
    }

    /**
     * 取消下载任务
     */
    public boolean cancelDownload(String taskId) {
        DownloadTask task = downloadTasks.get(taskId);
        if (task != null && task.getStatus() == DownloadStatus.DOWNLOADING) {
            task.setStatus(DownloadStatus.CANCELLED);
            return true;
        }
        return false;
    }

    /**
     * 删除下载任务
     */
    public boolean deleteDownloadTask(String taskId) {
        return downloadTasks.remove(taskId) != null;
    }

    /**
     * 下载本地文件
     */
    public ResponseEntity<Resource> downloadLocalFile(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return ResponseEntity.notFound().build();
            }

            Resource resource = new UrlResource(path.toUri());
            
            String contentType = determineContentType(path);
            String filename = path.getFileName().toString();

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                    .body(resource);
        } catch (IOException e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private void performDownload(DownloadTask task) {
        try {
            task.setStatus(DownloadStatus.DOWNLOADING);
            
            URL url = new URL(task.getUrl());
            URLConnection connection = url.openConnection();
            
            // 获取文件大小
            int contentLength = connection.getContentLength();
            task.setTotalSize(contentLength);
            
            // 确定文件名
            String fileName = getFileNameFromUrl(task.getUrl());
            if (!StringUtils.hasText(fileName)) {
                fileName = "downloaded_file_" + System.currentTimeMillis();
            }
            
            // 创建目标目录
            Path targetDir = Paths.get(task.getTargetDirectory());
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Path targetFile = targetDir.resolve(fileName);
            task.setLocalPath(targetFile.toString());
            
            // 开始下载
            try (var inputStream = connection.getInputStream()) {
                long downloadedBytes = Files.copy(inputStream, targetFile, StandardCopyOption.REPLACE_EXISTING);
                task.setDownloadedSize(downloadedBytes);
                task.setStatus(DownloadStatus.COMPLETED);
            }
            
        } catch (IOException e) {
            task.setStatus(DownloadStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }
    }

    private void performLocalFileCopy(DownloadTask task) {
        try {
            task.setStatus(DownloadStatus.DOWNLOADING);
            
            Path sourcePath = Paths.get(task.getUrl()); // 这里url字段存储的是源文件路径
            if (!Files.exists(sourcePath) || !Files.isRegularFile(sourcePath)) {
                throw new IOException("源文件不存在或不是普通文件: " + sourcePath);
            }
            
            // 获取文件大小
            long fileSize = Files.size(sourcePath);
            task.setTotalSize(fileSize);
            
            // 确定目标文件名
            String fileName = sourcePath.getFileName().toString();
            
            // 创建目标目录
            Path targetDir = Paths.get(task.getTargetDirectory());
            if (!Files.exists(targetDir)) {
                Files.createDirectories(targetDir);
            }
            
            Path targetFile = targetDir.resolve(fileName);
            
            // 复制文件
            long copiedBytes = Files.copy(sourcePath, targetFile, StandardCopyOption.REPLACE_EXISTING).getNameCount();
            task.setDownloadedSize(copiedBytes);
            task.setLocalPath(targetFile.toString());
            task.setStatus(DownloadStatus.COMPLETED);
            
        } catch (IOException e) {
            task.setStatus(DownloadStatus.FAILED);
            task.setErrorMessage(e.getMessage());
        }
    }

    private String getFileNameFromUrl(String url) {
        try {
            String fileName = url.substring(url.lastIndexOf('/') + 1);
            // 移除查询参数
            int queryIndex = fileName.indexOf('?');
            if (queryIndex > 0) {
                fileName = fileName.substring(0, queryIndex);
            }
            return fileName;
        } catch (Exception e) {
            return null;
        }
    }

    private String determineContentType(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        
        if (fileName.endsWith(".pdf")) return "application/pdf";
        if (fileName.endsWith(".txt")) return "text/plain";
        if (fileName.endsWith(".html") || fileName.endsWith(".htm")) return "text/html";
        if (fileName.endsWith(".css")) return "text/css";
        if (fileName.endsWith(".js")) return "application/javascript";
        if (fileName.endsWith(".json")) return "application/json";
        if (fileName.endsWith(".xml")) return "application/xml";
        if (fileName.endsWith(".zip")) return "application/zip";
        if (fileName.endsWith(".tar")) return "application/x-tar";
        if (fileName.endsWith(".gz")) return "application/gzip";
        if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) return "image/jpeg";
        if (fileName.endsWith(".png")) return "image/png";
        if (fileName.endsWith(".gif")) return "image/gif";
        if (fileName.endsWith(".mp4")) return "video/mp4";
        if (fileName.endsWith(".mp3")) return "audio/mpeg";
        
        return "application/octet-stream";
    }

    // 下载状态枚举
    public enum DownloadStatus {
        PENDING, DOWNLOADING, COMPLETED, FAILED, CANCELLED
    }

    // 下载任务类
    public static class DownloadTask {
        private final String taskId;
        private final String url;
        private final String targetDirectory;
        private String localPath;
        private DownloadStatus status = DownloadStatus.PENDING;
        private long totalSize = -1;
        private long downloadedSize = 0;
        private String errorMessage;
        private final long startTime = System.currentTimeMillis();

        public DownloadTask(String taskId, String url, String targetDirectory) {
            this.taskId = taskId;
            this.url = url;
            this.targetDirectory = targetDirectory;
        }

        // Getters and Setters
        public String getTaskId() { return taskId; }
        public String getUrl() { return url; }
        public String getTargetDirectory() { return targetDirectory; }
        public String getLocalPath() { return localPath; }
        public void setLocalPath(String localPath) { this.localPath = localPath; }
        public DownloadStatus getStatus() { return status; }
        public void setStatus(DownloadStatus status) { this.status = status; }
        public long getTotalSize() { return totalSize; }
        public void setTotalSize(long totalSize) { this.totalSize = totalSize; }
        public long getDownloadedSize() { return downloadedSize; }
        public void setDownloadedSize(long downloadedSize) { this.downloadedSize = downloadedSize; }
        public String getErrorMessage() { return errorMessage; }
        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
        public long getStartTime() { return startTime; }
        
        public double getProgress() {
            if (totalSize <= 0) return 0.0;
            return (double) downloadedSize / totalSize * 100.0;
        }
        
        public long getElapsedTime() {
            return System.currentTimeMillis() - startTime;
        }
    }
} 