package cn.cordys.crm.health.controller;

import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.mybatis.BaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 体检记录管理接口
 */
@Tag(name = "健康管理-体检记录")
@RestController
@RequestMapping("/health/examination")
public class HealthExaminationController {

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    /**
     * 异常指标分级统计
     * - 按检查项聚合：每个检查项的异常率
     * - 按客户聚合：每个客户的异常指标数量
     * - 支持日期范围筛选
     *
     * @param startDate 开始日期（时间戳，毫秒）
     * @param endDate   结束日期（时间戳，毫秒）
     * @param minRecords 最小体检记录数（过滤样本量太少的项）
     */
    @GetMapping("/abnormal/stat")
    @Operation(summary = "异常指标分级统计")
    public AbnormalStatResponse getAbnormalStat(
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate,
            @RequestParam(required = false, defaultValue = "10") Integer minRecords) {

        AbnormalStatResponse resp = new AbnormalStatResponse();

        // 统计项1：按检查项聚合（每个检查项的异常率）
        List<ExamItemStat> byItem = getAbnormalStatByItem(startDate, endDate, minRecords);
        resp.setByItem(byItem);

        // 统计项2：按客户聚合（每个客户的异常数）
        List<CustomerAbnormalStat> byCustomer = getAbnormalStatByCustomer(startDate, endDate);
        resp.setByCustomer(byCustomer);

        // 总体统计
        long totalExams = examMapper.countByExample(new HealthExamination());
        long abnormalExams = countAbnormalExams(startDate, endDate);
        resp.setTotalExamRecords(totalExams);
        resp.setAbnormalExamRecords(abnormalExams);
        resp.setAbnormalRate(totalExams > 0 ? Math.round((double) abnormalExams / totalExams * 10000) / 100.0 : 0.0);

        return resp;
    }

    /**
     * 按检查项聚合异常率
     */
    private List<ExamItemStat> getAbnormalStatByItem(Long startDate, Long endDate, int minRecords) {
        List<ExamItemStat> result = new ArrayList<>();

        // 查询所有体检项
        HealthExamination example = new HealthExamination();
        List<HealthExamination> allExams = examMapper.select(example);

        if (allExams == null || allExams.isEmpty()) {
            return result;
        }

        // 按 examItem 分组
        Map<String, List<HealthExamination>> byItem = new LinkedHashMap<>();
        for (HealthExamination exam : allExams) {
            // 日期过滤
            if (startDate != null && exam.getExamDate() != null && exam.getExamDate() < startDate) continue;
            if (endDate != null && exam.getExamDate() != null && exam.getExamDate() > endDate) continue;

            String item = exam.getExamItem();
            if (item == null || item.isBlank()) continue;
            byItem.computeIfAbsent(item, k -> new ArrayList<>()).add(exam);
        }

        // 计算每个检查项的异常率
        for (Map.Entry<String, List<HealthExamination>> entry : byItem.entrySet()) {
            List<HealthExamination> items = entry.getValue();
            if (items.size() < minRecords) continue;

            long abnormalCount = items.stream().filter(e -> Boolean.TRUE.equals(e.getIsAbnormal())).count();
            double rate = (double) abnormalCount / items.size();

            ExamItemStat stat = new ExamItemStat();
            stat.setExamItem(entry.getKey());
            stat.setTotalCount(items.size());
            stat.setAbnormalCount((int) abnormalCount);
            stat.setAbnormalRate(Math.round(rate * 10000) / 100.0); // 百分比，保留2位

            // 异常等级：0-5%正常，5-15%偏低/偏高，15-30%警示，>30%危险
            if (rate > 0.30) {
                stat.setLevel("DANGER");
            } else if (rate > 0.15) {
                stat.setLevel("WARNING");
            } else if (rate > 0.05) {
                stat.setLevel("CAUTION");
            } else {
                stat.setLevel("NORMAL");
            }

            result.add(stat);
        }

        // 按异常率降序
        result.sort((a, b) -> Double.compare(b.getAbnormalRate(), a.getAbnormalRate()));
        return result;
    }

