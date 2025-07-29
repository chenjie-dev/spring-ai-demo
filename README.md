# Spring AI Ollama Demo with Agent

这是一个使用 Spring Boot 3 + Java 17 + Jackson 的 Spring AI 项目，集成了 Ollama 大语言模型和智能Agent功能。

## 项目特性

- 🚀 Spring Boot 3.2.0
- ☕ Java 17
- 🤖 Spring AI 1.0.0-M6
- 🎯 Ollama 集成
- 📝 Jackson JSON 处理
- 🌐 RESTful API
- 💬 实时聊天界面
- 🤖 智能Agent功能
- 🔍 本地文件搜索
- ⬇️ 文件下载管理
- 📊 系统监控

## 前置要求

1. **Java 17+**
2. **Maven 3.6+**
3. **Ollama** - 本地大语言模型服务

### 安装 Ollama

#### macOS
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

#### Linux
```bash
curl -fsSL https://ollama.ai/install.sh | sh
```

#### Windows
从 [Ollama官网](https://ollama.ai/download) 下载安装包。

### 启动 Ollama 并下载模型

```bash
# 启动 Ollama 服务
ollama serve

# 在另一个终端下载 llama3 模型
ollama pull llama3
```

## 快速开始

### 1. 克隆项目

```bash
git clone <your-repo-url>
cd spring-ai-demo
```

### 2. 配置应用

编辑 `src/main/resources/application.yml`：

```yaml
spring:
  ai:
    ollama:
      base-url: http://localhost:11434  # Ollama 服务地址
      chat:
        model: llama3                   # 使用的模型名称
        options:
          temperature: 0.7              # 温度参数
          top-p: 0.9                    # Top-p 参数
          max-tokens: 1000              # 最大令牌数
```

### 3. 运行应用

```bash
# 使用 Maven
mvn spring-boot:run

# 或者打包后运行
mvn clean package
java -jar target/spring-ai-demo-1.0.0.jar
```

### 4. 访问应用

- 聊天界面: http://localhost:8080
- Agent管理面板: http://localhost:8080/agent.html
- API 文档: http://localhost:8080/api/chat/health

## API 接口

### 聊天相关接口

#### 1. 健康检查
```
GET /api/chat/health
```

#### 2. 简单聊天
```
GET /api/chat/simple
```

#### 3. 发送消息（支持Agent功能）
```
POST /api/chat/message
Content-Type: application/json

{
  "message": "搜索 pom.xml"
}
```

#### 4. Agent智能处理
```
POST /api/chat/agent
Content-Type: application/json

{
  "message": "下载 https://example.com/file.txt"
}
```

#### 5. 流式聊天
```
POST /api/chat/stream
Content-Type: application/json

{
  "message": "请写一首诗"
}
```

#### 6. 多轮对话
```
POST /api/chat/conversation
Content-Type: application/json

{
  "messages": [
    {"role": "user", "content": "你好"},
    {"role": "assistant", "content": "你好！有什么可以帮助你的吗？"}
  ],
  "newMessage": "请介绍一下Spring Boot"
}
```

### Agent功能接口

#### 7. 文件搜索
```
GET /api/agent/search/files?query=pom.xml&basePath=.
GET /api/agent/search/content?query=spring&basePath=.
```

#### 8. 文件操作
```
GET /api/agent/files/list?directory=.
GET /api/agent/files/content?filePath=README.md
```

#### 9. 文件下载
```
# 从URL下载
POST /api/agent/download/start
Content-Type: application/json

{
  "url": "https://example.com/file.txt",
  "targetDirectory": "./downloads"
}

# 下载本地文件
POST /api/agent/download/local
Content-Type: application/json

{
  "filePath": "/path/to/local/file.txt",
  "targetDirectory": "./downloads"
}
```

#### 10. 下载管理
```
GET /api/agent/download/tasks
GET /api/agent/download/status/{taskId}
POST /api/agent/download/cancel/{taskId}
DELETE /api/agent/download/task/{taskId}
```

#### 11. 系统信息
```
GET /api/agent/system/info
```

## 项目结构

```
spring-ai-demo/
├── src/
│   ├── main/
│   │   ├── java/com/example/
│   │   │   ├── SpringAiDemoApplication.java
│   │   │   ├── controller/
│   │   │   │   ├── OllamaChatController.java
│   │   │   │   └── AgentController.java
│   │   │   └── service/
│   │   │       ├── AgentService.java
│   │   │       ├── FileSearchService.java
│   │   │       └── FileDownloadService.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── static/
│   │           ├── index.html
│   │           └── agent.html
│   └── test/
├── pom.xml
└── README.md
```

## 配置说明

### Ollama 配置

在 `application.yml` 中可以配置以下 Ollama 参数：

- `base-url`: Ollama 服务地址
- `chat.model`: 使用的模型名称
- `chat.options.temperature`: 控制输出的随机性 (0.0-1.0)
- `chat.options.top-p`: 核采样参数 (0.0-1.0)
- `chat.options.max-tokens`: 最大输出令牌数

### Agent 功能

项目集成了智能Agent功能，支持以下操作：

#### 文件搜索
- 文件名搜索：支持按文件名模糊搜索
- 内容搜索：在文本文件中搜索指定内容
- 智能搜索：结合AI分析搜索结果

#### 文件下载
- 支持HTTP/HTTPS文件下载
- 智能下载本地文件（先搜索再确认）
- 支持多种确认方式：数字、中文、文件名、部分文件名
- 异步下载任务管理
- 下载进度监控
- 支持取消和删除下载任务

#### 系统监控
- 内存使用情况监控
- 下载任务状态监控
- 实时系统信息展示

### 支持的模型

确保在 Ollama 中已下载以下模型之一：

```bash
# 下载常用模型
ollama pull llama3
ollama pull llama3.2
ollama pull codellama
ollama pull mistral
ollama pull qwen2.5
```

## 故障排除

### 1. 连接错误

如果遇到连接错误，请检查：

- Ollama 服务是否正在运行 (`ollama serve`)
- 端口 11434 是否可访问
- 防火墙设置

### 2. 模型未找到

如果遇到模型错误，请确保：

- 模型已正确下载 (`ollama list`)
- 模型名称在配置中正确指定
- 有足够的磁盘空间

### 3. 内存不足

如果遇到内存问题：

- 减少 `max-tokens` 参数
- 使用更小的模型
- 增加系统内存

## 使用示例

### 基本聊天
```bash
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "你好，请介绍一下你自己"}'
```

### Agent智能处理
```bash
# 搜索文件
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "搜索 pom.xml"}'

# 下载网络文件
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "下载 https://example.com/file.txt"}'

# 智能下载文件（会先搜索再确认）
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "下载 pom.xml"}'

# 或者直接说文件名
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "下载OllamaChatController"}'

# 确认下载时支持多种输入方式：
# - 数字：1、2、3...
# - 中文：第一个、第二个...
# - 文件名：pom.xml、README.md...
# - 部分文件名：pom、readme...

# 读取文件
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "读取 README.md"}'

# 查看系统信息
curl -X POST http://localhost:8080/api/chat/message \
  -H "Content-Type: application/json" \
  -d '{"message": "系统信息"}'
```

### 直接API调用
```bash
# 文件搜索
curl "http://localhost:8080/api/agent/search/files?query=pom.xml"

# 开始下载
curl -X POST http://localhost:8080/api/agent/download/start \
  -H "Content-Type: application/json" \
  -d '{"url": "https://example.com/file.txt", "targetDirectory": "./downloads"}'

# 下载本地文件
curl -X POST http://localhost:8080/api/agent/download/local \
  -H "Content-Type: application/json" \
  -d '{"filePath": "/path/to/local/file.txt", "targetDirectory": "./downloads"}'

# 获取系统信息
curl http://localhost:8080/api/agent/system/info
```

### 流式聊天
```bash
curl -X POST http://localhost:8080/api/chat/stream \
  -H "Content-Type: application/json" \
  -d '{"message": "请写一首诗"}' \
  --no-buffer
```

## 开发

### 添加新功能

1. 在 `OllamaChatController` 或 `AgentController` 中添加新的端点
2. 更新前端界面 (`index.html` 或 `agent.html`)
3. 添加相应的测试

### 自定义模型

可以通过修改 `application.yml` 来使用不同的模型：

```yaml
spring:
  ai:
    ollama:
      chat:
        model: your-custom-model
```

## 许可证

MIT License

## 贡献

欢迎提交 Issue 和 Pull Request！ 