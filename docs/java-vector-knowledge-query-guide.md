# Java 中知识向量库查询学习文档

## 1. 这份文档讲什么

这份文档专门讲：

- 在 Java 中怎么做知识向量库查询
- 整体步骤是什么
- 代码大概怎么写
- 查询后怎么交给 AI 回答

如果你现在想做的是：

- 企业知识库问答
- RAG
- 文档检索
- AI 助手

那这份文档就是你要看的入门版流程。

---

## 2. 先说结论

在 Java 里做知识向量库查询，标准流程一般是：

1. 准备知识文档
2. 对文档做切片
3. 生成每个切片的向量
4. 把切片和向量存入向量数据库
5. 用户提问时，把问题也转成向量
6. 用问题向量去向量库做相似度搜索
7. 取出最相关的若干条内容
8. 判断命中质量
9. 命中高就基于知识库回答
10. 命中低就走兜底逻辑

这就是最常见的 `RAG` 流程。

---

## 3. 你要先搞清楚几个角色

### 3.1 原始知识

原始知识可以来自：

- 文档
- FAQ
- 数据库文本字段
- Markdown
- PDF
- 网站说明页

### 3.2 Embedding 模型

它负责把文本变成向量。

例如：

```text
“如何创建 API Key”
-> [0.23, -0.51, 0.92, ...]
```

### 3.3 向量数据库

它负责：

- 存向量
- 相似度搜索

常见向量数据库：

- `pgvector`
- `Milvus`
- `Qdrant`

### 3.4 大模型

它负责根据检索结果组织自然语言答案。

---

## 4. Java 中最推荐的新手方案

如果你是 Java 入门阶段，我建议先学这一套：

```text
Spring Boot + Spring AI + PostgreSQL + pgvector
```

为什么推荐这个组合：

- 上手快
- Java 生态比较顺
- 容易和现有 Spring Boot 项目整合
- 适合第一版知识库问答

---

## 5. 整体流程图

```text
原始文档
-> 文本清洗
-> 文档切片
-> 调 embedding
-> 写入向量库

用户问题
-> 调 embedding
-> 相似度搜索
-> 取 TopK
-> 阈值判断
-> 把上下文交给大模型
-> 返回答案
```

---

## 6. 第一步：准备知识数据

知识数据可以是：

- 一篇说明文档
- 一批 FAQ
- 数据库中的帮助中心内容

例如：

```text
标题：如何创建 API Key
内容：登录后台后进入 API Keys 管理页面，点击创建按钮即可生成密钥。
```

这时候还不能直接检索，必须继续做切片和向量化。

---

## 7. 第二步：文档切片

不要把整篇长文档直接存成一个向量。

更好的方式是切成多个片段。

### 7.1 为什么要切片

因为整篇文档太长会导致：

- 检索不准
- 召回粒度太粗
- 上下文太长

### 7.2 常见切片策略

- 按标题切
- 按段落切
- 按固定长度切

### 7.3 一个最简单的 Java 切片工具

```java
import java.util.ArrayList;
import java.util.List;

public class TextSplitUtil {

    public static List<String> splitByLength(String text, int chunkSize) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        for (int i = 0; i < text.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, text.length());
            result.add(text.substring(i, end));
        }

        return result;
    }
}
```

这个是最基础版本，先够你学习使用。

---

## 8. 第三步：把切片转换成向量

每个切片都要调用 embedding 模型。

例如：

```text
切片文本：
“登录后台后进入 API Keys 管理页面，点击创建按钮即可生成密钥。”

转换后：
[0.12, -0.33, 0.85, ...]
```

如果你使用 Spring AI，这一步通常可以交给 `EmbeddingModel` 或 `VectorStore` 来处理。

---

## 9. 第四步：写入向量库

入库时，建议保存：

- 切片文本
- 向量
- 元数据

元数据例如：

- 文档标题
- 来源
- 业务 ID
- 标签

这样后续查到结果时，你才知道这段内容来自哪里。

---

## 10. 一个最小的知识入库代码示例

下面是一个简单的 Java 入库思路，使用 `Spring AI` 的 `VectorStore`：

```java
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeImportService {

    private final VectorStore vectorStore;

    public KnowledgeImportService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public void importText(String title, String content) {
        List<String> chunks = TextSplitUtil.splitByLength(content, 500);

        List<Document> docs = chunks.stream()
                .map(chunk -> new Document(
                        "标题：" + title + "\n内容：" + chunk
                ))
                .toList();

        vectorStore.add(docs);
    }
}
```

这段代码的意义是：

1. 先切片
2. 每段转成 `Document`
3. 调用 `vectorStore.add(...)`
4. 底层完成向量化和写库

---

## 11. 第五步：用户提问时做向量检索

当用户提问时，不是去查“完全一样的问题”，而是去查“语义相近的内容”。

例如用户问：

```text
密码忘了怎么办？
```

系统可能召回：

```text
管理员密码重置流程
```

这就是向量检索的意义。

---

## 12. Java 中的查询代码示例

下面是最常见的相似度搜索写法：

