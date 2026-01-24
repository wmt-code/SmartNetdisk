pipeline {
    agent any

    environment {
        // 项目基本信息
        PROJECT_NAME = 'smartnetdisk'
        IMAGE_NAME = 'smartnetdisk'
        IMAGE_TAG = "${BUILD_NUMBER}"
        
        // 部署配置
        DEPLOY_PATH = '/opt/smartnetdisk'
    }

    options {
        // 构建超时时间
        timeout(time: 30, unit: 'MINUTES')
        // 保留最近 10 次构建
        buildDiscarder(logRotator(numToKeepStr: '10'))
        // 禁止并发构建
        disableConcurrentBuilds()
        // 显示时间戳
        timestamps()
    }

    stages {
        stage('检出代码') {
            steps {
                echo '📥 正在检出代码...'
                checkout scm
            }
        }

        stage('构建 Docker 镜像') {
            steps {
                echo '🐳 正在构建 Docker 镜像...'
                sh """
                    docker build -t ${IMAGE_NAME}:${IMAGE_TAG} .
                    docker tag ${IMAGE_NAME}:${IMAGE_TAG} ${IMAGE_NAME}:latest
                """
            }
        }

        stage('停止旧容器') {
            steps {
                echo '🛑 停止并删除旧容器...'
                sh '''
                    docker stop smartnetdisk-app || true
                    docker rm smartnetdisk-app || true
                '''
            }
        }

        stage('部署新容器') {
            steps {
                echo '🚀 部署新容器...'
                sh """
                    # 创建部署目录
                    mkdir -p ${DEPLOY_PATH}
                    
                    # 复制 docker-compose.yml
                    cp docker-compose.yml ${DEPLOY_PATH}/
                    
                    # 进入部署目录并启动
                    cd ${DEPLOY_PATH}
                    docker-compose up -d
                """
            }
        }

        stage('健康检查') {
            steps {
                echo '🏥 等待服务启动...'
                sh '''
                    # 等待容器启动
                    sleep 30
                    
                    # 检查容器是否运行
                    if docker ps | grep -q smartnetdisk-app; then
                        echo "✅ 容器运行正常!"
                        
                        # 检查后端服务
                        if curl -s http://localhost:8081/actuator/health > /dev/null 2>&1; then
                            echo "✅ 后端服务健康!"
                        else
                            echo "⚠️ 后端服务可能还在启动中..."
                        fi
                        
                        # 检查前端服务
                        if curl -s http://localhost:9000 > /dev/null 2>&1; then
                            echo "✅ 前端服务健康!"
                        else
                            echo "⚠️ 前端服务可能还在启动中..."
                        fi
                    else
                        echo "❌ 容器未运行!"
                        docker logs smartnetdisk-app
                        exit 1
                    fi
                '''
            }
        }

        stage('清理旧镜像') {
            steps {
                echo '🧹 清理旧镜像...'
                sh '''
                    # 删除悬空镜像
                    docker image prune -f || true
                    
                    # 保留最近 3 个版本的镜像
                    docker images ${IMAGE_NAME} --format "{{.Tag}}" | \
                        grep -v latest | \
                        sort -rn | \
                        tail -n +4 | \
                        xargs -I {} docker rmi ${IMAGE_NAME}:{} || true
                '''
            }
        }
    }

    post {
        success {
            echo '''
            ✅ ==========================================
            ✅ 部署成功!
            ✅ 前端地址: http://your-server:9000
            ✅ 后端地址: http://your-server:8081
            ✅ ==========================================
            '''
        }
        failure {
            echo '❌ 构建或部署失败!'
            sh '''
                echo "查看容器日志..."
                docker logs smartnetdisk-app || true
            '''
        }
        always {
            echo '🧹 清理工作空间...'
            cleanWs()
        }
    }
}
