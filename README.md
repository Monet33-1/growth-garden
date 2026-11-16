# Growth Garden · 成长花园

一个记录随笔、整理情绪、陪伴成长的小花园。

后端使用 Spring Boot，前端是原生 JavaScript 单页应用；既可以在服务器上以 MySQL 运行，也可以打包成 Windows 桌面应用，使用内置 H2 数据库离线运行。

## 功能

- 花园账号登录、JWT 身份认证
- 随笔创建、查看、编辑、删除，支持 Markdown
- 随笔与情绪瓶多对多关联
- 按情绪、关键词检索旧随笔
- 成长花园等级与经验值
- 隐藏信件：达到指定随笔数量后解锁
- DeepSeek 生成标题与情绪分类
- AI 不可用时仍可保存随笔，并手动选择情绪
- Windows 桌面版：本地 H2、首次启动创建账号、API Key 设置、NSIS 安装包
- GitHub Actions 构建发布，electron-updater 检查、下载、重试和安装更新

## 技术栈

**后端**

- Java 21
- Spring Boot 3.5
- Spring Security + JWT
- MyBatis-Plus
- MySQL 8 / H2
- DeepSeek API
- Maven Wrapper

**前端**

- 原生 JavaScript ES Modules
- HTML / CSS
- Markdown 渲染

**桌面端**

- Electron
- 内置 Java 21 运行时
- H2 本地数据库
- electron-builder + NSIS
- electron-updater
- GitHub Releases

## 仓库结构

```text
.
├── .github/workflows/desktop-release.yml   # Windows 桌面版发布流程
└── lyy/                                    # Spring Boot 应用
    ├── src/main/java/                      # 后端代码
    ├── src/main/resources/static/          # 前端页面与资源
    ├── src/main/resources/db/              # 数据库初始化脚本
    ├── desktop/electron/                   # Electron 工程
    ├── scripts/                            # 数据导出脚本
    ├── README.md                           # 应用开发与部署说明
    └── pom.xml
```

## 快速开始（桌面版开发）

需要 JDK 21、Node.js 20+。

```powershell
cd lyy

# 构建 Spring Boot jar
.\mvnw.cmd -DskipTests package

# 启动桌面 profile，使用 H2
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=desktop
```

打开 <http://localhost:18080>，首次启动会创建本地账号。

桌面数据默认位于：

```text
%USERPROFILE%\.growth-garden\
├── data\growth_garden.mv.db
├── config\application-local.properties
└── logs\
```

Electron 外壳与打包步骤见
[`lyy/desktop/electron/README.md`](lyy/desktop/electron/README.md)。

## 快速开始（服务器版）

需要 JDK 21、MySQL 8。

```sql
CREATE DATABASE growth_garden
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

导入表结构和情绪种子：

```bash
mysql -u root -p growth_garden < lyy/db/schema.sql
mysql -u root -p growth_garden < lyy/db/seed-emotions.sql
```

复制并填写本地配置：

```powershell
cd lyy
copy src\main\resources\application-local.example.properties src\main\resources\application-local.properties
```

至少填写：

```properties
spring.datasource.password=YOUR_DB_PASSWORD
jwt.secret=YOUR_RANDOM_SECRET
ai.deepseek.api-key=YOUR_DEEPSEEK_API_KEY
```

启动：

```powershell
.\mvnw.cmd spring-boot:run
```

服务器部署、Docker Compose、环境变量和数据库导出说明见
[`lyy/README.md`](lyy/README.md)。

## 桌面版构建

```powershell
cd lyy

# 构建 jar + 精简 Java 运行时
.\desktop\build-runtime.ps1

cd desktop\electron
npm install
npm run dist
```

输出：

```text
lyy/desktop/electron/dist/GrowthGarden-Setup-<version>.exe
```

## 发布流程

仓库根目录的 GitHub Actions 在推送 `v*` tag 时构建 Windows 安装包：

```text
.github/workflows/desktop-release.yml
```

```powershell
git tag v1.0.0
git push origin v1.0.0
```

构建产物包括：

```text
GrowthGarden-Setup-<version>.exe
latest.yml
GrowthGarden-Setup-<version>.exe.blockmap
```

## 隐私与安全

这个项目会保存个人随笔和隐藏信件，请勿把以下内容提交到公开仓库：

```text
db/export/
src/main/resources/db/desktop/letters.sql
.env
application-local.properties
```

隐藏信件只存在于私有 seed 和运行数据库中；桌面版构建时可以通过私有 seed 注入，不使用公开仓库保存信件正文。

如果要把桌面版安装包发给别人，请注意安装包内的信件可以被解包读取，界面上的“锁定”不是加密。

## 说明

- 服务器版使用 MySQL；桌面版使用内置 H2，不需要安装数据库。
- 桌面版自动更新依赖 GitHub Releases；如果网络无法访问 GitHub，需要配置可访问的更新镜像。
- 生产环境请通过环境变量提供数据库密码、JWT secret、管理员凭据和 DeepSeek API Key。
