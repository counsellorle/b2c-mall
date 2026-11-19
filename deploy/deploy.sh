#!/bin/bash
# ============================================================
# 一键部署：构建镜像 + 启动整个项目（在 CentOS7 虚拟机内运行）
# ============================================================
set -e

cd "$(dirname "$0")/.."

echo "===== 1. 构建业务镜像 ====="
bash deploy/build-images.sh

echo "===== 2. 启动全部容器 ====="
docker compose up -d

echo "===== 3. 等待服务就绪（约 40 秒）====="
sleep 40

echo "===== 4. 容器运行状态 ====="
docker compose ps

echo ""
echo "部署完成！访问地址（虚拟机 IP）："
echo "  - 商城入口(Nginx) : http://<虚拟机IP>/"
echo "  - 网关(Gateway)   : http://<虚拟机IP>:8081"
echo "  - 业务(Shop)      : http://<虚拟机IP>:8082"
echo "  - Nacos 控制台    : http://<虚拟机IP>:8848/nacos  (账号/密码: nacos/nacos)"
echo "  - MySQL           : <虚拟机IP>:3306  (root/密码见 .env 的 MYSQL_ROOT_PASSWORD)"
echo "  - Redis           : <虚拟机IP>:6379"
echo ""
echo "提示：合规密码、地址等敏感值均从 .env 读取，参见 .env.example。"
