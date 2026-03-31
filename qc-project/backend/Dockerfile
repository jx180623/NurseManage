# ──────────────────────────────────────────────
# Dockerfile  —  Spring Boot 后端（多阶段构建）
# ──────────────────────────────────────────────

# Stage 1: 编译打包
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /build
COPY pom.xml .
# 先下载依赖（利用 Docker 缓存层）
RUN mvn dependency:go-offline -q
COPY src ./src
RUN mvn clean package -DskipTests -q

# Stage 2: 运行镜像（轻量）
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# 时区
RUN apk add --no-cache tzdata curl && \
    cp /usr/share/zoneinfo/Asia/Shanghai /etc/localtime && \
    echo "Asia/Shanghai" > /etc/timezone && \
    apk del tzdata

# 非 root 运行
RUN addgroup -S qcgroup && adduser -S qcuser -G qcgroup
USER qcuser

COPY --from=builder /build/target/qc-system-1.0.0.jar app.jar

EXPOSE 8080

# JVM 优化参数（适配阿里云 2 核 4G ECS）
ENTRYPOINT ["java", \
  "-Xms256m", "-Xmx512m", \
  "-XX:+UseG1GC", \
  "-XX:+UseContainerSupport", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
