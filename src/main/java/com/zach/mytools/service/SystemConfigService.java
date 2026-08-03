package com.zach.mytools.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zach.mytools.dto.HolidayItem;
import com.zach.mytools.entity.SystemConfig;
import com.zach.mytools.mapper.SystemConfigMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 系统配置服务层
 * 管理节假日等系统参数，数据存储在 system_config 表中
 */
@Slf4j
@Service
public class SystemConfigService {

    private final SystemConfigMapper configMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SystemConfigService(SystemConfigMapper configMapper) {
        this.configMapper = configMapper;
    }

    // ==================== 节假日配置 ====================

    /**
     * 获取系统配置（始终返回第一条记录，不存在则使用默认值）
     */
    public SystemConfig getConfig() {
        SystemConfig config = configMapper.selectById(1L);
        if (config == null) {
            config = new SystemConfig();
            config.setId(1L);
            config.setHolidays("[]");
        }
        return config;
    }

    /**
     * 获取节假日列表
     */
    public List<HolidayItem> getHolidays() {
        String json = getConfig().getHolidays();
        return parseHolidays(json);
    }

    /**
     * 更新节假日列表
     */
    public SystemConfig saveHolidays(List<HolidayItem> holidays, String updatedBy) {
        String json;
        try {
            json = objectMapper.writeValueAsString(holidays != null ? holidays : Collections.emptyList());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("节假日数据序列化失败", e);
        }

        SystemConfig config = configMapper.selectById(1L);
        boolean isNew = (config == null);
        if (isNew) {
            config = new SystemConfig();
            config.setId(1L);
        }
        config.setHolidays(json);
        config.setUpdatedBy(updatedBy);

        if (isNew) {
            configMapper.insert(config);
        } else {
            configMapper.updateById(config);
        }
        return config;
    }

    /**
     * 解析节假日 JSON 为列表
     */
    private List<HolidayItem> parseHolidays(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<HolidayItem>>() {});
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
        }
    }
}