    /**
     * 按客户聚合异常数
     */
    private List<CustomerAbnormalStat> getAbnormalStatByCustomer(Long startDate, Long endDate) {
        List<CustomerAbnormalStat> result = new ArrayList<>();

        HealthExamination example = new HealthExamination();
        List<HealthExamination> allExams = examMapper.select(example);
        if (allExams == null || allExams.isEmpty()) {
            return result;
        }

        // 按 customerId 分组
        Map<String, List<HealthExamination>> byCustomer = new LinkedHashMap<>();
        for (HealthExamination exam : allExams) {
            if (startDate != null && exam.getExamDate() != null && exam.getExamDate() < startDate) continue;
            if (endDate != null && exam.getExamDate() != null && exam.getExamDate() > endDate) continue;

            String cid = exam.getCustomerId();
            if (cid == null || cid.isBlank()) continue;
            byCustomer.computeIfAbsent(cid, k -> new ArrayList<>()).add(exam);
        }

        for (Map.Entry<String, List<HealthExamination>> entry : byCustomer.entrySet()) {
            List<HealthExamination> items = entry.getValue();
            long abnormalCount = items.stream().filter(e -> Boolean.TRUE.equals(e.getIsAbnormal())).count();

            CustomerAbnormalStat stat = new CustomerAbnormalStat();
            stat.setCustomerId(entry.getKey());
            stat.setTotalItems(items.size());
            stat.setAbnormalItems((int) abnormalCount);
            stat.setAbnormalRate(Math.round((double) abnormalCount / items.size() * 10000) / 100.0);

            result.add(stat);
        }

        // 按异常数降序，截取前50
        result.sort((a, b) -> Integer.compare(b.getAbnormalItems(), a.getAbnormalItems()));
        if (result.size() > 50) {
            result = result.subList(0, 50);
        }

        // 批量查询 HealthArchive，丰富客户姓名和手机号
        Set<String> customerIds = result.stream().map(CustomerAbnormalStat::getCustomerId).collect(Collectors.toSet());
        if (!customerIds.isEmpty()) {
            // select all archives and filter - same pattern used in existing services
            List<HealthArchive> allArchives = archiveMapper.select(new HealthArchive());
            if (allArchives != null) {
                Map<String, HealthArchive> archiveMap = new LinkedHashMap<>();
                for (HealthArchive a : allArchives) {
                    if (a.getCustomerId() != null && customerIds.contains(a.getCustomerId())) {
                        archiveMap.putIfAbsent(a.getCustomerId(), a);
                    }
                }
                for (CustomerAbnormalStat stat : result) {
                    HealthArchive archive = archiveMap.get(stat.getCustomerId());
                    if (archive != null) {
                        stat.setCustomerName(archive.getCustomerName());
                        stat.setPhone(archive.getPhone());
                        stat.setArchiveId(archive.getId());
                    }
                }
            }
        }
        return result;
    }

    private long countAbnormalExams(Long startDate, Long endDate) {
        // 统计有异常记录的独立客户数（去重）
        HealthExamination example = new HealthExamination();
        List<HealthExamination> all = examMapper.select(example);
        if (all == null) return 0;

        Set<String> abnormalCustomers = new HashSet<>();
        for (HealthExamination exam : all) {
            if (startDate != null && exam.getExamDate() != null && exam.getExamDate() < startDate) continue;
            if (endDate != null && exam.getExamDate() != null && exam.getExamDate() > endDate) continue;
            if (Boolean.TRUE.equals(exam.getIsAbnormal()) && exam.getCustomerId() != null) {
                abnormalCustomers.add(exam.getCustomerId());
            }
        }
        return abnormalCustomers.size();
    }

    @GetMapping("/abnormal/customers")
    @Operation(summary = "获取异常指标客户列表（含档案信息）")
    public List<CustomerAbnormalDetail> getAbnormalCustomers(
            @RequestParam(required = false) Long startDate,
            @RequestParam(required = false) Long endDate,
            @RequestParam(required = false, defaultValue = "abnormalItems") String sortBy,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {

        List<CustomerAbnormalDetail> result = new ArrayList<>();
        HealthExamination example = new HealthExamination();
        List<HealthExamination> allExams = examMapper.select(example);
        if (allExams == null || allExams.isEmpty()) return result;

        Map<String, List<HealthExamination>> byCustomer = new LinkedHashMap<>();
        for (HealthExamination exam : allExams) {
            if (startDate != null && exam.getExamDate() != null && exam.getExamDate() < startDate) continue;
            if (endDate != null && exam.getExamDate() != null && exam.getExamDate() > endDate) continue;
            String cid = exam.getCustomerId();
            if (cid == null || cid.isBlank()) continue;
            byCustomer.computeIfAbsent(cid, k -> new ArrayList<>()).add(exam);
        }

        List<HealthArchive> allArchives = archiveMapper.select(new HealthArchive());
        Map<String, HealthArchive> archiveByCustomerId = new LinkedHashMap<>();
        if (allArchives != null) {
            for (HealthArchive a : allArchives) {
                if (a.getCustomerId() != null) archiveByCustomerId.putIfAbsent(a.getCustomerId(), a);
                if (a.getId() != null) archiveByCustomerId.putIfAbsent(a.getId(), a);
            }
        }

        for (Map.Entry<String, List<HealthExamination>> entry : byCustomer.entrySet()) {
            List<HealthExamination> items = entry.getValue();
            HealthArchive archive = archiveByCustomerId.get(entry.getKey());
            CustomerAbnormalDetail detail = new CustomerAbnormalDetail();
            detail.setCustomerId(entry.getKey());
            if (archive != null) {
                detail.setArchiveId(archive.getId());
                detail.setArchiveNo(archive.getArchiveNo());
                detail.setCustomerName(archive.getCustomerName());
                detail.setPhone(archive.getPhone());
            }
            detail.setAbnormalItems(items.size());
            detail.setAbnormalItemNames(items.stream()
                .map(HealthExamination::getExamItem)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList()));
            HealthExamination first = items.get(0);
            detail.setExamDate(first.getExamDate());
            detail.setSyncTime(first.getCreateTime());
            result.add(detail);
        }
        Set<String> syncedIds = new HashSet<>();
        Map<String, Integer> syncedCountMap = new HashMap<>();
        for (HealthExamination exam : allExams) {
            if (exam.getExamNo() != null && exam.getExamNo().startsWith("SYNC") && exam.getCustomerId() != null) {
                syncedIds.add(exam.getCustomerId());
                syncedCountMap.merge(exam.getCustomerId(), 1, Integer::sum);
            }
        }
        for (CustomerAbnormalDetail detail : result) {
            detail.setSynced(syncedIds.contains(detail.getCustomerId()));
            detail.setSyncedCount(syncedCountMap.getOrDefault(detail.getCustomerId(), 0));
        }

        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        switch (sortBy) {
            case "syncTime":
                result.sort((a, b) -> asc
                    ? Long.compare(a.getSyncTime() != null ? a.getSyncTime() : 0, b.getSyncTime() != null ? b.getSyncTime() : 0)
                    : Long.compare(b.getSyncTime() != null ? b.getSyncTime() : 0, a.getSyncTime() != null ? a.getSyncTime() : 0));
                break;
            case "examDate":
                result.sort((a, b) -> asc
                    ? Long.compare(a.getExamDate() != null ? a.getExamDate() : 0, b.getExamDate() != null ? b.getExamDate() : 0)
                    : Long.compare(b.getExamDate() != null ? b.getExamDate() : 0, a.getExamDate() != null ? a.getExamDate() : 0));
                break;
            default:
                result.sort((a, b) -> Integer.compare(b.getAbnormalItems(), a.getAbnormalItems()));
                break;
        }
        if (result.size() > 100) result = result.subList(0, 100);
        return result;
    }

