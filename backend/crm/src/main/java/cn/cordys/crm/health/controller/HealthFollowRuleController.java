package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthFollowRule;
import cn.cordys.crm.health.service.HealthFollowRuleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * 随访规则管理接口
 */
@Tag(name = "健康管理-随访规则")
@RestController
@RequestMapping("/health/rule")
public class HealthFollowRuleController {

    @Resource
    private HealthFollowRuleService ruleService;

    @PostMapping("/page")
    @Operation(summary = "规则列表（分页）")
    public List<HealthFollowRule> page(@RequestBody Map<String, Object> request) {
        int pageNum = request.get("page") != null ? (int) request.get("page") : 1;
        int pageSize = request.get("pageSize") != null ? (int) request.get("pageSize") : 20;
        return ruleService.listRules(pageNum, pageSize);
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "规则详情")
    public HealthFollowRule get(@PathVariable String id) {
        return ruleService.getRule(id);
    }

    @PostMapping("/save")
    @Operation(summary = "新建/更新规则")
    public void save(@RequestBody HealthFollowRule rule) {
        ruleService.saveRule(rule);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除规则")
    public void delete(@PathVariable String id) {
        ruleService.deleteRule(id);
    }

    @PostMapping("/enable/{id}")
    @Operation(summary = "启用规则")
    public void enable(@PathVariable String id) {
        ruleService.enableRule(id);
    }

    @PostMapping("/disable/{id}")
    @Operation(summary = "禁用规则")
    public void disable(@PathVariable String id) {
        ruleService.disableRule(id);
    }

    @GetMapping("/evaluate")
    @Operation(summary = "评估所有启用规则，返回每个规则匹配的档案列表")
    public List<RuleMatchedArchive> evaluateAllRules() {
        return ruleService.evaluateAllRules();
    }

    @lombok.Data
    public static class RuleMatchedArchive {
        private String ruleId;
        private String ruleName;
        private String followMethod;
        private int followInterval;
        private String archiveId;
        private String archiveNo;
        private String customerName;
        private String phone;
        private int abnormalCount;
        private List<String> matchedExamItems;
        private boolean synced;
    }
}