package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthKnowledge;
import cn.cordys.crm.health.dto.HealthKnowledgePageRequest;
import cn.cordys.crm.health.service.HealthKnowledgeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 健康知识库接口
 */
@Tag(name = "健康管理-健康宣教/知识库")
@RestController
@RequestMapping("/health/knowledge")
public class HealthKnowledgeController {

    @Resource
    private HealthKnowledgeService knowledgeService;

    @PostMapping("/page")
    @Operation(summary = "知识库列表（分页）")
    public Map<String, Object> page(@RequestBody HealthKnowledgePageRequest request) {
        int pageNum = request.getPage() != null ? request.getPage() : 1;
        int pageSize = request.getPageSize() != null ? request.getPageSize() : 20;
        return knowledgeService.listKnowledge(pageNum, pageSize, request.getKeyword(), request.getCategory());
    }

    @GetMapping("/get/{id}")
    @Operation(summary = "知识详情")
    public HealthKnowledge get(@PathVariable String id) {
        return knowledgeService.getKnowledge(id);
    }

    @PostMapping("/save")
    @Operation(summary = "新建/更新知识")
    public void save(@RequestBody HealthKnowledge knowledge) {
        knowledgeService.saveKnowledge(knowledge);
    }

    @PostMapping("/delete/{id}")
    @Operation(summary = "删除知识")
    public void delete(@PathVariable String id) {
        knowledgeService.deleteKnowledge(id);
    }

    @GetMapping("/categories")
    @Operation(summary = "获取所有分类")
    public List<String> categories() {
        return knowledgeService.getCategories();
    }

    @PostMapping("/search")
    @Operation(summary = "语义搜索健康知识")
    public List<HealthKnowledge> search(@RequestBody Map<String, Object> request) {
        String query = (String) request.get("query");
        int topK = request.get("topK") != null ? (int) request.get("topK") : 10;
        return knowledgeService.semanticSearch(query, topK);
    }
}