    @lombok.Data
    public static class CustomerAbnormalDetail {
        private String customerId;
        private String archiveId;
        private String archiveNo;
        private String customerName;
        private String phone;
        private int abnormalItems;
        private List<String> abnormalItemNames;
        private boolean synced;
        private int syncedCount;
        private Long examDate;
        private Long syncTime;
    }

    // ====== Response DTOs ======

    @lombok.Data
    public static class AbnormalStatResponse {
        private List<ExamItemStat> byItem;
        private List<CustomerAbnormalStat> byCustomer;
        private long totalExamRecords;
        private long abnormalExamRecords;
        private double abnormalRate; // 百分比
    }

    @lombok.Data
    public static class ExamItemStat {
        private String examItem;
        private int totalCount;
        private int abnormalCount;
        private double abnormalRate; // 百分比
        private String level; // NORMAL / CAUTION / WARNING / DANGER
    }

    @lombok.Data
    public static class CustomerAbnormalStat {
        private String customerId;
        private String customerName;
        private String phone;
        private String archiveId;
        private int totalItems;
        private int abnormalItems;
        private double abnormalRate;
    }

    /**
     * 分页查询所有体检客户列表（含档案信息和检查项目数）
     */
    @PostMapping("/page")
    @Operation(summary = "体检客户分页列表")
    public Map<String, Object> page(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {

        List<HealthExamination> allExams = examMapper.select(new HealthExamination());
        Map<String, List<HealthExamination>> byCustomer = new LinkedHashMap<>();
        for (HealthExamination exam : allExams) {
            String cid = exam.getCustomerId();
            if (cid == null || cid.isBlank()) continue;
            byCustomer.computeIfAbsent(cid, k -> new ArrayList<>()).add(exam);
        }

        List<HealthArchive> allArchives = archiveMapper.select(new HealthArchive());
        Map<String, HealthArchive> archiveById = new LinkedHashMap<>();
        for (HealthArchive a : allArchives) {
            if (a.getId() != null) archiveById.putIfAbsent(a.getId(), a);
        }

        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, List<HealthExamination>> entry : byCustomer.entrySet()) {
            HealthArchive archive = archiveById.get(entry.getKey());
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("archiveId", entry.getKey());
            item.put("customerName", archive != null ? archive.getCustomerName() : "");
            item.put("phone", archive != null ? archive.getPhone() : "");
            item.put("archiveNo", archive != null ? archive.getArchiveNo() : "");
            item.put("gender", archive != null ? archive.getGender() : "");
            item.put("age", archive != null ? archive.getAge() : null);
            item.put("examCount", entry.getValue().size());
            item.put("examItems", entry.getValue().stream().map(HealthExamination::getExamItem).distinct().toList());
            list.add(item);
        }

        int total = list.size();
        int from = (page - 1) * size;
        int to = Math.min(from + size, total);
        List<Map<String, Object>> paged = from < total ? list.subList(from, to) : List.of();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("list", paged);
        return result;
    }

}