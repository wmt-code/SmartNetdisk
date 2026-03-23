# SmartNetdisk - AI 智能云存储系统

<p align="center">
  <b>一个集成 AI 语义搜索和 RAG 问答能力的私有云存储系统</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen" alt="Spring Boot" />
  <img src="https://img.shields.io/badge/Vue-3.x-blue" alt="Vue 3" />
  <img src="https://img.shields.io/badge/JDK-21-orange" alt="JDK 21" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-blue" alt="PostgreSQL" />
  <img src="https://img.shields.io/badge/pgvector-向量搜索-purple" alt="pgvector" />
</p>

---

## 项目简介

SmartNetdisk 是一个功能完整的私有云存储系统，除了提供文件上传、下载、分享等基础网盘功能外，还深度集成了 AI 能力：

- **语义搜索**：通过自然语言描述查找文件，而非仅靠文件名匹配
- **RAG 智能问答**：基于已上传的文档内容回答问题，支持多文档知识库
- **AI 文件摘要**：一键生成文件摘要，鼠标悬停即可查看
- **智能向量化**：上传后自动对小文件进行向量化处理

## 技术栈

### 后端
| 技术 | 说明 |
|------|------|
| Spring Boot 4.x | 核心框架 |
| JDK 21 | Java 运行时 |
| MyBatis-Plus | ORM 框架 |
| Sa-Token | 认证鉴权（支持角色权限） |
| PostgreSQL 16 | 主数据库 |
| pgvector | 向量相似度搜索扩展 |
| Redis 7 | Session 存储 + 缓存 |
| MinIO | 对象存储（文件存储） |
| Apache POI | Word 文档文本提取 |
| Apache PDFBox | PDF 文本提取 |
| kkFileView | Office/文档在线预览 |

### 前端
| 技术 | 说明 |
|------|------|
| Vue 3 + TypeScript | 前端框架 |
| Vite | 构建工具 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| ECharts | 数据可视化（管理后台） |
| Uppy | 文件上传（分片/秒传/断点续传） |
| Monaco Editor | 代码在线编辑 |
| marked | Markdown 渲染 |

### AI 能力
| 能力 | 说明 |
|------|------|
| 对话/摘要/RAG | 支持阿里云 DashScope、火山引擎、硅基流动等 OpenAI 兼容 API |
| 文本向量化 | 支持多种 Embedding 模型 |
| 向量存储 | PostgreSQL pgvector（余弦相似度搜索） |

## 功能特性

### 文件管理
- 文件上传（拖拽上传、文件夹上传、分片上传、秒传、断点续传）
- 文件下载（单文件下载、批量 ZIP 打包下载）
- 文件预览（图片缩放旋转/视频音频/PDF/Office/代码/Markdown）
- 在线编辑（44 种文本/代码文件格式，Monaco Editor）
- 文件夹管理（创建、重命名、移动、复制、递归删除）
- 回收站（软删除 + 恢复 + 彻底删除 + 清空）
- 文件搜索（文件名 + AI 摘要联合搜索）
- 列表/网格双视图切换
- 右键自定义菜单

### AI 能力
- **全局问答**：基于全部已向量化文件的 RAG 对话
- **多文档知识库**：选择特定文件创建知识库，限定范围对话
- **AI 文件摘要**：一键生成摘要，列表悬停 tooltip 展示
- **智能分析（向量化）**：文本提取 + 分块 + Embedding + pgvector 存储
- **上传自动向量化**：≤50KB 的支持文件自动后台向量化
- **对话历史持久化**：会话保存到 PostgreSQL，刷新不丢失
- **Markdown 渲染**：AI 回答支持完整 Markdown 格式展示
- **多模型适配**：支持阿里云、火山引擎、硅基流动等多个 AI 服务商

### 分享功能
- 单文件/文件夹/批量分享
- 提取码保护
- 有效期设置（1天/7天/30天/永久）
- 分享页在线预览（图片/视频/音频/PDF/Office）
- 显示分享者头像和用户名

### 用户系统
- 注册/登录（Sa-Token + Redis Session）
- 个人中心（头像上传、用户名修改、密码修改）
- 存储空间管理（仪表盘式展示）
- 系统设置（主题切换 浅色/暗色/跟随系统、偏好配置）
- 通知系统（上传/分享/AI 完成通知，30s 轮询，未读徽标）

### 管理后台（仅管理员可见）
- **数据概览**：ECharts 图表（用户数、文件数、存储量、上传趋势、文件类型分布、用户存储排行）
- **用户管理**：搜索、启用/禁用、调整存储配额、设置角色、删除用户
- **文件管理**：全局文件搜索（跨用户）、按用户/类型/日期多维度筛选、管理员删除

## 项目结构

