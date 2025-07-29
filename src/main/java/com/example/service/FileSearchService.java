package com.example.service;

import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileSearchService {

    private static final int MAX_SEARCH_RESULTS = 100;
    private static final Set<String> EXCLUDED_DIRS = Set.of(
            ".git", ".idea", "target", "node_modules", ".vscode", 
            "build", "dist", ".gradle", ".mvn", "logs"
    );

    /**
     * 搜索文件
     */
    public List<FileInfo> searchFiles(String query, String basePath) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }

        Path searchPath = StringUtils.hasText(basePath) ? Paths.get(basePath) : Paths.get(".");
        
        if (!Files.exists(searchPath)) {
            return Collections.emptyList();
        }

        try {
            return Files.walk(searchPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(path))
                    .filter(path -> matchesQuery(path, query))
                    .limit(MAX_SEARCH_RESULTS)
                    .map(this::toFileInfo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 搜索文件内容
     */
    public List<FileContentMatch> searchFileContent(String query, String basePath) {
        if (!StringUtils.hasText(query)) {
            return Collections.emptyList();
        }

        Path searchPath = StringUtils.hasText(basePath) ? Paths.get(basePath) : Paths.get(".");
        
        if (!Files.exists(searchPath)) {
            return Collections.emptyList();
        }

        List<FileContentMatch> results = new ArrayList<>();
        
        try {
            Files.walk(searchPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> !isExcluded(path))
                    .filter(path -> isTextFile(path))
                    .forEach(path -> {
                        List<ContentMatch> matches = searchInFile(path, query);
                        if (!matches.isEmpty()) {
                            results.add(new FileContentMatch(path.toString(), matches));
                        }
                    });
        } catch (IOException e) {
            // 忽略错误
        }

        return results.stream()
                .limit(MAX_SEARCH_RESULTS)
                .collect(Collectors.toList());
    }

    /**
     * 获取文件列表
     */
    public List<FileInfo> listFiles(String directory) {
        Path dirPath = StringUtils.hasText(directory) ? Paths.get(directory) : Paths.get(".");
        
        if (!Files.exists(dirPath) || !Files.isDirectory(dirPath)) {
            return Collections.emptyList();
        }

        try {
            return Files.list(dirPath)
                    .map(this::toFileInfo)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            return Collections.emptyList();
        }
    }

    /**
     * 读取文件内容
     */
    public String readFileContent(String filePath) {
        try {
            Path path = Paths.get(filePath);
            if (!Files.exists(path) || !Files.isRegularFile(path)) {
                return null;
            }
            
            // 检查文件大小，避免读取过大的文件
            long fileSize = Files.size(path);
            if (fileSize > 10 * 1024 * 1024) { // 10MB限制
                return "文件过大，无法读取完整内容。文件大小: " + formatFileSize(fileSize);
            }
            
            return Files.readString(path);
        } catch (IOException e) {
            return null;
        }
    }

    private boolean isExcluded(Path path) {
        return EXCLUDED_DIRS.stream()
                .anyMatch(excluded -> path.toString().contains(excluded));
    }

    private boolean matchesQuery(Path path, String query) {
        String fileName = path.getFileName().toString().toLowerCase();
        String queryLower = query.toLowerCase();
        return fileName.contains(queryLower);
    }

    private boolean isTextFile(Path path) {
        String fileName = path.getFileName().toString().toLowerCase();
        return fileName.endsWith(".txt") || fileName.endsWith(".md") || 
               fileName.endsWith(".java") || fileName.endsWith(".xml") ||
               fileName.endsWith(".yml") || fileName.endsWith(".yaml") ||
               fileName.endsWith(".json") || fileName.endsWith(".properties") ||
               fileName.endsWith(".js") || fileName.endsWith(".ts") ||
               fileName.endsWith(".html") || fileName.endsWith(".css") ||
               fileName.endsWith(".py") || fileName.endsWith(".sh");
    }

    private List<ContentMatch> searchInFile(Path path, String query) {
        List<ContentMatch> matches = new ArrayList<>();
        try {
            List<String> lines = Files.readAllLines(path);
            String queryLower = query.toLowerCase();
            
            for (int i = 0; i < lines.size(); i++) {
                String line = lines.get(i);
                if (line.toLowerCase().contains(queryLower)) {
                    matches.add(new ContentMatch(i + 1, line.trim()));
                }
            }
        } catch (IOException e) {
            // 忽略错误
        }
        return matches;
    }

    private FileInfo toFileInfo(Path path) {
        try {
            BasicFileAttributes attrs = Files.readAttributes(path, BasicFileAttributes.class);
            return new FileInfo(
                    path.toString(),
                    path.getFileName().toString(),
                    attrs.isDirectory(),
                    attrs.size(),
                    attrs.lastModifiedTime().toMillis()
            );
        } catch (IOException e) {
            return new FileInfo(path.toString(), path.getFileName().toString(), 
                              Files.isDirectory(path), 0, 0);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.1f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    // 数据类
    public static class FileInfo {
        private final String path;
        private final String name;
        private final boolean isDirectory;
        private final long size;
        private final long lastModified;

        public FileInfo(String path, String name, boolean isDirectory, long size, long lastModified) {
            this.path = path;
            this.name = name;
            this.isDirectory = isDirectory;
            this.size = size;
            this.lastModified = lastModified;
        }

        // Getters
        public String getPath() { return path; }
        public String getName() { return name; }
        public boolean isDirectory() { return isDirectory; }
        public long getSize() { return size; }
        public long getLastModified() { return lastModified; }
    }

    public static class ContentMatch {
        private final int lineNumber;
        private final String content;

        public ContentMatch(int lineNumber, String content) {
            this.lineNumber = lineNumber;
            this.content = content;
        }

        public int getLineNumber() { return lineNumber; }
        public String getContent() { return content; }
    }

    public static class FileContentMatch {
        private final String filePath;
        private final List<ContentMatch> matches;

        public FileContentMatch(String filePath, List<ContentMatch> matches) {
            this.filePath = filePath;
            this.matches = matches;
        }

        public String getFilePath() { return filePath; }
        public List<ContentMatch> getMatches() { return matches; }
    }
} 