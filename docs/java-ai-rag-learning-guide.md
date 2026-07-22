# Java 接入 AI 与向量数据库学习文档

## 1. 这份文档适合谁

这份文档适合下面这类学习目标：

- 你想用 Java 接入大模型
- 你想做一个“先查知识库，再让 AI 回答”的问答系统
- 你正在学 Spring Boot，希望知道项目应该怎么设计

如果用一句话概括你的目标，就是：

> 用户提问后，系统先从知识库中检索相关内容；如果检索结果足够可靠，就基于知识库回答；如果没有命中或命中太差，再走大模型通用回答。

这套方案通常叫做 `RAG`，即 `Retrieval-Augmented Generation`，中文常叫“检索增强生成”。

---

## 2. 你的思路对不对

你的原始想法是：

1. 用 Java 接入 AI
2. 再写一个向量数据库
3. 用户提问时，先从数据库获取
4. 如果没有，再让 AI 回答

这个方向整体是对的，但有两个地方需要修正。

### 2.1 不要先判断“有没有这条问题”

向量数据库不是查“完全一样的问题”，而是查“语义相似的内容”。

比如：

- 用户问题：`怎么重置管理员密码？`
- 知识库文档：`后台管理员密码修改流程`

虽然字面不同，但语义上非常接近。向量检索就是专门解决这种问题的。

### 2.2 不要只看“有没有结果”，还要看“结果质量”

更合理的逻辑不是：

```text
查到了 -> 直接回答
查不到 -> 调 AI
```

而应该是：

```text
先做向量检索
如果检索结果分数足够高 -> 基于知识库回答
如果检索结果分数太低 -> 走 AI 通用回答或明确说不知道
```

也就是说，核心不只是“有没有”，而是：

- `相似度是否足够高`
- `检索出的内容是否可信`
- `上下文是否足够支持回答`

---

## 3. 正确的整体流程

一个标准的 Java + AI + 向量库问答系统，大致流程如下：

### 3.1 数据准备阶段

1. 收集知识文档
2. 对文档进行切片
3. 调用嵌入模型，把每个切片转成向量
4. 把切片文本、向量、元数据存入向量数据库

### 3.2 用户提问阶段

1. 接收用户问题
2. 把问题转成向量
3. 去向量数据库检索最相似的若干条内容
4. 判断最相似结果是否达到阈值
5. 如果达到阈值，把检索到的内容作为上下文交给大模型
6. 如果没达到阈值，走兜底策略

### 3.3 兜底策略

兜底策略常见有三种：

- 返回：`知识库中没有找到明确答案`
- 返回：`我可以基于通用模型尝试回答，但结果未必来自你的知识库`
- 直接走通用 AI 回答

如果是公司业务系统，通常更推荐前两种，因为更安全。

---

## 4. 先搞清楚几个核心概念

### 4.1 大模型

大模型负责“理解问题”和“组织答案”。

它擅长：

- 自然语言理解
- 内容生成
- 多轮对话
- 总结归纳

它不擅长：

- 保证你的私有知识一定正确
- 实时知道你数据库里的所有最新内容
- 自动知道企业内部规则

所以我们才需要知识库和向量检索。

### 4.2 Embedding

`Embedding` 可以理解成“把一句话变成一个向量数组”。

例如：

```text
“怎么创建 API Key”
-> [0.125, -0.883, 0.317, ...]
```

这样做的目的，是让语义相近的文本在向量空间中也更接近。

### 4.3 向量数据库

向量数据库用来保存这些向量，并支持“相似度搜索”。

你问一个问题时，系统把问题也转成向量，再去找“距离最近”的文档片段。

常见向量库：

- `pgvector`：适合已经在用 PostgreSQL 的项目
- `Milvus`：适合独立的专业向量检索服务
- `Qdrant`：适合做 RAG 和过滤检索

如果你是 Java 初学阶段，我最建议先学 `pgvector`。

### 4.4 文档切片

一篇完整文档通常太长，不能直接整篇作为一个向量。

所以一般会把文档切成多个片段，例如：

- 每段 `300` 到 `800` 字
- 或者按标题、段落、小节来切

这样检索更精准，也更适合给大模型当上下文。

