package com.zach.mytools.config;

import com.zach.mytools.entity.Employee;
import com.zach.mytools.entity.SystemConfig;
import com.zach.mytools.mapper.EmployeeMapper;
import com.zach.mytools.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 数据初始化器：首次启动时自动创建默认管理员账号、员工通讯录和系统配置
 */
@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final EmployeeMapper employeeMapper;
    private final SystemConfigMapper configMapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DataInitializer(EmployeeMapper employeeMapper,
                           SystemConfigMapper configMapper) {
        this.employeeMapper = employeeMapper;
        this.configMapper = configMapper;
    }

    @Override
    public void run(String... args) {
        initAdmin();
        initEmployeeContacts();
        initSystemConfig();
    }

    /**
     * 初始化员工通讯录（解析自"管理人员通讯录及邮箱"图片）
     * 已存在的同名用户名不会重复插入，仅补充缺失用户
     */
    private void initEmployeeContacts() {
        String[][] contacts = {
            // {username, name, workNo, department, phone, email}
            {"gaoderong",     "高德荣",   "1",  "management",   "13002151771", "gaoderong2010@163.com"},
            {"diaocaixi",     "刁才喜",   "2",  "management",   "13651875492", "990471128@qq.com"},
            {"caichenghai",   "蔡成海",   "3",  "management",   "13818479519", "caichenghai@163.com"},
            {"shendongjian",  "沈冬建",   "4",  "management",   "13512185493", "18116487820@163.com"},
            {"wuxiaobing",    "吴小兵",   "5",  "management",   "15601605927", "15601605927@163.com"},
            {"xiongliangliang","熊亮亮",  "6",  "general",      "13773820877", "XLL613713@qq.com"},
            {"zhaohongjun",   "赵宏军",   "7",  "general",      "13621895839", "652833580@qq.com"},
            {"hanchangqi",    "韩昌琪",   "8",  "general",      "13621706448", "2247648584@qq.com"},
            {"qianjiqiu",     "钱季秋",   "9",  "general",      "13626297740", "605368476@qq.com"},
            {"huangrenzhong", "黄任重",   "10", "general",      "15190888642", "15190888642@163.com"},
            {"wangdan",       "王���",     "11", "general",      "15851361377", "473136372@qq.com"},
            {"guozhoumanjie", "郭周漫杰", "12", "general",      "13382365866", "2829654828@qq.com"},
            {"xuehao",        "薛昊",     "13", "finance",      "13646198116", "2907808125@qq.com"},
            {"zhouxiaojuan",  "周小娟",   "14", "finance",      "18362115646", "632638156@qq.com"},
            {"shixiaoyong",   "石小勇",   "15", "technology",   "13052205221", "jszb@rongyongmachine.com"},
            {"zhengxiaolong", "郑小龙",   "16", "technology",   "18001483638", "13921683755@163.com"},
            {"liunianfan",    "刘念凡",   "17", "technology",   "15851294124", "15851294124@163.com"},
            {"guochao",       "郭潮",     "18", "technology",   "13160360121", "13160360121@163.com"},
            {"sunhaijuan",    "孙海娟",   "19", "technology",   "13776963237", "sunhaijuan@rongyongmachine.com"},
            {"zhushijie",     "朱仕杰",   "20", "technology",   "15601971229", "zhushijie1228@163.com"},
            {"zhuyusheng",    "朱宇圣",   "21", "technology",   "19851424372", "zys080401@163.com"},
            {"yuandandan",    "袁丹丹",   "22", "technology",   "18248724816", "1667683955@qq.com"},
            {"shenzhengke",   "沈争珂",   "23", "production",   "15052379214", "15052379214@163.com"},
            {"chenfayang",    "陈发扬",   "24", "production",   "13915711573", "455947452@qq.com"},
            {"zhangpinqi",    "张品其",   "25", "production",   "19951102817", "19951102817@163.com"},
            {"jingrong",      "景荣",     "26", "production",   "13764139035", "1101463630@qq.com"},
            {"panxiangxiang", "潘相祥",   "27", "production",   "13564278368", "13564278368@163.com"},
            {"shiguangbing",  "时广兵",   "28", "production",   "18964835502", "1134336753@qq.com"},
            {"zhangzhaoping", "张兆平",   "29", "production",   "13472627236", "13472627236@163.com"},
            {"miaochangliang","缪昌亮",   "30", "production",   "13681829561", "miuchangliangry@163.com"},
            {"xuguobing",     "徐国兵",   "31", "production",   "15214389512", "xgb8269@sina.com"},
            {"zhuqiaosheng",  "朱乔圣",   "32", "production",   "15901609005", "15901609005@163.com"},
            {"chenchanggui",  "陈昌贵",   "33", "production",   "13818251170", "13818251170@163.com"},
            {"shiqingsong",   "时青松",   "34", "quality",      "13661449616", "1463046493@qq.com"},
            {"zhaohongbing",  "赵宏兵",   "35", "quality",      "15996653753", "1354762505@qq.com"},
            {"hongshutao",    "洪书涛",   "36", "quality",      "13732528002", "hsht200818@163.com"},
            {"zhaidongwei",   "翟红伟",   "37", "quality",      "17337881918", "zhw202405@163.com"},
            {"louhuaqing",    "娄华庆",   "38", "quality",      "19850377178", "19850377178@163.com"},
            {"juwei",         "鞠伟",     "39", "quality",      "15850891809", "juweikike2024@163.com"},
            {"youjun",        "尤军",     "40", "precision",    "18252800588", "153621327@qq.com"}
        };

        int inserted = 0, skipped = 0;
        for (int i = 0; i < contacts.length; i++) {
            String[] c = contacts[i];
            String username = c[0];
            if (employeeMapper.findByUsername(username) != null) {
                skipped++;
                continue;
            }
            try {
                Employee e = new Employee();
                e.setUsername(username);
                e.setPassword(passwordEncoder.encode("123456")); // 默认密码 123456
                e.setName(c[1]);
                e.setWorkNo(String.format("EMP%03d", i + 4)); // EMP003 起，自动递增
                e.setDepartment(c[3]);
                e.setPhone(c[4]);
                e.setEmail(c[5]);
                e.setRole("EMPLOYEE");
                e.setStatus(1);
                employeeMapper.insert(e);
                inserted++;
            } catch (Exception ex) {
                log.warn("  插入员工 {} ({}) 失败: {}", username, c[1], ex.getMessage());
            }
        }

        log.info("  员工通讯录初始化完成：新插入 {} 人，跳过已存在 {} 人（默认密码 123456）", inserted, skipped);
    }

    /**
     * 初始化默认管理员账号
     */
    private void initAdmin() {
        Employee existing = employeeMapper.findByUsername("admin");
        if (existing == null) {
            Employee admin = new Employee();
            admin.setUsername("admin");
            admin.setPassword(passwordEncoder.encode("admin123"));
            admin.setName("管理员");
            admin.setWorkNo("EMP001");
            admin.setDepartment("technology");
            admin.setPhone("13800000000");
            admin.setRole("ADMIN");
            admin.setStatus(1);
            employeeMapper.insert(admin);

            log.info("======================================");
            log.info("  已创��默认管理员账号：");
            log.info("  用户名: admin");
            log.info("  密码: admin123");
            log.info("  (请及时修改密码！)");
            log.info("======================================");
        } else if (existing.getRole() == null || !"ADMIN".equals(existing.getRole())) {
            // 兼容已有数据：确保 admin 账号拥有 ADMIN 角色
            existing.setRole("ADMIN");
            employeeMapper.updateById(existing);
        }
    }

    /**
     * 初始化默认系统配置（节假日为空数组）
     */
    private void initSystemConfig() {
        SystemConfig config = configMapper.selectById(1L);
        if (config == null) {
            config = new SystemConfig();
            config.setId(1L);
            config.setHolidays("[]");
            config.setUpdatedBy("system");
            configMapper.insert(config);

            log.info("  已初始化默认系统配置（节假日待管理员维护）");
        }
    }
}
