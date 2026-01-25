# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

SmartNetdisk is a private cloud storage system with AI-powered semantic search and RAG capabilities. It consists of a Spring Boot 4.x backend (JDK 21) and Vue 3 + TypeScript frontend.

## Build & Development Commands

### Backend (root directory)
```bash
mvn spring-boot:run              # Development with hot reload
mvn clean package -DskipTests    # Build JAR
java -jar target/SmartNetdisk-0.0.1-SNAPSHOT.jar  # Run packaged JAR
```
Backend runs on http://localhost:8081

### Frontend (SmartNetdiskFrontend/)
```bash
npm install                      # Install dependencies
npm run dev                      # Vite dev server (http://localhost:5173)
npm run build                    # Production build (outputs to dist/)
npm run type-check               # TypeScript validation (vue-tsc)
npm run lint                     # ESLint with auto-fix
npm run format                   # Prettier formatting
```

## Architecture

### Backend Structure (`src/main/java/com/wmt/smartnetdisk/`)
- **controller/** - REST endpoints (AuthController, FileController, AiController, ShareController, FolderController, PublicShareController)
- **service/impl/** - Business logic implementations
- **entity/** - JPA entities: FileInfo, FileChunk, User, Folder, Share, ShareItem, VectorDocument
- **dto/request/** - Input validation DTOs (LoginDTO, ChunkUploadDTO, SearchDTO, ChatDTO, etc.)
- **vo/** - Response objects (LoginVO, FileVO, UploadResultVO, ChatResultVO, etc.)
- **mapper/** - MyBatis-Plus data access interfaces
- **config/** - MinioConfig, SaTokenConfig, RedisConfig, AiConfig, CorsConfig
- **common/** - Result wrapper, GlobalExceptionHandler, constants, enums

### Frontend Structure (`SmartNetdiskFrontend/src/`)
- **views/** - Page components (LoginView, FileMain, MySharesView, ShareView)
- **layout/** - BasicLayout with sidebar/header
- **components/** - Reusable UI (AiSidebar, ShareDialog)
- **api/** - HTTP layer: auth.ts, file.ts, ai.ts, share.ts
- **stores/** - Pinia stores (user.ts)
- **router/** - Vue Router with auth guards
- **utils/api.ts** - Axios instance with token interceptor

## Key Technical Details

### Authentication
- Uses Sa-Token (not Spring Security) with Redis session storage
- Token name: `satoken`, stored in request headers
- 7-day session validity

### File Upload Flow
1. Frontend calculates MD5 → calls `/file/check` for fast upload (秒传) detection
2. If file exists by MD5: instant completion
3. Otherwise: chunked upload via `/file/chunk` → merge via `/file/merge`
4. Supports 10GB+ files (configured in application.yml)

### AI Features
- Vectorization: `/ai/vectorize/{fileId}` → extracts text, chunks, embeds to pgvector
- Semantic search: `/ai/search` → query embedding → k-NN in pgvector
- RAG chat: `/ai/chat` → retrieves context chunks → LLM generates answer
- Uses Silicon Flow API (Qwen2.5-7B) for embeddings and chat

### API Response Format
All endpoints return `Result<T>` wrapper:
```json
{ "code": 200, "message": "success", "data": {...} }
```

### Soft Deletes
Files use manual `deleted` flag (not @TableLogic) to support recycle bin restoration.

## Environment Variables

Backend (`application.yml`):
- `DB_PASSWORD` - PostgreSQL password
- `REDIS_PASSWORD`, `REDIS_HOST` - Redis config
- `MINIO_ENDPOINT`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY` - Object storage
- `SILICONFLOW_API_KEY` - AI API key

## Database Requirements

- PostgreSQL 16.x with pgvector extension enabled
- Redis 7.x for session storage
- MinIO for object storage
