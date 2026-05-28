package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthPushRecord;
import cn.cordys.crm.health.dto.HealthPushRequest;
import cn.cordys.crm.health.service.HealthPushService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 健康推送接口
 */
@Tag(name = "健康管理-健康推送")
@RestController
@RequestMapping("/health/push")
public class HealthPushController {

    @Resource
    private HealthPushService pushService;

    @PostMapping("/send")
    @Operation(summary = "推送健康知识给客户")
    public void send(@RequestBody HealthPushRequest request, HttpServletRequest httpRequest) {
        String operatorId = httpRequest.getHeader("X-User-Id");
        String organizationId = httpRequest.getHeader("X-Org-Id");
        pushService.pushKnowledge(request, operatorId, organizationId);
    }

    @PostMapping("/page")
    @Operation(summary = "推送记录列表（分页）")
    public List<HealthPushRecord> page(@RequestParam(defaultValue = "1") int page,
                                        @RequestParam(defaultValue = "20") int pageSize) {
        return pushService.listPushRecords(page, pageSize);
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "查询某客户的推送记录")
    public List<HealthPushRecord> byCustomer(@PathVariable String customerId) {
        return pushService.getByCustomerId(customerId);
    }

    @GetMapping("/knowledge/{knowledgeId}")
    @Operation(summary = "查询某知识的推送记录")
    public List<HealthPushRecord> byKnowledge(@PathVariable String knowledgeId) {
        return pushService.getByKnowledgeId(knowledgeId);
    }
}
