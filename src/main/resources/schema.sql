-- ============================================================
-- 员工信息表
-- ============================================================
CREATE TABLE IF NOT EXISTS employee
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username    VARCHAR(50)  NOT NULL COMMENT '登录用户名',
    password    VARCHAR(100) NOT NULL COMMENT '密码(BCrypt加密)',
    name        VARCHAR(50)  NOT NULL COMMENT '员工姓名',
    work_no     VARCHAR(30)           DEFAULT NULL COMMENT '工号',
    department  VARCHAR(50)           DEFAULT NULL COMMENT '部门',
    phone       VARCHAR(20)           DEFAULT NULL COMMENT '手机号',
    email       VARCHAR(100)          DEFAULT NULL COMMENT '邮箱',
    role        VARCHAR(20)           DEFAULT 'EMPLOYEE' COMMENT '角色：ADMIN-管理员 EMPLOYEE-普通员工',
    status      TINYINT               DEFAULT 1 COMMENT '1启用 0禁用',
    created_at  DATETIME              DEFAULT NULL COMMENT '创建时间',
    updated_at  DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_username (username)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '员工信息表';

-- 给 employee 新增 role 列（兼容已有数据）
ALTER TABLE employee ADD COLUMN IF NOT EXISTS role VARCHAR(20) DEFAULT 'EMPLOYEE' COMMENT '角色：ADMIN-管理员 EMPLOYEE-普通员工' AFTER phone;
UPDATE employee SET role = 'ADMIN' WHERE username = 'admin' AND (role IS NULL OR role = 'EMPLOYEE');

-- 给 employee 新增 email 列（兼容已有数据）
ALTER TABLE employee ADD COLUMN IF NOT EXISTS email VARCHAR(100) DEFAULT NULL COMMENT '邮箱' AFTER phone;

