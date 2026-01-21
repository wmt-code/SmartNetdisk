# SmartNetdisk - 智能网盘

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.0.1-brightgreen" alt="Spring Boot">
  <img src="https://img.shields.io/badge/JDK-21-orange" alt="JDK">
  <img src="https://img.shields.io/badge/PostgreSQL-16.x-blue" alt="PostgreSQL">
  <img src="https://img.shields.io/badge/Redis-7.x-red" alt="Redis">
  <img src="https://img.shields.io/badge/MinIO-Latest-yellow" alt="MinIO">
  <img src="https://img.shields.io/badge/MyBatis--Plus-3.5.15-blueviolet" alt="MyBatis-Plus">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="License">
</p>

> 🚀 私有化、高性能、智能化的个人云存储系统，深度集成 AI 能力

---

## ✨ 核心特性

- 🔐 **私有化部署** - 数据完全自主可控，支持个人或企业私有部署
- ⚡ **高性能传输** - 分片上传、断点续传、极速秒传，大文件上传无压力
- 🔍 **智能语义搜索** - 基于向量数据库的语义检索，告别传统关键词搜索
- 🤖 **AI 智能问答 (RAG)** - 对文档内容进行智能问答，快速获取关键信息
- 📄 **文档智能摘要** - 自动生成文档摘要，快速了解文档内容
- 🌐 **多端访问** - 支持 Web、移动端多平台访问

---

## 🛠️ 技术栈

| 层级 | 技术 | 版本 | 说明 |
|------|------|------|------|
| **前端** | Vue 3 + TypeScript | 3.x | SPA 单页应用 |
| | Vite | 5.x | 构建工具 |
| | Element Plus | 2.x | UI 组件库 |
| **后端** | Spring Boot | 4.x | 核心框架 |
| | JDK | 21 | LTS 版本 |
| | MyBatis-Plus | 3.5.x | ORM 框架 |
| **数据库** | PostgreSQL | 16.x | 主数据库 |
| | pgvector | 0.7.x | 向量存储扩展 |
| | Redis | 7.x | 缓存/会话 |
| **存储** | MinIO | Latest | 对象存储 |
| **AI** | 硅基流动 Qwen2.5 | - | LLM 大模型 |

---

## 📦 项目结构

```
SmartNetdisk
├── src/
│   └── main/
│       ├── java/com/wmt/smartnetdisk/
│       │   ├── config/          # 配置类
│       │   ├── controller/      # 控制器层
│       │   ├── service/         # 业务逻辑层
│       │   ├── mapper/          # MyBatis Mapper
│       │   ├── entity/          # 数据库实体
│       │   ├── dto/             # 数据传输对象
│       │   ├── vo/              # 视图对象
│       │   ├── common/          # 通用类
│       │   └── utils/           # 工具类
│       └── resources/
│           └── application.yml  # 配置文件
├── SmartNetdiskFrontend/        # 前端项目
├── pom.xml                      # Maven 配置
└── README.md
```

---

## 🚀 快速开始

### 环境要求

- JDK 21+
- Maven 3.9+
- PostgreSQL 16.x (启用 pgvector 扩展)
- Redis 7.x
- MinIO

### 1. 克隆项目

```bash
git clone https://github.com/your-repo/SmartNetdisk.git
cd SmartNetdisk
```

### 2. 配置数据库

```sql
-- 创建数据库
CREATE DATABASE smartnetdisk;

-- 启用向量扩展
CREATE EXTENSION IF NOT EXISTS vector;
```

### 3. 配置环境变量

```bash
# 数据库配置
export DB_PASSWORD=your_postgres_password

# Redis 配置
export REDIS_PASSWORD=your_redis_password
export REDIS_HOST=localhost

# MinIO 配置
export MINIO_ENDPOINT=http://localhost:9000
export MINIO_ACCESS_KEY=minioadmin
export MINIO_SECRET_KEY=minioadmin

# 硅基流动 AI API Key
export SILICONFLOW_API_KEY=your_api_key
```

### 4. 启动后端服务

```bash
# 使用 Maven 启动
mvn spring-boot:run

# 或者打包后运行
mvn clean package -DskipTests
java -jar target/SmartNetdisk-0.0.1-SNAPSHOT.jar
```

### 5. 启动前端服务

```bash
cd SmartNetdiskFrontend
npm install
npm run dev
```

### 6. 访问应用

- 后端 API: http://localhost:8081/api
- 前端页面: http://localhost:5173

---

## 📚 API 概览

| 模块 | 接口示例 | 描述 |
|------|----------|------|
| 认证 | `POST /api/auth/login` | 用户登录 |
| 用户 | `GET /api/user/info` | 获取用户信息 |
| 文件 | `POST /api/file/upload` | 文件上传 |
| 文件 | `POST /api/file/chunk` | 分片上传 |
| 分享 | `POST /api/share` | 创建分享链接 |
| AI | `POST /api/ai/search` | 语义搜索 |
| AI | `POST /api/ai/chat` | 智能问答 |

详细 API 文档请参考 [implementation_plan.md](./implementation_plan.md)

---

## 🤖 AI 功能说明

### 语义搜索
基于 pgvector 向量数据库，对上传的文档进行向量化处理，支持基于语义相似度的智能搜索。

### 智能问答 (RAG)
利用 RAG (Retrieval-Augmented Generation) 技术，对文档内容进行智能问答，快速从海量文档中获取所需信息。

### 支持的文档类型
- PDF
- Word (.doc, .docx)
- 纯文本 (.txt, .md)
- 更多格式持续扩展中...

---

## 📄 许可证

本项目采用 [MIT License](LICENSE) 开源协议。

---

## 🤝 贡献指南

欢迎提交 Issue 和 Pull Request！

1. Fork 本仓库
2. 创建你的特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交你的更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 开启一个 Pull Request

---

## 📧 联系方式

如有问题或建议，请通过以下方式联系：

- 提交 [Issue](https://github.com/your-repo/SmartNetdisk/issues)
- 发送邮件至: your-email@example.com

---

<p align="center">
  Made with ❤️ by SmartNetdisk Team
</p>