### 4.5 相似度阈值

系统不能只看“有结果”，还要看“结果像不像”。

例如：

- 最高分大于 `0.8`：高置信度
- 最高分在 `0.65 ~ 0.8`：中等，需要谨慎
- 最高分低于 `0.65`：通常视为不命中

注意：

不同模型、不同向量库、不同距离算法下，分值范围可能不同。阈值需要你自己测试。

---

## 5. 推荐你的第一版技术方案

如果你的目标是“先学会，再逐步做复杂”，我推荐第一版这样搭：

- `Java 17`
- `Spring Boot`
- `PostgreSQL + pgvector`
- `OpenAI 兼容接口` 或其他大模型接口
- `JDBC` 或 `Spring AI`

### 5.1 为什么不建议你一开始自己写向量数据库

因为“写一个向量数据库”这件事本身就很复杂，里面至少包括：

- 向量存储
- 距离计算
- 索引结构
- TopK 检索
- 过滤条件
- 性能优化
- 持久化

如果你的目标是做问答系统，而不是研究数据库底层，直接用现成向量库会更有效率。

更好的学习顺序是：

1. 先学会“怎么用向量库做问答”
2. 再研究“向量库底层原理”
3. 最后如果你有兴趣，再自己尝试写一个简单版

---

## 6. 推荐的项目分层

你可以把系统拆成下面几层：

### 6.1 控制层

负责接收 HTTP 请求。

例如：

- `POST /knowledge/import`
- `POST /chat/ask`

### 6.2 服务层

负责核心业务逻辑。

建议拆成：

- `EmbeddingService`
- `VectorStoreService`
- `KnowledgeService`
- `ChatService`
- `RagService`

### 6.3 数据层

负责数据库读写。

例如：

- 保存知识文档
- 保存知识切片
- 保存向量
- 相似度检索

### 6.4 AI 接入层

专门负责调用外部模型接口。

例如：

- 生成 embedding
- 调用 chat completion

这样设计的好处是：

- 后面换模型更方便
- 后面换向量库更方便
- 业务逻辑不会全堆在 controller 里

---

## 7. 一套你能真正跑起来的表设计思路

下面给你的是学习版设计，不是最终生产版，但很适合入门。

### 7.1 知识文档表

```sql
create table knowledge_document (
    id bigserial primary key,
    title varchar(255) not null,
    source varchar(255),
    content text not null,
    status varchar(32) not null default 'ENABLED',
    created_at timestamp not null default current_timestamp
);
```

### 7.2 文档切片表

如果你使用 `pgvector`，可以这样设计：

```sql
create extension if not exists vector;

create table knowledge_chunk (
    id bigserial primary key,
    document_id bigint not null,
    chunk_index int not null,
    chunk_text text not null,
    embedding vector(1536),
    created_at timestamp not null default current_timestamp
);
```

其中：

- `document_id` 表示这个切片属于哪篇文档
- `chunk_index` 表示这是第几段
- `chunk_text` 表示切片文本
- `embedding` 表示向量

如果你的 embedding 维度不是 `1536`，这里也要跟着改。

---

## 8. 用户提问时的核心逻辑

### 8.1 标准版流程

```text
用户提问
-> 生成问题向量
-> 检索 TopK 文档切片
-> 判断最高分是否超过阈值
-> 命中：将切片内容拼成上下文
-> 调用大模型，要求“只能基于上下文回答”
-> 未命中：走兜底逻辑
```

### 8.2 Java 伪代码

```java
public String ask(String question) {
    List<Float> queryEmbedding = embeddingService.embed(question);

    List<KnowledgeChunk> chunks = vectorStoreService.searchTopK(queryEmbedding, 3);

    if (chunks.isEmpty()) {
        return llmService.generalAnswer(question);
    }

    double bestScore = chunks.get(0).getScore();

    if (bestScore < 0.75) {
        return llmService.generalAnswer(question);
    }

    String context = chunks.stream()
            .map(KnowledgeChunk::getChunkText)
            .collect(Collectors.joining("\n\n"));

    return llmService.answerWithContext(question, context);
}
```

上面这段逻辑已经比“有就查，没有就问 AI”更准确了。

---

## 9. 更推荐的回答策略

