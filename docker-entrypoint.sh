#!/bin/sh

# 启动 Nginx（托管前端）
nginx

# 启动 Spring Boot 应用（前台运行）
exec java -jar /app/app.jar \
    -Xms512m \
    -Xmx1024m \
    --spring.profiles.active=prod
