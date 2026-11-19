# ============================================================
# 业务服务镜像  2505273112shopservice:1.0
# 构建上下文必须是项目根目录：
#   docker build -f shop/Dockerfile -t 2505273112shopservice:1.0 .
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
RUN mvn -B -q dependency:go-offline -pl shop -am || true

# 拷贝源码并打包（shop 及其依赖的父工程）
COPY gateway/src gateway/src
COPY shop/src shop/src
RUN mvn -B -q clean package -pl shop -am -DskipTests

# ---------- 阶段2：运行 ----------
FROM eclipse-temurin:8-jre-jammy
WORKDIR /app

COPY --from=builder /build/shop/target/shop-1.0-SNAPSHOT.jar /app/shop.jar

# 把 SQLite 数据库文件放进镜像内部（避免依赖宿主机 Windows 路径）
COPY shop/shop.db /data/shop.db

EXPOSE 8082

# 运行时区设为东八区
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# JDBC URL 用环境变量覆盖为容器内路径；Nacos/Redis 地址也用环境变量覆盖，
# 这样同一个镜像既能连本地 docker 内的中间件，也能连外部服务器。
ENTRYPOINT ["java", "-jar", "/app/shop.jar", \
    "--spring.datasource.url=jdbc:sqlite:/data/shop.db?foreign_keys=on"]
