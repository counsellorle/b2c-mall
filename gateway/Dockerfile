# ============================================================
# 网关服务镜像  2505273112gatewayservice:1.0
# 构建上下文必须是项目根目录：
#   docker build -f gateway/Dockerfile -t 2505273112gatewayservice:1.0 .
# ============================================================

# ---------- 阶段1：编译打包 ----------
FROM maven:3.8.8-eclipse-temurin-8 AS builder
WORKDIR /build

# 配置阿里云 Maven 镜像，避免直连中央仓库过慢
RUN mkdir -p /root/.m2 && \
    printf '<settings><mirrors><mirror><id>aliyun</id><mirrorOf>central</mirrorOf><url>https://maven.aliyun.com/repository/public</url></mirror></mirrors></settings>' > /root/.m2/settings.xml

# 先拷贝父 POM 与两个模块的 POM，利用 Docker 层缓存预下载依赖
COPY pom.xml ./
COPY gateway/pom.xml gateway/pom.xml
COPY shop/pom.xml shop/pom.xml
RUN mvn -B -q dependency:go-offline -pl gateway -am || true

# 拷贝源码并打包（gateway 及其依赖的父工程）
COPY gateway/src gateway/src
COPY shop/src shop/src
RUN mvn -B -q clean package -pl gateway -am -DskipTests

# ---------- 阶段2：运行 ----------
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app

COPY --from=builder /build/gateway/target/gateway-1.0-SNAPSHOT.jar /app/gateway.jar

EXPOSE 8081

# 运行时区设为东八区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

ENTRYPOINT ["java", "-jar", "/app/gateway.jar"]