-- ============================================================
-- 系统配置表（管理员可设置系统参数，如节假日配置）
-- ============================================================
CREATE TABLE IF NOT EXISTS system_config
(
    id               BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    holidays         TEXT                  DEFAULT NULL COMMENT '节假日配置(JSON数组：[{name,startDate,endDate}])',
    updated_by       VARCHAR(50)           DEFAULT NULL COMMENT '最后修改人',
    created_at       DATETIME              DEFAULT NULL COMMENT '创建时间',
    updated_at       DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '系统配置表';

-- 兼容已有数据库：删除旧考勤时间字段，新增节假日字段
ALTER TABLE system_config DROP COLUMN IF EXISTS work_on_time;
ALTER TABLE system_config DROP COLUMN IF EXISTS work_off_time;
ALTER TABLE system_config DROP COLUMN IF EXISTS overtime_time;
-- 2. 新增节假日配置字段（JSON 文本）
-- 格式：[{"name":"春节","type":"holiday","startDate":"2026-02-16","endDate":"2026-02-22"},{"name":"五一调班","type":"shift","startDate":"2026-04-26","endDate":"2026-04-26"}]
ALTER TABLE system_config ADD COLUMN IF NOT EXISTS holidays TEXT DEFAULT NULL COMMENT '节假日/调班配置(JSON，type=holiday|shift)' AFTER id;
-- ============================================================
-- 工时填报记录表
-- ============================================================
CREATE TABLE IF NOT EXISTS timesheet_record
(
    id             BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    emp_id         BIGINT       NOT NULL COMMENT '员工ID(关联employee.id)',
    date           DATE         NOT NULL COMMENT '填报日期',
    project        VARCHAR(100)          DEFAULT NULL COMMENT '项目名称',
    task           VARCHAR(200)          DEFAULT NULL COMMENT '任务名称',
    work_hours     DECIMAL(4,1)          DEFAULT NULL COMMENT '工时(小时)',
    overtime_hours DECIMAL(4,1)          DEFAULT NULL COMMENT '加班时长(小时)',
    status         VARCHAR(20)           DEFAULT 'DRAFT' COMMENT '状态: DRAFT-草稿 SUBMITTED-已提交',
    created_at     DATETIME              DEFAULT NULL COMMENT '创建时间',
    updated_at     DATETIME              DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_emp_date (emp_id, date),
    KEY idx_emp_id (emp_id),
    KEY idx_date (date)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '工时填报记录表';

-- 兼容已有数据库：新增项目名称和任务字段
ALTER TABLE timesheet_record ADD COLUMN IF NOT EXISTS project VARCHAR(100) DEFAULT NULL COMMENT '项目名称' AFTER date;
ALTER TABLE timesheet_record ADD COLUMN IF NOT EXISTS task VARCHAR(200) DEFAULT NULL COMMENT '任务名称' AFTER project;

-- 2026节假日配置（仅首次初始化，已存在则跳过）
INSERT IGNORE INTO `system_config` (`id`, `holidays`, `updated_by`, `created_at`, `updated_at`) VALUES ('1', '[{"name":"元旦","type":"holiday","startDate":"2026-01-01","endDate":"2026-01-03"},{"name":"元旦补班","type":"shift","startDate":"2026-01-04","endDate":"2026-01-04"},{"name":"春节补班","type":"shift","startDate":"2026-02-14","endDate":"2026-02-14"},{"name":"春节","type":"holiday","startDate":"2026-02-15","endDate":"2026-02-23"},{"name":"春节补班","type":"shift","startDate":"2026-02-28","endDate":"2026-02-28"},{"name":"清明节","type":"holiday","startDate":"2026-04-04","endDate":"2026-04-06"},{"name":"劳动节","type":"holiday","startDate":"2026-05-01","endDate":"2026-05-05"},{"name":"劳动节补班","type":"shift","startDate":"2026-05-09","endDate":"2026-05-09"},{"name":"端午节","type":"holiday","startDate":"2026-06-19","endDate":"2026-06-21"},{"name":"国庆节补班","type":"shift","startDate":"2026-09-20","endDate":"2026-09-20"},{"name":"中秋节","type":"holiday","startDate":"2026-09-25","endDate":"2026-09-27"},{"name":"国庆节","type":"holiday","startDate":"2026-10-01","endDate":"2026-10-07"},{"name":"国庆节补班","type":"shift","startDate":"2026-10-10","endDate":"2026-10-10"}]', 'admin', '2026-07-07 19:04:29', '2026-07-28 16:46:57');

-- ============================================================
-- 字典类型表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_type
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_type   VARCHAR(50)  NOT NULL COMMENT '字典类型编码（如 system_dept）',
    dict_name   VARCHAR(100) NOT NULL COMMENT '字典类型名称（如 部门列表）',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    created_at  DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_dict_type (dict_type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典类型表';

-- ============================================================
-- 字典数据项表
-- ============================================================
CREATE TABLE IF NOT EXISTS sys_dict_item
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    dict_type   VARCHAR(50)  NOT NULL COMMENT '字典类型编码',
    dict_label  VARCHAR(100) NOT NULL COMMENT '展示名称（部门名称）',
    dict_value  VARCHAR(50)  NOT NULL COMMENT '唯一编码Key（部门编码，英文/数字）',
    sort        INT          DEFAULT 0 COMMENT '排序（升序）',
    status      TINYINT      DEFAULT 1 COMMENT '状态：1启用 0禁用',
    remark      VARCHAR(200) DEFAULT NULL COMMENT '备注',
    created_at  DATETIME     DEFAULT NULL COMMENT '创建时间',
    updated_at  DATETIME     DEFAULT NULL COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_type_value (dict_type, dict_value)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '字典数据项表';

-- 种子数据：部门字典类型
INSERT IGNORE INTO sys_dict_type (dict_type, dict_name, remark) VALUES
('system_dept', '部门列表', '公司组织架构-平级部门');

-- 种子数据：7个部门（dict_label 为页面展示名称，统一以"部"结尾）
INSERT IGNORE INTO sys_dict_item (dict_type, dict_label, dict_value, sort) VALUES
('system_dept', '管理部', 'management',   1),
('system_dept', '综合部', 'general',      2),
('system_dept', '财务部', 'finance',      3),
('system_dept', '技术部', 'technology',   4),
('system_dept', '生产部', 'production',   5),
('system_dept', '品保部', 'quality',      6),
('system_dept', '精度部', 'precision',    7);

-- 已部署数据库升级：将现有 dict_label 统一更新为带"部"结尾（仅对未带"部"的记录）
UPDATE sys_dict_item SET dict_label = '管理部' WHERE dict_type = 'system_dept' AND dict_value = 'management' AND dict_label != '管理部';
UPDATE sys_dict_item SET dict_label = '财务部' WHERE dict_type = 'system_dept' AND dict_value = 'finance'     AND dict_label != '财务部';
UPDATE sys_dict_item SET dict_label = '技术部' WHERE dict_type = 'system_dept' AND dict_value = 'technology'  AND dict_label != '技术部';
UPDATE sys_dict_item SET dict_label = '生产部' WHERE dict_type = 'system_dept' AND dict_value = 'production'  AND dict_label != '生产部';
UPDATE sys_dict_item SET dict_label = '品保部' WHERE dict_type = 'system_dept' AND dict_value = 'quality'     AND dict_label != '品保部';
UPDATE sys_dict_item SET dict_label = '精度部' WHERE dict_type = 'system_dept' AND dict_value = 'precision'   AND dict_label != '精度部';

-- 历史数据迁移：将 employee.department 从中文名称更新为系统编码
UPDATE employee SET department = 'management'  WHERE department = '管理';
UPDATE employee SET department = 'general'     WHERE department = '综合部';
UPDATE employee SET department = 'finance'     WHERE department = '财务';
UPDATE employee SET department = 'technology'  WHERE department = '技术';
UPDATE employee SET department = 'production'  WHERE department = '生产';
UPDATE employee SET department = 'quality'     WHERE department = '品保';
UPDATE employee SET department = 'precision'   WHERE department = '精度';

-- ============================================================
-- 操作日志表（广播日志）
-- ============================================================
CREATE TABLE IF NOT EXISTS operation_log
(
    id          BIGINT       NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    emp_id      BIGINT       NOT NULL COMMENT '操作员工ID(关联employee.id)',
    emp_name    VARCHAR(50)  NOT NULL COMMENT '操作员工姓名',
    operation   VARCHAR(30)  NOT NULL COMMENT '操作类型: CREATE/UPDATE/DELETE/BATCH_CREATE/BATCH_DELETE',
    target_type VARCHAR(30)  NOT NULL COMMENT '目标类型: TIMESHEET',
    target_id   BIGINT                COMMENT '目标记录ID',
    target_date DATE                  COMMENT '目标日期(工时填报日期)',
    detail      VARCHAR(500)          COMMENT '操作详情描述',
    created_at  DATETIME     NOT NULL COMMENT '操作时间',
    PRIMARY KEY (id),
    KEY idx_created_at (created_at),
    KEY idx_emp_id (emp_id)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci
  COMMENT = '操作日志表（广播日志）';