```java
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class KnowledgeSearchService {

    private final VectorStore vectorStore;

    public KnowledgeSearchService(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
    }

    public List<Document> search(String question) {
        SearchRequest request = SearchRequest.builder()
                .query(question)
                .topK(3)
                .similarityThreshold(0.75)
                .build();

        return vectorStore.similaritySearch(request);
    }
}
```

这段代码的意思是：

- 用用户问题做查询
- 取最相似的前 `3` 条
- 相似度低于 `0.75` 的结果直接过滤掉

---

## 13. 第六步：为什么要做阈值判断

很多人会写成：

```text
只要查到结果就直接用
```

这其实不够安全。

正确做法是：

- 看有没有结果
- 看最高分够不够高
- 看结果内容是否真的相关

也就是说，核心不是“查到了没有”，而是“查得准不准”。

---

## 14. 第七步：把检索结果交给大模型

向量库只负责找内容，不负责组织自然语言答案。

真正回答用户，通常还是靠大模型。

### 14.1 RAG 的基本流程

```text
用户问题
-> 向量检索
-> 召回若干文档片段
-> 将片段拼成上下文
-> 把上下文发给大模型
-> 大模型回答
```

### 14.2 Java 代码示例

```java
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RagChatService {

    private final KnowledgeSearchService knowledgeSearchService;
    private final ChatClient chatClient;

    public RagChatService(KnowledgeSearchService knowledgeSearchService,
                          ChatClient.Builder chatClientBuilder) {
        this.knowledgeSearchService = knowledgeSearchService;
        this.chatClient = chatClientBuilder.build();
    }

    public String ask(String question) {
        List<Document> docs = knowledgeSearchService.search(question);

        if (docs == null || docs.isEmpty()) {
            return "知识库中没有找到明确答案。";
        }

        String context = docs.stream()
                .map(Document::getText)
                .collect(Collectors.joining("\n\n"));

        return chatClient.prompt()
                .system("你是知识库助手，只能根据提供的上下文回答；如果上下文不足，就明确说不知道。")
                .user("""
                        问题：
                        %s

                        上下文：
                        %s
                        """.formatted(question, context))
                .call()
                .content();
    }
}
```

---

## 15. 一个最小可理解的业务流程

你可以把系统理解成下面这样：

### 15.1 建库流程

```text
导入文档
-> 切片
-> 向量化
-> 写入向量库
```

### 15.2 问答流程

```text
用户提问
-> 问题向量化
-> 相似度检索
-> 取 TopK
-> 阈值判断
-> 大模型基于上下文回答
```

---

## 16. PGvector 配置思路示例

如果你使用 `Spring AI + PGvector`，可以这样配一个最基础的 `VectorStore`：

```java
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
public class VectorConfig {

    @Bean
    public VectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
                .dimensions(1536)
                .initializeSchema(true)
                .build();
    }
}
```

这里的意思是：

- 使用 PostgreSQL 的 `pgvector`
- 向量维度是 `1536`
- 如果表结构不存在就自动初始化

注意：

你的 embedding 模型维度如果不是 `1536`，这里要改成一致。

---

## 17. 你在项目中通常会拆哪些类

推荐的类划分如下：

- `KnowledgeImportService`
- `KnowledgeSearchService`
- `RagChatService`
- `TextSplitUtil`
- `VectorConfig`

如果你还要提供接口，可以加：

- `KnowledgeController`
- `ChatController`

这样结构会比较清晰。

---

## 18. 一个简单接口设计示例

### 18.1 导入知识

```http
POST /knowledge/import
```

请求体：

```json
{
  "title": "如何创建 API Key",
  "content": "登录后台后进入 API Keys 管理页面..."
}
```

### 18.2 提问

```http
POST /chat/ask
```

请求体：

```json
{
  "question": "怎么生成 API Key？"
}
```

---

## 19. 一个更稳妥的回答策略

建议不要简单写成：

```text
查到了就回答
查不到就自由发挥
```

更稳妥的是：

```text
高命中 -> 基于知识库回答
低命中 -> 提示知识库未找到明确答案
必要时再附加通用 AI 回答
```

这样可以减少模型乱编。

---

## 20. 你最容易踩的坑

### 20.1 不切片

整篇文档直接向量化，检索通常不准。

### 20.2 切片太大

会让召回结果过于粗糙。

### 20.3 切片太小

上下文不完整，模型容易理解不全。

### 20.4 不做阈值判断

会导致“明明没查准，也硬回答”。

### 20.5 Prompt 不加限制

如果不告诉模型“只能根据上下文回答”，它很容易混入自己的通用知识。

---

## 21. 你应该记住的核心主线

整套流程最重要的主线是：

```text
知识文档
-> 切片
-> embedding
-> 向量库

用户问题
-> embedding
-> similaritySearch
-> TopK
-> threshold
-> LLM answer
```

只要这条线打通了，你就真正入门了 Java 里的知识向量库查询。

---

## 22. 最后的建议

如果你现在刚开始学，不要一上来就追求：

- 自己写向量数据库
- 自己写 ANN 索引
- 很复杂的多路召回

最好的顺序是：

1. 先跑通最小闭环
2. 再优化切片和检索效果
3. 再考虑更复杂的工程化能力

先把“能查到、能回答、逻辑清楚”做出来，比什么都重要。
