package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.crm.health.dto.HealthArchivePageRequest;
import cn.cordys.crm.health.dto.SyncRequest;
import cn.cordys.crm.health.dto.SyncResult;
import cn.cordys.crm.health.service.HealthArchiveService;
import cn.cordys.mybatis.BaseMapper;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 健康档案管理接口
 */
@Tag(name = "健康管理-健康档案")
@RestController
@RequestMapping("/health/archive")
public class HealthArchiveController {

    @Resource
    private HealthArchiveService healthArchiveService;

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    /**
     * 档案列表（分页）
     */
    @PostMapping("/page")
    @Operation(summary = "档案列表（分页）")
    public Map<String, Object> page(@RequestBody HealthArchivePageRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        Page<HealthArchive> page = PageHelper.startPage(pageNum, pageSize);
        List<HealthArchive> list = healthArchiveService.listArchives(request.getKeyword());

        Map<String, Integer> syncedCounts = new HashMap<>();
        if (list != null && !list.isEmpty()) {
            List<String> archiveIds = list.stream()
                .map(HealthArchive::getId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            syncedCounts = healthArchiveService.batchSyncedCount(archiveIds);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("syncedCounts", syncedCounts);
        result.put("total", page.getTotal());
        result.put("current", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    @PostMapping("/add")
    @Operation(summary = "新增档案")
    public void add(@RequestBody HealthArchive archive) {
        healthArchiveService.addArchive(archive);
    }

    @PostMapping("/update")
    @Operation(summary = "更新档案")
    public void update(@RequestBody HealthArchive archive) {
        healthArchiveService.updateArchive(archive);
    }

    @PostMapping("/delete")
    @Operation(summary = "删除档案")
    public void delete(@RequestBody Map<String, String> params) {
        healthArchiveService.deleteArchive(params.get("id"));
    }

    @PostMapping("/match-customer")
    @Operation(summary = "匹配CRM客户（仅查询，不同步）")
    public Map<String, Object> matchCustomer(@RequestBody Map<String, String> params) {
        return healthArchiveService.matchCustomer(params.get("id"));
    }

    @GetMapping("/debug-customers")
    @Operation(summary = "调试：查看CRM客户数据")
    public Map<String, Object> debugCustomers() {
        return healthArchiveService.debugCustomers();
    }

    @PostMapping("/sync")
    @Operation(summary = "同步档案到体检数据")
    public SyncResult sync(@RequestBody SyncRequest request) {
        return healthArchiveService.syncToExamination(request.getId());
    }

    /**
     * 档案详情
     */
    @GetMapping("/get/{id}")
    @Operation(summary = "档案详情")
    public HealthArchive get(@PathVariable String id) {
        return healthArchiveService.getArchive(id);
    }

    /**
     * 档案关联的体检记录
     */
    @GetMapping("/examination/{archiveId}")
    @Operation(summary = "体检记录列表")
    public List<HealthExamination> examinations(@PathVariable String archiveId) {
        return healthArchiveService.getExaminations(archiveId);
    }

    /**
     * 新增/更新过敏史
     */
    @PostMapping("/allergy/save")
    @Operation(summary = "保存过敏史")
    public void saveAllergy(@RequestBody cn.cordys.crm.health.domain.HealthAllergy allergy) {
        healthArchiveService.saveAllergy(allergy);
    }

    /**
     * 新增/更新病史
     */
    @PostMapping("/history/save")
    @Operation(summary = "保存病史")
    public void saveHistory(@RequestBody cn.cordys.crm.health.domain.HealthMedicalHistory history) {
        healthArchiveService.saveHistory(history);
    }

    /**
     * 新增/更新疫苗接种
     */
    @PostMapping("/vaccination/save")
    @Operation(summary = "保存疫苗接种")
    public void saveVaccination(@RequestBody cn.cordys.crm.health.domain.HealthVaccination vaccination) {
        healthArchiveService.saveVaccination(vaccination);
    }

    /**
     * 获取过敏史列表
     */
    @GetMapping("/allergy/{archiveId}")
    @Operation(summary = "过敏史列表")
    public List<cn.cordys.crm.health.domain.HealthAllergy> allergies(@PathVariable String archiveId) {
        return healthArchiveService.getAllergies(archiveId);
    }

    /**
     * 获取病史列表
     */
    @GetMapping("/history/{archiveId}")
    @Operation(summary = "病史列表")
    public List<cn.cordys.crm.health.domain.HealthMedicalHistory> histories(@PathVariable String archiveId) {
        return healthArchiveService.getHistories(archiveId);
    }

    /**
     * 获取疫苗接种列表
     */
    @GetMapping("/vaccination/{archiveId}")
    @Operation(summary = "疫苗接种列表")
    public List<cn.cordys.crm.health.domain.HealthVaccination> vaccinations(@PathVariable String archiveId) {
        return healthArchiveService.getVaccinations(archiveId);
    }

    /**
     * 删除过敏史
     */
    @PostMapping("/allergy/delete")
    @Operation(summary = "删除过敏史")
    public void deleteAllergy(@RequestBody Map<String, String> params) {
        healthArchiveService.deleteAllergy(params.get("id"));
    }

    /**
     * 删除病史
     */
    @PostMapping("/history/delete")
    @Operation(summary = "删除病史")
    public void deleteHistory(@RequestBody Map<String, String> params) {
        healthArchiveService.deleteHistory(params.get("id"));
    }

    /**
     * 删除疫苗接种
     */
    @PostMapping("/vaccination/delete")
    @Operation(summary = "删除疫苗接种")
    public void deleteVaccination(@RequestBody Map<String, String> params) {
        healthArchiveService.deleteVaccination(params.get("id"));
    }

    /**
     * 新增/更新体检记录
     */
    @PostMapping("/examination/save")
    @Operation(summary = "保存体检记录")
    public void saveExamination(@RequestBody HealthExamination exam) {
        healthArchiveService.saveExamination(exam);
    }

    /**
     * 删除体检记录
     */
    @PostMapping("/examination/delete")
    @Operation(summary = "删除体检记录")
    public void deleteExamination(@RequestBody Map<String, String> params) {
        healthArchiveService.deleteExamination(params.get("id"));
    }
}