如果系统是给自己练手，你可以这样写：

```text
命中高 -> 基于知识库回答
命中低 -> 通用 AI 回答
```

如果系统是给公司业务或正式用户使用，我更建议：

```text
命中高 -> 基于知识库回答
命中低 -> 提示“知识库未命中”
必要时再附加通用回答，并明确标注不是知识库结论
```

原因是：

- 大模型自由回答可能会编
- 用户会误以为是你系统里的真实知识
- 一旦涉及业务规则、价格、政策、权限，就容易出问题

---

## 10. Prompt 应该怎么写

当你已经检索到知识片段后，不要直接把问题发给模型，而是要带上明确约束。

### 10.1 示例 Prompt

```text
你是一个知识库问答助手。
请严格基于提供的上下文回答问题。
如果上下文中没有明确答案，请直接回答“知识库中没有找到明确答案”，不要编造。

问题：
{{question}}

上下文：
{{context}}
```

这个约束非常重要，否则即使你做了向量检索，模型还是可能脱离上下文自由发挥。

---

## 11. 文档入库流程怎么做

知识库系统不只是“问答”，还有“建库”。

### 11.1 入库步骤

1. 接收原始文档
2. 清洗文本
3. 按段落或长度切片
4. 对每个切片生成 embedding
5. 写入数据库

### 11.2 Java 伪代码

```java
public void importDocument(String title, String content) {
    Long docId = knowledgeRepository.saveDocument(title, content);

    List<String> chunks = textSplitService.split(content);

    for (int i = 0; i < chunks.size(); i++) {
        String chunk = chunks.get(i);
        List<Float> embedding = embeddingService.embed(chunk);
        knowledgeRepository.saveChunk(docId, i, chunk, embedding);
    }
}
```

---

## 12. 你可以怎么拆类

下面是一套比较适合 Spring Boot 入门项目的类划分：

### 12.1 Controller

- `KnowledgeController`
- `ChatController`

### 12.2 Service

- `KnowledgeImportService`
- `TextSplitService`
- `EmbeddingService`
- `VectorSearchService`
- `LlmService`
- `RagChatService`

### 12.3 Repository

- `KnowledgeDocumentRepository`
- `KnowledgeChunkRepository`

### 12.4 DTO

- `AskRequest`
- `AskResponse`
- `ImportKnowledgeRequest`

这样拆完以后，你写代码时思路会清楚很多。

---

## 13. 一个简单但正确的接口设计

### 13.1 导入知识

```http
POST /knowledge/import
```

请求体：

```json
{
  "title": "管理员密码重置流程",
  "content": "这里是完整文档内容"
}
```

### 13.2 提问

```http
POST /chat/ask
```

请求体：

```json
{
  "question": "管理员密码忘了怎么处理？"
}
```

响应体可以设计成：

```json
{
  "answer": "根据知识库内容，管理员可通过后台安全设置页面重置密码。",
  "source": "KNOWLEDGE_BASE",
  "score": 0.86
}
```

如果没有命中：

```json
{
  "answer": "知识库中没有找到明确答案。",
  "source": "FALLBACK",
  "score": 0.42
}
```

这样前端也能知道这次回答是不是来自知识库。

---

## 14. 第一版不要追求太复杂

很多人一上来就想做下面这些：

- 多路召回
- 混合检索
- 重排序
- 多租户
- 多模型切换
- 流式输出
- 权限隔离

这些都可以做，但不适合作为第一步。

第一版建议你只完成这四件事：

1. 能导入文档
2. 能把文档切片并生成向量
3. 能按问题检索最相似切片
4. 能把切片作为上下文交给 AI 回答

只要这四步跑通，你就已经真正入门了。

---

## 15. 学习顺序建议

### 第一步：先理解概念

先吃透这几个关键词：

- `LLM`
- `Embedding`
- `Vector Search`
- `TopK`
- `Threshold`
- `RAG`

### 第二步：先跑通最小闭环

目标是跑通：

```text
导入文档 -> 切片 -> 生成向量 -> 向量检索 -> AI 回答
```

### 第三步：再优化效果

优化方向通常包括：

- 切片大小
- TopK 数量
- 相似度阈值
- Prompt 约束
- 检索结果排序

