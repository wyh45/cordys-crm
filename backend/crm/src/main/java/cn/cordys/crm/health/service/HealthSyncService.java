package cn.cordys.crm.health.service;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthArchiveMapping;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * 体检数据同步服务
 * 从第三方体检接口拉取数据，存入健康档案表
 */
@Service
@Slf4j
public class HealthSyncService {

    @Value("${health.api.base-url:http://newreport.jkhaopy.com:9002}")
    private String apiBaseUrl;

    @Value("${health.api.app-id:166660309689670637991}")
    private String appId;

    @Value("${health.api.secret:}")
    private String secret;

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    @Resource
    private BaseMapper<HealthArchiveMapping> mappingMapper;

    @Resource
    private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    // 同步状态枚举（大写值）
    public enum SyncStatusEnum {
        PENDING("PENDING"),
        PROCESSING("PROCESSING"),
        COMPLETED("COMPLETED"),
        FAILED("FAILED");

        private final String value;
        SyncStatusEnum(String value) { this.value = value; }
        public String getValue() { return value; }
    }

    // 同步结果缓存（按 startDate 存储），支持多个日期范围并发同步
    private final java.util.concurrent.ConcurrentHashMap<String, SyncResult> syncResults = new java.util.concurrent.ConcurrentHashMap<>();

    /**
     * 获取指定同步任务的状态（按 startDate 查找）
     */
    public SyncResult getSyncStatusById(String startDate) {
        SyncResult cached = syncResults.get(startDate);
        if (cached != null) {
            return cached;
        }
        SyncResult pending = new SyncResult();
        pending.setSyncId(startDate);
        pending.setStartDate(startDate);
        pending.setStatus(SyncStatusEnum.PENDING.getValue());
        pending.setMessage("暂无同步记录");
        return pending;
    }

    /**
     * 同步体检数据
     * @param startDate 开始日期 yyyy-MM-dd
     * @param endDate 结束日期 yyyy-MM-dd
     * @return 同步结果
     */
    @Transactional(rollbackFor = Exception.class)
    public SyncResult syncByDateRange(String startDate, String endDate) {
        SyncResult result = new SyncResult();
        result.setSyncId(startDate);
        result.setStartDate(startDate);
        result.setEndDate(endDate);
        result.setStatus(SyncStatusEnum.PROCESSING.getValue());
        syncResults.put(result.getStartDate(), result); // 初始状态可被轮询

        try {
            // Step 1: 调用接口一，获取时间段内所有体检记录摘要
            List<HealthApiRecord> records = fetchExamRecords(startDate, endDate);
            if (records == null || records.isEmpty()) {
                result.setMessage("未查到体检记录");
                result.setStatus(SyncStatusEnum.COMPLETED.getValue());
                syncResults.put(result.getStartDate(), result);
                return result;
            }

            result.setTotalRecords(records.size());
            log.info("[HealthSync] Fetched {} records from API for range {} ~ {}", records.size(), startDate, endDate);

            // Step 2: 逐条处理每个体检号，查详情并存储
            int successCount = 0;
            int failCount = 0;
            for (HealthApiRecord record : records) {
                try {
                    SyncStatus status = processSingleRecord(record);
                    if (status == SyncStatus.SUCCESS) {
                        successCount++;
                    } else if (status == SyncStatus.FAILED) {
                        failCount++;
                    }
                    // 每处理一条稍微休息一下，避免请求过快
                    Thread.sleep(100);
                } catch (Exception e) {
                    log.error("[HealthSync] Failed to process record: {}", record.getWorkNo(), e);
                    failCount++;
                }
            }

            result.setSuccessCount(successCount);
            result.setFailCount(failCount);
            result.setMessage(String.format("成功 %d 条, 失败 %d 条", successCount, failCount));
            result.setStatus(SyncStatusEnum.COMPLETED.getValue());
            syncResults.put(result.getStartDate(), result);
            return result;

        } catch (Exception e) {
            log.error("[HealthSync] Sync failed", e);
            result.setMessage("同步失败: " + e.getMessage());
            result.setStatus(SyncStatusEnum.FAILED.getValue());
            syncResults.put(result.getStartDate(), result);
            return result;
        }
    }

