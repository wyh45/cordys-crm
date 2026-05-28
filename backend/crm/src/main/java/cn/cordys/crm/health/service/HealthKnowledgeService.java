package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthKnowledge;
import cn.cordys.mybatis.BaseMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
public class HealthKnowledgeService {

    @Resource
    private BaseMapper<HealthKnowledge> knowledgeMapper;

    @Resource
    private HealthKnowledgeEmbeddingService embeddingService;

    public Map<String, Object> listKnowledge(int pageNum, int pageSize, String keyword, String category) {
        HealthKnowledge criteria = new HealthKnowledge();
        List<HealthKnowledge> list = knowledgeMapper.select(criteria);
        if (StringUtils.isNotBlank(keyword)) {
            String kw = keyword.toLowerCase();
            list = list.stream()
                .filter(k -> (k.getTitle() != null && k.getTitle().toLowerCase().contains(kw)) ||
                             (k.getContent() != null && k.getContent().toLowerCase().contains(kw)) ||
                             (k.getTags() != null && k.getTags().toLowerCase().contains(kw)))
                .toList();
        }
        if (StringUtils.isNotBlank(category)) {
            list = list.stream().filter(k -> category.equals(k.getCategory())).toList();
        }
        list.sort((a, b) -> Long.compare(
            b.getUpdateTime() != null ? b.getUpdateTime() : 0L,
            a.getUpdateTime() != null ? a.getUpdateTime() : 0L
        ));
        int total = list.size();
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<HealthKnowledge> page = start >= total ? List.of() : list.subList(start, end);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("list", page);
        result.put("total", total);
        result.put("current", pageNum);
        result.put("pageSize", pageSize);
        return result;
    }

    public HealthKnowledge getKnowledge(String id) {
        return knowledgeMapper.selectByPrimaryKey(id);
    }

    @Transactional
    public void saveKnowledge(HealthKnowledge knowledge) {
        if (knowledge.getId() == null) {
            knowledge.setId(IDGenerator.nextStr());
            knowledge.setCreateTime(System.currentTimeMillis());
        }
        knowledge.setUpdateTime(System.currentTimeMillis());
        HealthKnowledge existing = knowledgeMapper.selectByPrimaryKey(knowledge.getId());
        if (existing != null && existing.getId() != null) {
            knowledgeMapper.updateById(knowledge);
        } else {
            knowledgeMapper.insert(knowledge);
        }
        embeddingService.computeAndStoreEmbedding(knowledge);
    }

    /**
     * 语义搜索
     */
    public List<HealthKnowledge> semanticSearch(String query, int topK) {
        return embeddingService.semanticSearch(query, topK);
    }

    @Transactional
    public void deleteKnowledge(String id) {
        knowledgeMapper.deleteByPrimaryKey(id);
    }

    public List<String> getCategories() {
        HealthKnowledge criteria = new HealthKnowledge();
        List<HealthKnowledge> list = knowledgeMapper.select(criteria);
        return list.stream()
            .map(HealthKnowledge::getCategory)
            .filter(c -> c != null && !c.isEmpty())
            .distinct()
            .sorted()
            .toList();
    }
}