```
SmartNetdisk/
├── src/main/java/com/wmt/smartnetdisk/
│   ├── config/            # 配置类（Sa-Token、MinIO、AI、CORS、kkFileView）
│   ├── controller/        # REST 控制器
│   │   ├── AuthController         # 认证（登录/注册）
│   │   ├── FileController         # 文件操作
│   │   ├── FolderController       # 文件夹操作
│   │   ├── AiController           # AI（向量化/搜索/问答/摘要）
│   │   ├── ShareController        # 分享管理
│   │   ├── PublicShareController   # 公开分享访问（无需登录）
│   │   ├── AdminController        # 管理员后台
│   │   ├── NotificationController # 通知系统
│   │   ├── ChatSessionController  # AI 会话持久化
│   │   └── UserController         # 用户管理
│   ├── service/impl/      # 业务逻辑实现
│   ├── entity/            # 数据库实体
│   ├── mapper/            # MyBatis-Plus Mapper
│   ├── dto/request/       # 请求 DTO
│   ├── vo/                # 响应 VO
│   ├── common/            # 公共组件（Result 封装、全局异常处理）
│   └── utils/             # 工具类（MinioUtils）
│
├── SmartNetdiskFrontend/src/
│   ├── api/               # API 接口层（auth/file/ai/share/admin/notification）
│   ├── components/        # 组件
│   │   ├── AiSidebar.vue          # AI 助手侧边栏
│   │   ├── FilePreviewDialog.vue  # 文件预览（图片/视频/PDF/代码）
│   │   ├── FileEditorDialog.vue   # Monaco 代码编辑器
│   │   ├── NotificationDropdown.vue # 通知下拉
│   │   └── file/                  # 文件子组件（工具栏/网格/右键菜单）
│   ├── composables/       # Vue Composables（useFileList/useFileSelection 等）
│   ├── constants/         # 常量（文件类型 → 图标/颜色映射）
│   ├── layout/            # 布局（侧边栏 + Header）
│   ├── stores/            # Pinia 状态管理
│   ├── styles/            # 设计系统（CSS Variables 主题系统）
│   └── views/             # 页面
│       ├── file/FileMain.vue      # 文件管理
│       ├── admin/AdminDashboard.vue # 管理后台
│       ├── ProfileView.vue        # 个人中心
│       ├── SettingsView.vue       # 系统设置
│       └── ShareView.vue          # 分享访问页
│
├── docker-compose.local.yml    # 本地 Docker 开发环境
├── docker/init-db/             # 数据库初始化 SQL
└── start-local.sh              # 一键启动脚本
```

## 快速开始

### 环境要求

- JDK 21+
- Node.js 18+
- Docker Desktop
- Maven 3.9+

### 1. 启动基础服务

```bash
# 一键启动 PostgreSQL(pgvector) + Redis + MinIO + kkFileView
bash start-local.sh up
```

| 服务 | 地址 | 账号/密码 |
|------|------|----------|
| PostgreSQL (pgvector) | localhost:5433 | postgres / postgres123 |
| Redis | localhost:6381 | redis123 |
| MinIO API | localhost:9010 | minioadmin / minioadmin123 |
| MinIO 控制台 | localhost:9011 | minioadmin / minioadmin123 |
| kkFileView | localhost:8012 | - |

### 2. 启动后端

```bash
mvn clean spring-boot:run -Dspring-boot.run.profiles=local
```

后端运行在 http://localhost:8081

### 3. 启动前端

```bash
cd SmartNetdiskFrontend
npm install
npm run dev
```

前端运行在 http://localhost:5173

### 4. 访问系统

打开浏览器访问 http://localhost:5173，注册账号即可使用。

> 首个注册用户名为 `admin` 的账号将自动设为管理员。

## 环境变量

后端通过环境变量配置敏感信息（`application.yml` 中使用 `${VAR:default}` 格式）：

```bash
# 数据库
DB_HOST=localhost
DB_PORT=5432
DB_PASSWORD=your_password

# Redis
REDIS_HOST=localhost
REDIS_PASSWORD=your_password

# MinIO
MINIO_ENDPOINT=http://localhost:9000
MINIO_ACCESS_KEY=your_access_key
MINIO_SECRET_KEY=your_secret_key

# AI（支持任意 OpenAI 兼容 API）
AI_API_KEY=your_api_key
AI_BASE_URL=https://dashscope.aliyuncs.com/compatible-mode/v1
AI_MODEL=qwen-plus

# Embedding（可选，默认使用与 AI 相同的 provider）
EMBEDDING_MODEL=text-embedding-v3
EMBEDDING_BASE_URL=  # 留空则使用 AI_BASE_URL
EMBEDDING_API_KEY=   # 留空则使用 AI_API_KEY
```

## 数据库

Docker 初始化脚本自动创建完整表结构。增量迁移脚本位于 `src/main/resources/db/migration/`：

| 版本 | 说明 |
|------|------|
| schema.sql | 初始化（用户、文件、文件夹、分片、分享、向量文档） |
| V2 | 批量分享功能 |
| V3 | 文件夹删除时间 |
| V4 | 文件最近访问时间 |
| V5 | 通知系统表 |
| V6 | AI 摘要字段 |
| V7 | AI 会话持久化表 |
| V8 | 用户角色字段 |

## API 格式

所有接口统一返回：

```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

认证方式：请求头 `satoken: <token_value>`

## 开源协议

MIT License

## 致谢

- [Spring Boot](https://spring.io/projects/spring-boot) | [Vue.js](https://vuejs.org/) | [Element Plus](https://element-plus.org/)
- [Sa-Token](https://sa-token.cc/) | [MyBatis-Plus](https://baomidou.com/) | [pgvector](https://github.com/pgvector/pgvector)
- [MinIO](https://min.io/) | [kkFileView](https://kkfileview.keking.cn/)
- [Apache POI](https://poi.apache.org/) | [Apache PDFBox](https://pdfbox.apache.org/)
- [ECharts](https://echarts.apache.org/) | [Uppy](https://uppy.io/) | [Monaco Editor](https://microsoft.github.io/monaco-editor/)