    /**
     * 获取已同步的体检号列表（用于去重）
     */
    private Set<String> getSyncedExamNos() {
        Set<String> synced = new HashSet<>();
        HealthArchiveMapping example = new HealthArchiveMapping();
        List<HealthArchiveMapping> mappings = mappingMapper.select(example);
        for (HealthArchiveMapping m : mappings) {
            if (StringUtils.isNotBlank(m.getExamNo())) {
                synced.add(m.getExamNo());
            }
        }
        return synced;
    }

    /**
     * 处理单条体检记录（查详情 + 存储档案 + 存储检查项）
     */
    private SyncStatus processSingleRecord(HealthApiRecord record) {
        String examNo = record.getWorkNo();
        if (StringUtils.isBlank(examNo)) {
            return SyncStatus.SKIPPED;
        }

        // 去重检查
        Set<String> synced = getSyncedExamNos();
        if (synced.contains(examNo)) {
            log.debug("[HealthSync] ExamNo {} already synced, skip", examNo);
            return SyncStatus.SKIPPED;
        }

        // Step A: 创建或更新档案
        String archiveId = getOrCreateArchive(record);

        // Step B: 调用接口二，获取体检报告详情
        List<ExamDetailItem> details = fetchExamDetails(examNo);
        if (details == null || details.isEmpty()) {
            log.warn("[HealthSync] No details for examNo: {}", examNo);
        } else {
            // 存储检查项明细
            saveExamDetails(archiveId, record.getExamDateLong(), examNo, details);
        }

        // Step C: 创建映射记录
        createMapping(record, archiveId);

        return SyncStatus.SUCCESS;
    }

    /**
     * 根据身份证号获取或创建健康档案
     */
    private String getOrCreateArchive(HealthApiRecord record) {
        String idcardNo = record.getIdcardNo();
        if (StringUtils.isBlank(idcardNo)) {
            idcardNo = "UNKNOWN_" + record.getMobileNo();
        }

        // 查是否已存在档案（按身份证查）
        HealthArchive archive = new HealthArchive();
        archive.setIdcardNo(idcardNo);
        List<HealthArchive> existing = archiveMapper.select(archive);
        if (existing != null && !existing.isEmpty()) {
            // 更新档案基本信息
            HealthArchive a = existing.get(0);
            a.setCustomerName(record.getUserName());
            a.setPhone(record.getMobileNo());
            a.setGender("2".equals(record.getGenderId()) ? "女" : "男");
            if (record.getAge() != null) {
                try { a.setAge(Integer.parseInt(record.getAge())); } catch (Exception ignored) {}            }
            a.setUpdateTime(System.currentTimeMillis());
            archiveMapper.updateById(a);
            return a.getId();
        }

        // 新建档案
        HealthArchive newArchive = new HealthArchive();
        newArchive.setId(IDGenerator.nextStr());
        newArchive.setCustomerName(record.getUserName());
        newArchive.setPhone(record.getMobileNo());
        newArchive.setGender("2".equals(record.getGenderId()) ? "女" : "男");
        if (record.getAge() != null) {
            try { newArchive.setAge(Integer.parseInt(record.getAge())); } catch (Exception ignored) {}        }
        newArchive.setIdcardNo(idcardNo);
        newArchive.setCreateTime(System.currentTimeMillis());
        newArchive.setUpdateTime(System.currentTimeMillis());
        archiveMapper.insert(newArchive);
        return newArchive.getId();
    }

    /**
     * 保存体检检查项明细
     */
    private void saveExamDetails(String archiveId, Long examDate, String examNo, List<ExamDetailItem> details) {
        for (ExamDetailItem item : details) {
            HealthExamination exam = new HealthExamination();
            exam.setId(IDGenerator.nextStr());
            exam.setCustomerId(archiveId);
            exam.setExamNo(examNo);
            exam.setExamDate(examDate);
            exam.setExamItem(item.getCheckIndexName());
            exam.setResultValue(item.getResultValue());
            exam.setReferenceRange(item.getTextRef());
            exam.setIsAbnormal(item.isAbnormal());
            exam.setResultFlag(item.getResultFlag());
            exam.setCreateTime(System.currentTimeMillis());
            examMapper.insert(exam);
        }
    }

