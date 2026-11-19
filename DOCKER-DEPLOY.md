# B2C 商城 —— Docker 容器化部署说明

> 学号：2505273112
> 目标：在 CentOS 7 虚拟机中，用 Docker 运行 MySQL / Redis / Nacos / Nginx 及两个业务服务。

---

## 一、镜像命名规则

按 `学号 + 模块名 + service : 1.0` 命名：

| 模块 | 镜像名 |
|------|--------|
| gateway | `2505273112gatewayservice:1.0` |
| shop    | `2505273112shopservice:1.0` |

---

## 二、文件清单

```
b2c_mall/
├── docker-compose.yml          # 整站编排（mysql/redis/nacos/nginx/gateway/shop）
├── .dockerignore
├── nginx/
│   └── nginx.conf              # Nginx 反向代理到网关
├── gateway/
│   └── Dockerfile              # 多阶段构建网关镜像
├── shop/
│   └── Dockerfile              # 多阶段构建业务镜像（内置 shop.db）
└── deploy/
    ├── setup-centos7.sh        # 虚拟机内：修源 + 装 Docker/Compose
    ├── build-images.sh         # 虚拟机内：构建两个业务镜像
    └── deploy.sh               # 虚拟机内：一键构建 + 启动全部容器
```

---

## 三、完整部署步骤

### 步骤 0：已完成的宿主机准备工作

虚拟机已创建在 `F:\虚拟机\2505273112-b2cmall\`：

- 硬盘：`2505273112-b2cmall.vmdk`（20GB）
- 配置：`2505273112-b2cmall.vmx`（2 核 / 4GB / NAT 网卡）
- 光驱已挂载：`F:\镜像文件\CentOS-7-x86_64-DVD-2009.iso`

### 步骤 1：安装 CentOS 7 系统

1. 用 VMware Workstation 打开 `F:\虚拟机\2505273112-b2cmall\2505273112-b2cmall.vmx`
2. 开机，选择 `Install CentOS 7`，按提示完成安装（语言/分区/设置 root 密码）
3. 安装完成后 `reboot` 进入系统，确认能联网：`ping www.baidu.com`

### 步骤 2：把项目代码拷进虚拟机

任选一种方式，把整个 `F:\ssd\b2c_mall` 目录拷到虚拟机的 `/opt/b2c_mall`：

- **方式 A（推荐，共享目录）**：VM 设置 → Options → Shared Folders 开启，把 `F:\ssd\b2c_mall` 共享后在虚拟机里 `cp -r /mnt/hgfs/b2c_mall /opt/`
- **方式 B（scp）**：在虚拟机执行 `mkdir -p /opt/b2c_mall`，在宿主机用 scp/winscp 上传
- **方式 C（git）**：`git clone <仓库地址> /opt/b2c_mall`

### 步骤 3：安装 Docker 环境

```bash
cd /opt/b2c_mall/deploy
chmod +x *.sh
./setup-centos7.sh
```

该脚本会：关闭防火墙/SELinux → 修复 CentOS7 EOL 源（阿里云 vault）→ 安装 Docker CE → 配置镜像加速 → 安装 Compose 插件 → 跑 hello-world 验证。

### 步骤 4：一键部署

```bash
cd /opt/b2c_mall
./deploy/deploy.sh
```

脚本自动完成：构建两个业务镜像 → `docker compose up -d` → 等待 40 秒 → 打印状态和访问地址。

### 步骤 5：验证

```bash
docker ps                    # 应看到 6 个容器运行中
curl http://localhost:8081   # 网关
curl http://localhost:80     # Nginx
docker logs -f b2c-shop      # 查看业务日志
```

Nacos 控制台 `http://<虚拟机IP>:8848/nacos`（默认账号密码 nacos/nacos）。

---

## 四、常见问题

> **部署状态（已验证通过）**：当前 6 个容器已全部正常运行，端到端验证通过（`curl http://localhost/front/categories` 与 `http://localhost:8081/front/categories` 均返回 `code:200` 及真实数据）。以下是实际排障中遇到并已解决的问题：

1. **`docker ps` 为空 / `docker: 'compose' is not a docker command`**：原因是 Compose 插件文件从 GitHub 下载不完整（仅 7.5MB，正常 60MB+），执行时报 `segmentation fault`。**修复**：删除损坏文件后用 yum 重装（已配置的阿里云 docker-ce 源有该包，6 秒装好）：
   ```bash
   rm -f /usr/local/lib/docker/cli-plugins/docker-compose
   yum install -y docker-compose-plugin
   docker compose version    # 应显示 v2.27.1
   ```
   > 结论：CentOS7 上装 Compose 插件优先用 `yum install -y docker-compose-plugin`，不要用 curl 从 GitHub 下载。

2. **Maven 构建卡在 `dependency:go-offline` 数百秒**：Dockerfile 的 Maven 直连中央仓库很慢。**已修复**：两个 Dockerfile 的 builder 阶段都会写入 `/root/.m2/settings.xml`，把 central 指向 `https://maven.aliyun.com/repository/public`，改后依赖下载约 60 秒完成。

3. **Nginx 反复重启，`"upstream" directive is not allowed here`**：`upstream` 不能写在 `server { }` 内部。**已修复**：`nginx/nginx.conf` 中 `upstream` 已移到与 `server` 平级（该文件挂载到 `/etc/nginx/conf.d/default.conf`，位于主配置 http 块内，故不能再包一层 `http`）。改后执行 `docker compose up -d --force-recreate nginx`。

4. **Maven 构建拉不到依赖**：`setup-centos7.sh` 已配镜像加速；Dockerfile 里用 `dependency:go-offline || true` 容错。若仍失败，可在有网环境先 `mvn package` 打包，改用「只 COPY jar」的简化 Dockerfile。
2. **Nacos 起不来**：确认内存 ≥4GB（VM 已配 4GB）；首次启动约需 1-2 分钟，healthcheck 设了 15 次重试。
3. **Shop 连不上数据库**：镜像内已内置 `shop.db` 到 `/data/shop.db`，并通过启动参数覆盖 JDBC URL，不依赖宿主机路径。
4. **namespace 不一致**：本地 Nacos 使用 public（空 namespace ID），已通过 `NACOS_NAMESPACE: ""` 覆盖；原代码默认的 `zhuduole` 用于连接外部 Nacos（47.100.22.158）。
5. **端口冲突**：如宿主机 80/3306/6379/8848/8081/8082 被占用，改 `docker-compose.yml` 里 `ports` 左侧端口。

---

## 五、中间件说明

| 服务 | 镜像 | 端口 | 说明 |
|------|------|------|------|
| MySQL | mysql:5.7 | 3306 | 按任务要求部署（本项目的业务数据当前仍存 SQLite） |
| Redis | redis:7-alpine | 6379 | 应用登录态/会话依赖，必须启动 |
| Nacos | v1.4.7 | 8848 | 注册中心，单机模式 |
| Nginx | 1.25-alpine | 80 | 反向代理到 gateway:8081 |
| gateway | 自建 | 8081 | 网关服务 |
| shop | 自建 | 8082 | 业务服务 |
