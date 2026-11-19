#!/bin/bash
# ============================================================
# 构建各模块镜像（在 CentOS7 虚拟机内运行）
# 镜像命名规则：学号xxservice:1.0  学号=2505273112
# ============================================================
set -e

cd "$(dirname "$0")/.."   # 回到项目根目录，作为 docker build 上下文

echo "===== [1/2] 构建 gateway 镜像 ====="
docker build -f gateway/Dockerfile -t 2505273112gatewayservice:1.0 .

echo "===== [2/2] 构建 shop 镜像 ====="
docker build -f shop/Dockerfile -t 2505273112shopservice:1.0 .

echo "===== 构建完成，当前镜像列表 ====="
docker images | grep -E '2505273112|REPOSITORY'