    /**
     * 创建档案-体检号映射记录
     */
    private void createMapping(HealthApiRecord record, String archiveId) {
        HealthArchiveMapping mapping = new HealthArchiveMapping();
        mapping.setId(IDGenerator.nextStr());
        mapping.setIdcardNo(StringUtils.isNotBlank(record.getIdcardNo()) ? record.getIdcardNo() : "UNKNOWN");
        mapping.setArchiveId(archiveId);
        mapping.setExamNo(record.getWorkNo());
        mapping.setExamDate(record.getExamDateLong());
        mapping.setSyncStatus(1); // success
        mapping.setCreateTime(System.currentTimeMillis());
        mapping.setUpdateTime(System.currentTimeMillis());
        mappingMapper.insert(mapping);
    }

    /**
     * 调用接口一：按时间段获取体检记录列表
     */
    @SuppressWarnings("unchecked")
    private List<HealthApiRecord> fetchExamRecords(String startDate, String endDate) {
        try {
            Map<String, String> signParams = new LinkedHashMap<>();
            signParams.put("appId", appId);
            signParams.put("startCheckDate", startDate);
            signParams.put("endCheckDate", endDate);
            String sign = generateSign(signParams, secret);

            String url = apiBaseUrl + "/health/physical/ai/get/record";

            Map<String, Object> params = new HashMap<>();
            params.put("appId", appId);
            params.put("startCheckDate", startDate);
            params.put("endCheckDate", endDate);
            params.put("sign", sign);

            String response = restTemplate.postForObject(url, params, String.class);
            if (response == null) {
                log.warn("[HealthSync] Empty response from API");
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response);
            int errno = root.path("errno").asInt(0);
            if (errno != 0) {
                log.warn("[HealthSync] API errno={}: {}", errno, root.path("errmsg").asText());
                return Collections.emptyList();
            }

            List<HealthApiRecord> records = new ArrayList<>();
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    HealthApiRecord record = new HealthApiRecord();
                    record.setUserName(item.path("userName").asText(""));
                    record.setMobileNo(item.path("mobileNo").asText(""));
                    record.setGenderId(item.path("genderId").asText(""));
                    record.setAge(item.path("age").asText(""));
                    record.setWorkNo(item.path("workNo").asText(""));
                    record.setProductName(item.path("productName").asText(""));
                    record.setIdcardNo(item.path("idcardNo").asText(""));
                    records.add(record);
                }
            }
            return records;
        } catch (Exception e) {
            log.error("[HealthSync] Failed to fetch exam records", e);
            return Collections.emptyList();
        }
    }

    /**
     * 调用接口二：获取体检报告详情
     */
    @SuppressWarnings("unchecked")
    private List<ExamDetailItem> fetchExamDetails(String examNo) {
        try {
            Map<String, String> signParams = new LinkedHashMap<>();
            signParams.put("appId", appId);
            signParams.put("workNo", examNo);
            String sign = generateSign(signParams, secret);

            String url = apiBaseUrl + "/health/physical/ai/report/new/detail";

            Map<String, Object> params = new HashMap<>();
            params.put("appId", appId);
            params.put("workNo", examNo);
            params.put("sign", sign);

            String response = restTemplate.postForObject(url, params, String.class);
            if (response == null) {
                return Collections.emptyList();
            }

            JsonNode root = objectMapper.readTree(response);
            int errno = root.path("errno").asInt(0);
            if (errno != 0) {
                return Collections.emptyList();
            }

            List<ExamDetailItem> items = new ArrayList<>();
            JsonNode data = root.path("data");
            if (data.isArray()) {
                for (JsonNode item : data) {
                    ExamDetailItem detailItem = new ExamDetailItem();
                    detailItem.setCheckIndexName(item.path("checkIndexName").asText(""));
                    detailItem.setResultValue(item.path("resultValue").asText(""));
                    detailItem.setTextRef(item.path("textRef").asText(""));
                    String flag = item.path("resultFlag").asText("-");
                    detailItem.setResultFlag(flag);
                    items.add(detailItem);
                }
            }
            return items;
        } catch (Exception e) {
            log.error("[HealthSync] Failed to fetch exam details for {}", examNo, e);
            return Collections.emptyList();
        }
    }

    /**
     * 生成签名：对参数按key升序排列，跳过空值，拼接k=v&k=v，最后加appSecret=xxx，MD5
     */
    private String generateSign(Map<String, String> params, String appSecret) {
        try {
            List<Map.Entry<String, String>> entries = new ArrayList<>(params.entrySet());
            entries.sort(Map.Entry.comparingByKey());

            StringBuilder sb = new StringBuilder();
            for (Map.Entry<String, String> entry : entries) {
                if (StringUtils.isNotBlank(entry.getValue())) {
                    sb.append(entry.getKey()).append("=").append(entry.getValue()).append("&");
                }
            }
            sb.append("appSecret=").append(appSecret.trim());

            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] bytes = md.digest(sb.toString().getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : bytes) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * 生成签名 MD5(appId + idCardNo + secret) - 旧版，保留兼容
     */
    @Deprecated
    private String generateSign(String appId, String idCardNo, String secret) {
        Map<String, String> params = new LinkedHashMap<>();
        params.put("appId", appId);
        if (StringUtils.isNotBlank(idCardNo)) {
            params.put("idCardNo", idCardNo);
        }
        return generateSign(params, secret);
    }

    // ====== 内部类 ======

    public static class SyncResult {
        private String syncId;       // 同步任务ID（= startDate）
        private String startDate;
        private String endDate;
        private String status;       // PENDING / PROCESSING / COMPLETED / FAILED
        private int totalRecords;
        private int successCount;
        private int failCount;
        private String message;

        public String getSyncId() { return syncId; }
        public void setSyncId(String syncId) { this.syncId = syncId; }
        public String getStartDate() { return startDate; }
        public void setStartDate(String startDate) { this.startDate = startDate; }
        public String getEndDate() { return endDate; }
        public void setEndDate(String endDate) { this.endDate = endDate; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public int getTotalRecords() { return totalRecords; }
        public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }
        public int getSuccessCount() { return successCount; }
        public void setSuccessCount(int successCount) { this.successCount = successCount; }
        public int getFailCount() { return failCount; }
        public void setFailCount(int failCount) { this.failCount = failCount; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }

    public static class ExamDetailItem {
        private String checkIndexName;
        private String resultValue;
        private String textRef;
        private String resultFlag;

        public boolean isAbnormal() {
            return "↑".equals(resultFlag) || "↓".equals(resultFlag);
        }

        public String getCheckIndexName() { return checkIndexName; }
        public void setCheckIndexName(String v) { this.checkIndexName = v; }
        public String getResultValue() { return resultValue; }
        public void setResultValue(String v) { this.resultValue = v; }
        public String getTextRef() { return textRef; }
        public void setTextRef(String v) { this.textRef = v; }
        public String getResultFlag() { return resultFlag; }
        public void setResultFlag(String v) { this.resultFlag = v; }
    }

    private enum SyncStatus {
        SUCCESS, FAILED, SKIPPED
    }

    /**
     * 脱敏手机号：保留前3后4，中间用****替代
     */
    private static String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) return "***";
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 脱敏身份证号：保留前3后4，中间用****替代
     */
    private static String maskIdcard(String idcard) {
        if (idcard == null || idcard.length() < 8) return "***";
        return idcard.substring(0, 3) + "****" + idcard.substring(idcard.length() - 4);
    }

    // DTO for API response
    @lombok.Data
    public static class HealthApiRecord {
        private String userName;
        private String mobileNo;
        private String genderId;
        private String age;
        private String workNo;
        private String productName;
        private String idcardNo;

        @Override
        public String toString() {
            return "HealthApiRecord{userName='" + userName + "', mobileNo='" + maskPhone(mobileNo) +
                "', genderId='" + genderId + "', age='" + age + "', workNo='" + workNo +
                "', productName='" + productName + "', idcardNo='" + maskIdcard(idcardNo) + "'}";
        }

        public Long getExamDateLong() {
            // 从 workNo 解析体检日期（格式: yyyyMMdd...）
            if (workNo != null && workNo.length() >= 8) {
                try {
                    String dateStr = workNo.substring(0, 8);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd");
                    return sdf.parse(dateStr).getTime();
                } catch (Exception e) {
                    return System.currentTimeMillis();
                }
            }
            return System.currentTimeMillis();
        }
    }
}