### 第四步：再考虑工程化

例如：

- 日志
- 重试
- 限流
- 鉴权
- 监控
- 缓存

---

## 16. 你最容易踩的坑

### 16.1 误把“问答记录”当成“知识库”

知识库应该存“知识内容”，而不是只存“别人问过什么”。

更适合存入向量库的是：

- 产品文档
- FAQ
- 流程说明
- 制度规则
- 接口文档

### 16.2 切片太大

切片太大，会导致：

- 检索不准
- 上下文过长
- 成本变高

### 16.3 切片太小

切片太小，会导致上下文不完整，模型读不懂。

### 16.4 不做阈值判断

这是最常见的问题之一。

如果不做阈值判断，模型会拿低质量检索结果硬答，最后看起来像“查到了”，其实是在乱答。

### 16.5 Prompt 不加约束

如果你没有明确告诉模型“只能基于上下文回答”，它很可能会把自己的通用知识也掺进来。

### 16.6 一开始自己造全套轮子

学习可以造轮子，做项目不建议全造。

建议：

- 向量库直接用现成的
- 你重点学习“RAG 业务流程”和“系统设计”

---

## 17. 一个很适合你的最小实现方案

如果你现在就准备开始做，我建议你的第一个版本这样安排：

### 17.1 技术栈

- `Spring Boot`
- `PostgreSQL`
- `pgvector`
- `JDBC Template` 或 `MyBatis`
- `OpenAI 兼容 Chat 接口`
- `OpenAI 兼容 Embedding 接口`

### 17.2 先实现的功能

- 上传一篇知识文档
- 自动切成多个片段
- 生成 embedding 并入库
- 提问时检索 `Top 3`
- 如果最高分大于阈值，就基于上下文回答
- 如果最高分不够，就返回兜底信息

### 17.3 暂时不要做的功能

- 自己写 ANN 索引
- 自己写向量数据库引擎
- 多用户隔离
- 复杂后台管理
- 自动训练

---

## 18. 你后续可以升级的方向

当你把第一版做完后，可以继续往下升级：

### 18.1 检索增强

- 增加 `TopK`
- 增加关键词检索
- 做混合检索
- 做重排序

### 18.2 数据增强

- 给文档加标签
- 给切片加来源
- 增加更新时间
- 支持多知识库

### 18.3 回答增强

- 返回引用来源
- 返回命中片段
- 支持流式输出
- 支持多轮会话

### 18.4 工程增强

- 缓存热门问题
- 增加审计日志
- 增加失败重试
- 增加接口鉴权

---

## 19. 一句话记住这套系统

你做的不是：

```text
数据库里有没有这个问题
```

而是：

```text
知识库里有没有和这个问题语义接近、足够可靠的内容
```

这是 RAG 系统最关键的思维转换。

---

## 20. 给你的最终建议

如果你现在是学习阶段，我建议你按下面顺序走：

1. 先用 Java 跑通一个最简单的 AI 调用
2. 再用 `pgvector` 做最小向量检索
3. 再把“检索 + 大模型生成”串起来
4. 最后再优化命中率和工程结构

你暂时不用急着自己写一个真正的向量数据库。

先把 `RAG` 跑通，比什么都重要。

---

## 21. 推荐你下一步就做什么

你接下来最适合的练习任务是：

### 任务一

写一个 `POST /knowledge/import` 接口，把一篇文档拆分后存入数据库。

### 任务二

写一个 `POST /chat/ask` 接口，完成：

- 问题转向量
- 相似度搜索
- 阈值判断
- AI 生成答案

### 任务三

给回答增加字段：

- `source`
- `score`
- `matchedChunks`

这样你就能直观看到系统到底是不是“真的检索到了”。

---

## 22. 结尾

如果你把这套思路学会了，后面你不管换成：

- `Qdrant`
- `Milvus`
- `Elasticsearch`
- `Spring AI`
- `LangChain4j`

底层思路都不会变。

真正重要的不是你背了多少框架名字，而是你理解了这条主线：

```text
知识入库 -> 向量化 -> 相似度检索 -> 阈值判断 -> 基于上下文回答 -> 兜底策略
```

这条主线一旦打通，你就已经真正入门了。
