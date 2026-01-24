# ==================== 阶段1: 前端构建 ====================
FROM node:20-alpine AS frontend-builder

WORKDIR /app/frontend

# 复制前端依赖文件
COPY SmartNetdiskFrontend/package*.json ./

# 安装依赖（使用淘宝镜像加速）
RUN npm ci --registry=https://registry.npmmirror.com

# 复制前端源码
COPY SmartNetdiskFrontend/ ./

# 构建前端
RUN npm run build

# ==================== 阶段2: 后端构建 ====================
FROM maven:3.9-eclipse-temurin-21-alpine AS backend-builder

WORKDIR /app/backend

# 复制 Maven 配置和依赖文件
COPY pom.xml ./
COPY .mvn ./.mvn
COPY mvnw ./

# 下载依赖（利用 Docker 缓存）
RUN mvn dependency:go-offline -B

# 复制后端源码
COPY src ./src

# 构建后端（跳过测试）
RUN mvn clean package -DskipTests -B

# ==================== 阶段3: 运行时镜像 ====================
FROM eclipse-temurin:21-jre-alpine

LABEL maintainer="SmartNetdisk"
LABEL description="SmartNetdisk Backend Application"

WORKDIR /app

# 安装 Nginx 用于托管前端
RUN apk add --no-cache nginx

# 复制后端 JAR 文件
COPY --from=backend-builder /app/backend/target/SmartNetdisk-0.0.1-SNAPSHOT.jar ./app.jar

# 复制前端构建产物到 Nginx 目录
COPY --from=frontend-builder /app/frontend/dist /usr/share/nginx/html

# 复制 Nginx 配置
COPY nginx.conf /etc/nginx/nginx.conf

# 复制启动脚本
COPY docker-entrypoint.sh /docker-entrypoint.sh
RUN chmod +x /docker-entrypoint.sh

# 暴露端口：8081(后端) 80(前端)
EXPOSE 8081 80

# 启动脚本
ENTRYPOINT ["/docker-entrypoint.sh"]
