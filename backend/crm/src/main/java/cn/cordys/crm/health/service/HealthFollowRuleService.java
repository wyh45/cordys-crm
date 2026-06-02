package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.crm.health.domain.HealthFollowRecord;
import cn.cordys.crm.health.domain.HealthFollowRule;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * 随访规则服务
 * 
 * 触发时机：健康档案新建/更新时，调用triggerRules()遍历所有启用规则，
 * 评估conditionExpr是否匹配archive，匹配则生成HealthFollowRecord
 */
@Service
public class HealthFollowRuleService {

    @Resource
    private BaseMapper<HealthFollowRule> ruleMapper;

    @Resource
    private BaseMapper<HealthFollowRecord> followMapper;

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 规则列表（分页）
     */
    public List<HealthFollowRule> listRules(int pageNum, int pageSize) {
        HealthFollowRule criteria = new HealthFollowRule();
        List<HealthFollowRule> list = ruleMapper.select(criteria);
        // 按priority排序
        list.sort((a, b) -> Integer.compare(
            a.getPriority() != null ? a.getPriority() : 0,
            b.getPriority() != null ? b.getPriority() : 0
        ));
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, list.size());
        if (start >= list.size()) return List.of();
        return list.subList(start, end);
    }

    /**
     * 获取规则详情
     */
    public HealthFollowRule getRule(String id) {
        return ruleMapper.selectByPrimaryKey(id);
    }

    /**
     * 新建/更新规则
     */
    @Transactional
    public void saveRule(HealthFollowRule rule) {
        if (rule.getId() == null) {
            rule.setId(IDGenerator.nextStr());
            rule.setCreateTime(System.currentTimeMillis());
        }
        rule.setUpdateTime(System.currentTimeMillis());
        HealthFollowRule existing = ruleMapper.selectByPrimaryKey(rule.getId());
        if (existing != null && existing.getId() != null) {
            ruleMapper.updateById(rule);
        } else {
            ruleMapper.insert(rule);
        }
    }

    /**
     * 删除规则
     */
    @Transactional
    public void deleteRule(String id) {
        ruleMapper.deleteByPrimaryKey(id);
    }

    /**
     * 启用规则
     */
    @Transactional
    public void enableRule(String id) {
        HealthFollowRule rule = ruleMapper.selectByPrimaryKey(id);
        if (rule != null) {
            rule.setEnabled(true);
            rule.setUpdateTime(System.currentTimeMillis());
            ruleMapper.updateById(rule);
        }
    }

    /**
     * 禁用规则
     */
    @Transactional
    public void disableRule(String id) {
        HealthFollowRule rule = ruleMapper.selectByPrimaryKey(id);
        if (rule != null) {
            rule.setEnabled(false);
            rule.setUpdateTime(System.currentTimeMillis());
            ruleMapper.updateById(rule);
        }
    }

    /**
     * 核心方法：档案新建/更新时调用，遍历所有enabled规则，
     * 根据关注检查项和最小异常数匹配，匹配则生成随访记录
     */
    @Transactional
    public void triggerRules(HealthArchive archive) {
        HealthFollowRule criteria = new HealthFollowRule();
        criteria.setEnabled(true);
        List<HealthFollowRule> rules = ruleMapper.select(criteria);
        if (rules == null || rules.isEmpty()) return;

        // 查询该档案的所有体检记录
        HealthExamination examCriteria = new HealthExamination();
        examCriteria.setCustomerId(archive.getId());
        List<HealthExamination> exams = examMapper.select(examCriteria);

        for (HealthFollowRule rule : rules) {
            boolean matched;
            if (StringUtils.isNotBlank(rule.getWatchExamItems())) {
                matched = evaluateByExamItems(rule, exams);
            } else if (StringUtils.isNotBlank(rule.getConditionExpr())) {
                matched = evaluateCondition(rule.getConditionExpr(), archive);
            } else {
                continue;
            }
            if (!matched) continue;

            HealthFollowRecord record = new HealthFollowRecord();
            record.setId(IDGenerator.nextStr());
            record.setCustomerId(archive.getId());
            record.setFollowDate(System.currentTimeMillis());
            record.setFollowType(
                StringUtils.isNotBlank(rule.getFollowMethod()) ? rule.getFollowMethod() : rule.getFollowType()
            );
            record.setFollowResult(buildFollowResult(rule, archive));
            int intervalDays = rule.getFollowInterval() != null ? rule.getFollowInterval()
                : (rule.getFollowIntervalDays() != null ? rule.getFollowIntervalDays() : 7);
            record.setNextFollowDate(System.currentTimeMillis() + (long) intervalDays * 24 * 60 * 60 * 1000);
            record.setCreateUser("system");
            record.setCreateTime(System.currentTimeMillis());
            record.setUpdateTime(System.currentTimeMillis());
            followMapper.insert(record);
        }
    }

    private boolean evaluateByExamItems(HealthFollowRule rule, List<HealthExamination> exams) {
        if (exams == null || exams.isEmpty()) return false;

        Set<String> watchItems = new HashSet<>();
        for (String item : rule.getWatchExamItems().split(",")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) watchItems.add(trimmed);
        }
        if (watchItems.isEmpty()) return false;

        long matchedCount = exams.stream()
            .filter(e -> Boolean.TRUE.equals(e.getIsAbnormal()))
            .filter(e -> e.getExamItem() != null && watchItems.stream()
                .anyMatch(w -> e.getExamItem().contains(w)))
            .count();

        int threshold = rule.getMinAbnormalCount() != null ? rule.getMinAbnormalCount() : 1;
        return matchedCount >= threshold;
    }

    /**
     * 评估条件表达式是否匹配档案
     * 支持格式：
     *   单条件: {"field":"bloodType","op":"eq","value":"O"}
     *   组合AND: {"and":[{"field":"age","op":"gte","value":50},{"field":"gender","op":"eq","value":"男"}]}
     *   组合OR: {"or":[{"field":"bloodType","op":"eq","value":"O"},{"field":"bloodType","op":"eq","value":"A"}]}
     */
    private boolean evaluateCondition(String conditionExpr, HealthArchive archive) {
        if (conditionExpr == null || conditionExpr.isEmpty()) {
            return true; // 无条件则始终匹配
        }
        try {
            JsonNode root = objectMapper.readTree(conditionExpr);
            return evaluateNode(root, archive);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean evaluateNode(JsonNode node, HealthArchive archive) {
        // 组合AND条件
        if (node.has("and")) {
            JsonNode andList = node.get("and");
            for (JsonNode item : andList) {
                if (!evaluateNode(item, archive)) {
                    return false;
                }
            }
            return true;
        }
        // 组合OR条件
        if (node.has("or")) {
            JsonNode orList = node.get("or");
            for (JsonNode item : orList) {
                if (evaluateNode(item, archive)) {
                    return true;
                }
            }
            return false;
        }
        // 单条件
        return evaluateSingleCondition(node, archive);
    }

    private boolean evaluateSingleCondition(JsonNode condition, HealthArchive archive) {
        if (!condition.has("field") || !condition.has("op")) {
            return false;
        }
        String field = condition.get("field").asText();
        String op = condition.get("op").asText();
        JsonNode valueNode = condition.get("value");

        Object archiveValue = getArchiveField(archive, field);
        if (archiveValue == null) {
            return false;
        }

        switch (op) {
            case "eq":   return equals(archiveValue, valueNode);
            case "ne":   return !equals(archiveValue, valueNode);
            case "gt":   return compare(archiveValue, valueNode) > 0;
            case "gte":  return compare(archiveValue, valueNode) >= 0;
            case "lt":   return compare(archiveValue, valueNode) < 0;
            case "lte":  return compare(archiveValue, valueNode) <= 0;
            case "between": return between(archiveValue, valueNode);
            case "contains": return contains(archiveValue, valueNode);
            default: return false;
        }
    }

    /**
     * 获取档案字段值，支持嵌套字段（如 diagnosis.name）
     */
    private Object getArchiveField(HealthArchive archive, String field) {
        // 支持直接从archive获取的字段
        switch (field) {
            case "bloodType": return archive.getBloodType();
            case "age": return archive.getAge();
            case "gender": return archive.getGender();
            case "height": return archive.getHeight();
            case "weight": return archive.getWeight();
            case "customerId": return archive.getCustomerId();
            case "customerName": return archive.getCustomerName();
            case "phone": return archive.getPhone();
            case "idcardNo": return archive.getIdcardNo();
            default: return null;
        }
    }

    private boolean equals(Object a, JsonNode b) {
        if (a == null || b == null) return false;
        String aStr = a.toString();
        String bStr = b.isNumber() ? b.asText() : b.asText();
        return aStr.equals(bStr);
    }

    private int compare(Object a, JsonNode b) {
        if (a == null || b == null) return -1;
        if (a instanceof Number && b.isNumber()) {
            return Double.compare(((Number) a).doubleValue(), b.asDouble());
        }
        try {
            double aNum = Double.parseDouble(a.toString());
            return Double.compare(aNum, b.asDouble());
        } catch (NumberFormatException e) {
            return a.toString().compareTo(b.asText());
        }
    }

    private boolean between(Object value, JsonNode rangeNode) {
        if (!rangeNode.isArray() || rangeNode.size() != 2) return false;
        try {
            double v = Double.parseDouble(value.toString());
            double low = rangeNode.get(0).asDouble();
            double high = rangeNode.get(1).asDouble();
            return v >= low && v <= high;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean contains(Object value, JsonNode searchNode) {
        if (value == null) return false;
        String search = searchNode.asText();
        return value.toString().contains(search);
    }

    /**
     * 评估所有启用规则，返回每个规则匹配的档案列表
     */
    public List<cn.cordys.crm.health.controller.HealthFollowRuleController.RuleMatchedArchive> evaluateAllRules() {
        List<cn.cordys.crm.health.controller.HealthFollowRuleController.RuleMatchedArchive> result = new ArrayList<>();

        HealthFollowRule ruleCriteria = new HealthFollowRule();
        ruleCriteria.setEnabled(true);
        List<HealthFollowRule> rules = ruleMapper.select(ruleCriteria);
        if (rules == null || rules.isEmpty()) return result;

        List<HealthArchive> archives = archiveMapper.select(new HealthArchive());
        if (archives == null || archives.isEmpty()) return result;

        List<HealthExamination> allExams = examMapper.select(new HealthExamination());
        if (allExams == null) allExams = List.of();

        Map<String, List<HealthExamination>> examsByCustomer = new HashMap<>();
        for (HealthExamination exam : allExams) {
            if (exam.getCustomerId() != null && !exam.getCustomerId().isBlank()) {
                examsByCustomer.computeIfAbsent(exam.getCustomerId(), k -> new ArrayList<>()).add(exam);
            }
        }

        for (HealthFollowRule rule : rules) {
            Set<String> watchItems = new HashSet<>();
            if (StringUtils.isNotBlank(rule.getWatchExamItems())) {
                for (String item : rule.getWatchExamItems().split(",")) {
                    String t = item.trim();
                    if (!t.isEmpty()) watchItems.add(t);
                }
            }
            int threshold = rule.getMinAbnormalCount() != null ? rule.getMinAbnormalCount() : 1;

            for (HealthArchive archive : archives) {
                if (archive.getId() == null) continue;

                List<HealthExamination> exams = new ArrayList<>();
                if (archive.getCustomerId() != null) {
                    List<HealthExamination> byCid = examsByCustomer.get(archive.getCustomerId());
                    if (byCid != null) exams.addAll(byCid);
                }
                List<HealthExamination> byAid = examsByCustomer.get(archive.getId());
                if (byAid != null) exams.addAll(byAid);

                if (exams.isEmpty()) continue;

                List<String> matchedItems = new ArrayList<>();
                for (HealthExamination exam : exams) {
                    if (exam.getExamItem() == null) continue;
                    if (watchItems.isEmpty() || watchItems.stream().anyMatch(w -> exam.getExamItem().contains(w))) {
                        matchedItems.add(exam.getExamItem());
                    }
                }

                if (matchedItems.size() < threshold) continue;

                cn.cordys.crm.health.controller.HealthFollowRuleController.RuleMatchedArchive item =
                    new cn.cordys.crm.health.controller.HealthFollowRuleController.RuleMatchedArchive();
                item.setRuleId(rule.getId());
                item.setRuleName(rule.getName());
                item.setFollowMethod(
                    StringUtils.isNotBlank(rule.getFollowMethod()) ? rule.getFollowMethod() : rule.getFollowType()
                );
                item.setFollowInterval(
                    rule.getFollowInterval() != null ? rule.getFollowInterval()
                        : (rule.getFollowIntervalDays() != null ? rule.getFollowIntervalDays() : 7)
                );
                item.setArchiveId(archive.getId());
                item.setArchiveNo(archive.getArchiveNo());
                item.setCustomerName(archive.getCustomerName());
                item.setPhone(archive.getPhone());
                item.setAbnormalCount(matchedItems.size());
                item.setMatchedExamItems(matchedItems);
                item.setSynced(exams.stream().anyMatch(e -> e.getExamNo() != null && e.getExamNo().startsWith("SYNC")));
                result.add(item);
            }
        }

        result.sort((a, b) -> Integer.compare(b.getAbnormalCount(), a.getAbnormalCount()));
        return result;
    }

    /**
     * 根据规则模板和档案构建随访结果
     */
    private String buildFollowResult(HealthFollowRule rule, HealthArchive archive) {
        String template = rule.getFollowResultTemplate();
        if (template == null || template.isEmpty()) {
            return "系统自动创建随访任务：" + rule.getName();
        }
        // 简单的变量替换
        return template
            .replace("{{customerName}}", archive.getCustomerName() != null ? archive.getCustomerName() : "")
            .replace("{{age}}", archive.getAge() != null ? archive.getAge().toString() : "")
            .replace("{{gender}}", archive.getGender() != null ? archive.getGender() : "")
            .replace("{{bloodType}}", archive.getBloodType() != null ? archive.getBloodType() : "");
    }
}