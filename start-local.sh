#!/bin/bash
# SmartNetdisk 本地开发环境一键启动脚本
# 用法: ./start-local.sh [up|down|restart|logs|status]

COMPOSE_FILE="docker-compose.local.yml"
ACTION=${1:-up}

case "$ACTION" in
  up)
    echo ">>> 启动本地开发环境 (PostgreSQL + Redis + MinIO)..."
    docker compose -f $COMPOSE_FILE up -d
    echo ""
    echo ">>> 等待服务就绪..."
    sleep 5
    docker compose -f $COMPOSE_FILE ps
    echo ""
    echo "=== 服务地址 ==="
    echo "PostgreSQL: localhost:5433  (postgres / postgres123)"
    echo "Redis:      localhost:6381  (密码: redis123)"
    echo "MinIO API:  localhost:9010  (minioadmin / minioadmin123)"
    echo "MinIO 控制台: localhost:9011"
    echo ""
    echo ">>> 启动后端: mvn spring-boot:run -Dspring-boot.run.profiles=local"
    ;;
  down)
    echo ">>> 停止本地开发环境..."
    docker compose -f $COMPOSE_FILE down
    ;;
  restart)
    echo ">>> 重启本地开发环境..."
    docker compose -f $COMPOSE_FILE down
    docker compose -f $COMPOSE_FILE up -d
    ;;
  logs)
    docker compose -f $COMPOSE_FILE logs -f
    ;;
  status)
    docker compose -f $COMPOSE_FILE ps
    ;;
  clean)
    echo ">>> 停止并清除所有数据卷..."
    docker compose -f $COMPOSE_FILE down -v
    echo ">>> 数据已清除"
    ;;
  *)
    echo "用法: $0 [up|down|restart|logs|status|clean]"
    exit 1
    ;;
esac
