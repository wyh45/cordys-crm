package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthFollowRecord;
import cn.cordys.crm.health.dto.HealthFollowPageRequest;
import cn.cordys.crm.health.service.HealthFollowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 健康随访管理接口
 */
@Tag(name = "健康管理-健康干预/随访")
@RestController
@RequestMapping("/health/follow")
public class HealthFollowController {

    @Resource
    private HealthFollowService healthFollowService;

    @PostMapping("/page")
    @Operation(summary = "随访记录列表（分页）")
    public Map<String, Object> page(@RequestBody HealthFollowPageRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        return healthFollowService.listFollowRecords(pageNum, pageSize);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "随访详情")
    public HealthFollowRecord get(@PathVariable String id) {
        return healthFollowService.getFollowRecord(id);
    }

    @PostMapping("/save")
    @Operation(summary = "新建/更新随访记录")
    public void save(@RequestBody HealthFollowRecord record) {
        healthFollowService.saveFollowRecord(record);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除随访记录")
    public void delete(@PathVariable String id) {
        healthFollowService.deleteFollowRecord(id);
    }

    @PostMapping("/mark-contacted/{archiveId}")
    @Operation(summary = "标记客户已联系")
    public void markContacted(@PathVariable String archiveId) {
        healthFollowService.markContacted(archiveId);
    }

    @GetMapping("/by-archive/{archiveId}")
    @Operation(summary = "客户随访记录")
    public List<HealthFollowRecord> byArchive(@PathVariable String archiveId) {
        return healthFollowService.getByArchiveId(archiveId);
    }

    @PostMapping("/record-phone-contact/{archiveId}")
    @Operation(summary = "记录电话随访")
    public void recordPhoneContact(@PathVariable String archiveId, @RequestBody Map<String, Integer> params) {
        int intervalDays = params.getOrDefault("followInterval", 7);
        healthFollowService.recordPhoneContact(archiveId, intervalDays);
    }

    @PostMapping("/record-sms-push/{archiveId}")
    @Operation(summary = "记录短信推送随访")
    public void recordSmsPush(@PathVariable String archiveId, @RequestBody Map<String, Integer> params) {
        int intervalDays = params.getOrDefault("followInterval", 7);
        healthFollowService.recordSmsPush(archiveId, intervalDays);
    }

    @PostMapping("/batch-action-status")
    @Operation(summary = "批量查询随访操作状态")
    public Map<String, Map<String, Object>> batchActionStatus(@RequestBody Map<String, List<String>> params) {
        List<String> archiveIds = params.getOrDefault("archiveIds", List.of());
        return healthFollowService.batchCheckActionStatus(archiveIds);
    }
}
