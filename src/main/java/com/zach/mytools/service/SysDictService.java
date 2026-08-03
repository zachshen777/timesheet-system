package com.zach.mytools.service;

import com.zach.mytools.dto.DeptSaveRequest;
import com.zach.mytools.dto.DictOptionDTO;
import com.zach.mytools.entity.SysDictItem;
import com.zach.mytools.mapper.EmployeeMapper;
import com.zach.mytools.mapper.SysDictItemMapper;
import com.zach.mytools.mapper.TimesheetRecordMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 字典服务（部门管理 + 通用下拉查询）
 */
@Slf4j
@Service
public class SysDictService {

    private static final String DEPT_DICT_TYPE = "system_dept";

    private final SysDictItemMapper dictItemMapper;
    private final EmployeeMapper employeeMapper;
    private final TimesheetRecordMapper timesheetRecordMapper;

    public SysDictService(SysDictItemMapper dictItemMapper,
                          EmployeeMapper employeeMapper,
                          TimesheetRecordMapper timesheetRecordMapper) {
        this.dictItemMapper = dictItemMapper;
        this.employeeMapper = employeeMapper;
        this.timesheetRecordMapper = timesheetRecordMapper;
    }

    // ==================== 公共下拉 ====================

    /**
     * 获取部门下拉选项（仅启用项）
     */
    public List<DictOptionDTO> getDeptOptions() {
        List<SysDictItem> items = dictItemMapper.findEnabledByDictType(DEPT_DICT_TYPE);
        return items.stream()
                .map(i -> new DictOptionDTO(i.getDictLabel(), i.getDictValue()))
                .collect(Collectors.toList());
    }

    /**
     * 将部门编码翻译为中文名称（用于列表展示）
     */
    public String translateDeptCode(String deptCode) {
        if (deptCode == null || deptCode.isEmpty()) return deptCode;
        List<SysDictItem> items = dictItemMapper.findByDictType(DEPT_DICT_TYPE);
        return items.stream()
                .filter(i -> deptCode.equals(i.getDictValue()))
                .findFirst()
                .map(SysDictItem::getDictLabel)
                .orElse(deptCode); // 找不到则原样返回
    }

    // ==================== 管理 CRUD ====================

    /**
     * 查询所有部门（含禁用）
     */
    public List<SysDictItem> listAll() {
        return dictItemMapper.findByDictType(DEPT_DICT_TYPE);
    }

    /**
     * 新增部门
     */
    public SysDictItem create(DeptSaveRequest request) {
        // 校验编码唯一性
        List<SysDictItem> existing = dictItemMapper.findByDictType(DEPT_DICT_TYPE);
        boolean dup = existing.stream().anyMatch(i -> request.getDictValue().equals(i.getDictValue()));
        if (dup) {
            throw new IllegalArgumentException("部门编码「" + request.getDictValue() + "」已存在");
        }

        SysDictItem item = new SysDictItem();
        item.setDictType(DEPT_DICT_TYPE);
        item.setDictValue(request.getDictValue());
        item.setDictLabel(request.getDictLabel());
        item.setSort(request.getSort() != null ? request.getSort() : 0);
        item.setStatus(1);
        item.setRemark(request.getRemark());
        dictItemMapper.insert(item);
        log.info("新增部门: {} ({})", item.getDictLabel(), item.getDictValue());
        return item;
    }

    /**
     * 编辑部门（dictValue 不可修改）
     */
    public SysDictItem update(Long id, DeptSaveRequest request) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        if (!DEPT_DICT_TYPE.equals(item.getDictType())) {
            throw new IllegalArgumentException("不允许修改非部门数据");
        }

        item.setDictLabel(request.getDictLabel());
        item.setSort(request.getSort() != null ? request.getSort() : item.getSort());
        item.setRemark(request.getRemark());
        dictItemMapper.updateById(item);
        log.info("编辑部门: id={} -> {}", id, item.getDictLabel());
        return item;
    }

    /**
     * 启用/禁用切换
     */
    public SysDictItem toggleStatus(Long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("部门不存在");
        }
        item.setStatus(item.getStatus() == 1 ? 0 : 1);
        dictItemMapper.updateById(item);
        log.info("部门状态切换: {} -> {}", item.getDictLabel(), item.getStatus() == 1 ? "启用" : "禁用");
        return item;
    }

    /**
     * 删除部门（前置校验）
     */
    public void delete(Long id) {
        SysDictItem item = dictItemMapper.selectById(id);
        if (item == null) {
            throw new IllegalArgumentException("部门不存在");
        }

        String deptCode = item.getDictValue();

        // 校验1：在职员工
        int activeEmpCount = employeeMapper.countByDepartment(deptCode);
        if (activeEmpCount > 0) {
            throw new IllegalStateException(
                    "该部门「" + item.getDictLabel() + "」下存在 " + activeEmpCount + " 名在职员工，请先转移员工部门后再删除");
        }

        // 校验2：历史工时记录（仅提示）
        int historyCount = timesheetRecordMapper.countByDepartment(deptCode);
        if (historyCount > 0) {
            throw new IllegalStateException(
                    "该部门「" + item.getDictLabel() + "」下存在 " + historyCount + " 条历史工时记录，不建议删除。如确需删除，请先清理关联数据");
        }

        dictItemMapper.deleteById(id);
        log.info("删除部门: {} ({})", item.getDictLabel(), deptCode);
    }
}
