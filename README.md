# 考勤工时管理系统 (Timesheet System)

基于 Spring Boot + Vue 3 的考勤工时管理系统，支持工时填报、报表统计、员工管理、部门管理等功能。

## 技术栈

| 层级 | 技术 |
|------|------|
| 后端 | Spring Boot 3.x, MyBatis-Plus, MariaDB |
| 前端 | Vue 3, Vite, Element Plus, Pinia, ECharts 5 |
| 构建 | Maven, npm |
| 部署 | WSL2 + nginx + systemd |

## 功能模块

- **工时管理** — 日历视图（月/周切换）、单日/批量填报、复制粘贴
- **工时报表** — 月度饼图、汇总表、明细表、出勤表 Excel 导出
- **员工管理** — CRUD、分页、状态切换、多条件筛选
- **部门管理** — 基于字典的部门 CRUD
- **历史查询** — 管理员全员工时记录查询、统计卡片
- **系统配置** — 节假日/调班日管理

## 快速开始

### 后端

```bash
cd mytools
mvn spring-boot:run
```

默认激活 dev 环境，端口 5173。

### 前端

```bash
cd mytools-web
npm install
npm run dev
```

### 生产部署

```bash
# 构建前端
cd mytools-web && npm run build

# 构建后端
cd mytools && mvn clean package -DskipTests

# 部署（详见项目内 systemd 配置）
java -jar target/mytools-1.0-SNAPSHOT.jar --server.port=8080
```

## 数据库

MariaDB，初始化脚本：`src/main/resources/schema.sql`

## 项目路径

- 后端：`/mytools`
- 前端：`/mytools-web`
