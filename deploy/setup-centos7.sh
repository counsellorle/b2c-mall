#!/bin/bash
# ============================================================
# 在 CentOS 7 虚拟机内执行：修复软件源 + 安装 Docker + Docker Compose
# 用法：chmod +x setup-centos7.sh && ./setup-centos7.sh
# ============================================================
set -e

echo "===== 1. 关闭防火墙 / SELinux（实验环境） ====="
systemctl stop firewalld 2>/dev/null || true
systemctl disable firewalld 2>/dev/null || true
setenforce 0 2>/dev/null || true
sed -i 's/^SELINUX=enforcing/SELINUX=disabled/' /etc/selinux/config 2>/dev/null || true

echo "===== 2. 修复 CentOS7 已停止维护的 yum 源（指向阿里云 vault） ====="
mkdir -p /etc/yum.repos.d/bak
mv /etc/yum.repos.d/CentOS-*.repo /etc/yum.repos.d/bak/ 2>/dev/null || true
cat > /etc/yum.repos.d/CentOS7-vault.repo <<'EOF'
[base]
name=CentOS-7 - Base - vault
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/os/x86_64/
gpgcheck=0
enabled=1

[updates]
name=CentOS-7 - Updates - vault
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/updates/x86_64/
gpgcheck=0
enabled=1

[extras]
name=CentOS-7 - Extras - vault
baseurl=https://mirrors.aliyun.com/centos-vault/7.9.2009/extras/x86_64/
gpgcheck=0
enabled=1
EOF
yum clean all
yum makecache fast

echo "===== 3. 安装依赖 ====="
yum install -y yum-utils device-mapper-persistent-data lvm2 curl wget vim

echo "===== 4. 添加 Docker 源（阿里云）并安装 ====="
yum-config-manager --add-repo https://mirrors.aliyun.com/docker-ce/linux/centos/docker-ce.repo
yum install -y docker-ce docker-ce-cli containerd.io

echo "===== 5. 配置镜像加速 ====="
mkdir -p /etc/docker
cat > /etc/docker/daemon.json <<'EOF'
{
  "registry-mirrors": [
    "https://docker.m.daocloud.io",
    "https://docker.1ms.run",
    "https://mirror.ccs.tencentyun.com"
  ],
  "log-opts": { "max-size": "100m", "max-file": "3" }
}
EOF

echo "===== 6. 启动 Docker ====="
systemctl daemon-reload
systemctl enable docker
systemctl start docker

echo "===== 7. 安装 Docker Compose 插件 ====="
mkdir -p /usr/local/lib/docker/cli-plugins
curl -SL https://github.com/docker/compose/releases/download/v2.27.0/docker-compose-linux-x86_64 \
  -o /usr/local/lib/docker/cli-plugins/docker-compose
chmod +x /usr/local/lib/docker/cli-plugins/docker-compose

echo "===== 8. 验证 ====="
docker --version
docker compose version
docker run --rm hello-world

echo ""
echo "Docker 环境安装完成！接下来把项目代码拷入虚拟机，执行 deploy/deploy.sh"
