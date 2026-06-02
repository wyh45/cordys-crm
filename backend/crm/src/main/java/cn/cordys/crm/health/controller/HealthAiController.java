package cn.cordys.crm.health.controller;

import cn.cordys.common.uid.IDGenerator;
import cn.cordys.crm.health.domain.HealthAiInterpretRecord;
import cn.cordys.crm.health.domain.HealthArchive;
import cn.cordys.crm.health.domain.HealthExamination;
import cn.cordys.crm.health.domain.HealthFollowRule;
import cn.cordys.crm.health.domain.HealthPushRecord;
import cn.cordys.crm.health.service.HealthAiService;
import cn.cordys.crm.health.service.HealthArchiveService;
import cn.cordys.crm.system.utils.SmsSender;
import cn.cordys.mybatis.BaseMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;

@Tag(name = "健康管理-AI智能解读")
@RestController
@RequestMapping("/health/ai")
@Slf4j
public class HealthAiController {

    @Resource
    private HealthAiService healthAiService;

    @Resource
    private HealthArchiveService archiveService;

    @Resource
    private BaseMapper<HealthArchive> archiveMapper;

    @Resource
    private BaseMapper<HealthExamination> examMapper;

    @Resource
    private BaseMapper<HealthFollowRule> ruleMapper;

    @Resource
    private BaseMapper<HealthPushRecord> pushRecordMapper;

    @Resource
    private SmsSender smsSender;

    @Value("${tencentcloud.sms.invite-template-id:}")
    private String inviteTemplateId;

    @PostMapping("/eligible-archives")
    @Operation(summary = "获取可AI解读的档案列表（有异常体检+随访规则匹配）")
    public Map<String, Object> getEligibleArchives(@RequestBody Map<String, Object> params) {
        int page = params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 20;

        // Find archives with exam records (all, not just abnormal)
        Set<String> abnormalArchiveIds = new HashSet<>();
        List<HealthExamination> allExams = examMapper.select(new HealthExamination());
        if (allExams != null) {
            for (HealthExamination exam : allExams) {
                if (exam.getCustomerId() != null) {
                    abnormalArchiveIds.add(exam.getCustomerId());
                }
            }
        }

        // Find archives matched by follow rules
        Set<String> ruleMatchedArchiveIds = new HashSet<>();
        List<HealthFollowRule> enabledRules = new ArrayList<>();
        List<HealthFollowRule> allRules = ruleMapper.select(new HealthFollowRule());
        if (allRules != null) {
            for (HealthFollowRule r : allRules) {
                if (Boolean.TRUE.equals(r.getEnabled())) enabledRules.add(r);
            }
        }
        // For each archive, check if any rule matches
        List<HealthArchive> allArchives = archiveMapper.select(new HealthArchive());
        if (allArchives != null && !enabledRules.isEmpty()) {
            for (HealthArchive archive : allArchives) {
                for (HealthFollowRule rule : enabledRules) {
                    boolean match = false;
                    if (rule.getWatchExamItems() != null && !rule.getWatchExamItems().isBlank()) {
                        // Check if archive has matching abnormal exams
                        Set<String> watchItems = new HashSet<>();
                        for (String item : rule.getWatchExamItems().split(",")) {
                            String t = item.trim();
                            if (!t.isEmpty()) watchItems.add(t);
                        }
                        if (allExams != null) {
                            long count = allExams.stream()
                                .filter(e -> archive.getId().equals(e.getCustomerId()))
                                .filter(e -> e.getExamItem() != null && watchItems.stream().anyMatch(w -> e.getExamItem().contains(w)))
                                .count();
                            int threshold = rule.getMinAbnormalCount() != null ? rule.getMinAbnormalCount() : 1;
                            if (count >= threshold) match = true;
                        }
                    }
                    if (match) {
                        ruleMatchedArchiveIds.add(archive.getId());
                        break;
                    }
                }
            }
        }

        // Merge and build result
        Set<String> eligibleIds = new HashSet<>();
        eligibleIds.addAll(abnormalArchiveIds);
        eligibleIds.addAll(ruleMatchedArchiveIds);

        List<Map<String, Object>> resultList = new ArrayList<>();
        if (allArchives != null) {
            for (HealthArchive a : allArchives) {
                if (!eligibleIds.contains(a.getId())) continue;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", a.getId());
                item.put("archiveNo", a.getArchiveNo());
                item.put("customerName", a.getCustomerName());
                item.put("phone", a.getPhone());
                item.put("gender", a.getGender());
                item.put("age", a.getAge());
                item.put("customerId", a.getCustomerId());
                boolean fromExam = abnormalArchiveIds.contains(a.getId());
                boolean fromRule = ruleMatchedArchiveIds.contains(a.getId());
                item.put("source", fromExam && fromRule ? "both" : fromExam ? "abnormal_exam" : "follow_rule");
                resultList.add(item);
            }
        }

        // Paginate
        int total = resultList.size();
        int start = (page - 1) * pageSize;
        int end = Math.min(start + pageSize, total);
        List<Map<String, Object>> pageList = start < total ? resultList.subList(start, end) : List.of();

        Map<String, Object> resp = new LinkedHashMap<>();
        resp.put("list", pageList);
        resp.put("total", total);
        return resp;
    }

    @PostMapping("/interpret")
    @Operation(summary = "AI健康解读（自动拉取档案的体检数据，支持多建议类型批量解读）")
    public Map<String, Object> interpret(@RequestBody Map<String, Object> params) {
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String reportType = params.get("reportType") != null ? params.get("reportType").toString() : "GENERAL";

        // select 支持数组或逗号分隔字符串
        List<String> suggestionTypes = new ArrayList<>();
        Object selectRaw = params.get("select");
        if (selectRaw instanceof List<?> list) {
            for (Object item : list) suggestionTypes.add(item.toString());
        } else if (selectRaw instanceof String s && !s.isBlank()) {
            for (String part : s.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) suggestionTypes.add(t);
            }
        }
        if (suggestionTypes.isEmpty()) suggestionTypes.add("检查建议");
        @SuppressWarnings("unchecked")
        Map<String, Object> reportData = new LinkedHashMap<>(params.get("reportData") != null
                ? (Map<String, Object>) params.get("reportData")
                : Map.of());

        // 自动拉取档案基本信息
        if (archiveId != null) {
            try {
                HealthArchive archive = archiveService.getArchive(archiveId);
                if (archive != null) {
                    reportData.putIfAbsent("customerName", archive.getCustomerName());
                    reportData.putIfAbsent("gender", archive.getGender());
                    reportData.putIfAbsent("age", archive.getAge() != null ? archive.getAge().toString() : null);
                    reportData.putIfAbsent("bloodType", archive.getBloodType());
                    reportData.putIfAbsent("height", archive.getHeight() != null ? archive.getHeight().toString() : null);
                    reportData.putIfAbsent("weight", archive.getWeight() != null ? archive.getWeight().toString() : null);
                    reportData.putIfAbsent("bloodPressure", archive.getBloodPressure());
                    reportData.putIfAbsent("heartRate", archive.getHeartRate() != null ? archive.getHeartRate().toString() : null);
                    // allergies/pastMedicalHistory/familyHistory are in separate service (HealthAllergy/HealthMedicalHistory)
                    // Skip for now - can be added via dedicated service calls if needed
                }

                // 拉取体检记录中的异常指标
                List<HealthExamination> exams = archiveService.getExaminations(archiveId);
                if (exams != null && !exams.isEmpty()) {
                    StringBuilder examResultBuilder = new StringBuilder();
                    for (HealthExamination exam : exams) {
                        if (exam.getExamItem() != null) {
                            examResultBuilder.append(exam.getExamItem())
                                    .append(": ").append(exam.getResultValue() != null ? exam.getResultValue() : "")
                                    .append(" (参考: ").append(exam.getReferenceRange() != null ? exam.getReferenceRange() : "无")
                                    .append(")");
                            if (exam.getIsAbnormal() != null && exam.getIsAbnormal()) {
                                examResultBuilder.append(" [异常]");
                            }
                            examResultBuilder.append("\n");
                        }
                    }
                    reportData.put("examResults", examResultBuilder.toString());
                }
            } catch (Exception e) {
                log.warn("[HealthAi] Failed to load archive/exam data for {}: {}", archiveId, e.getMessage());
            }
        }

        log.info("[HealthAi] interpret request - archiveId: {}, reportType: {}, types: {}, dataKeys: {}", archiveId, reportType, suggestionTypes, reportData.keySet());

        Map<String, String> results = healthAiService.generateHealthInterpretations(archiveId, reportType, suggestionTypes, reportData);

        // 自动保存每条解读记录
        String customerName = reportData.getOrDefault("customerName", "").toString();
        String customerId = reportData.getOrDefault("customerId", "").toString();
        for (Map.Entry<String, String> entry : results.entrySet()) {
            try {
                healthAiService.saveInterpretRecord(archiveId,
                    customerId != null && !customerId.isBlank() ? customerId : null,
                    customerName != null && !customerName.isBlank() ? customerName : null,
                    entry.getKey(), entry.getValue(), null, null);
            } catch (Exception e) {
                log.warn("[HealthAi] Failed to save record for {}: {}", entry.getKey(), e.getMessage());
            }
        }

        String patientPhone = null;
        if (archiveId != null) {
            try {
                HealthArchive archive = archiveService.getArchive(archiveId);
                if (archive != null) patientPhone = archive.getPhone();
            } catch (Exception ignored) {}
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("results", results);
        if (patientPhone != null) response.put("patientPhone", patientPhone);
        return response;
    }

    @PostMapping("/interpret-stream")
    @Operation(summary = "AI健康解读（SSE流式输出）")
    public SseEmitter interpretStream(@RequestBody Map<String, Object> params) {
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String reportType = params.get("reportType") != null ? params.get("reportType").toString() : "GENERAL";

        List<String> suggestionTypes = new ArrayList<>();
        Object selectRaw = params.get("select");
        if (selectRaw instanceof List<?> list) {
            for (Object item : list) suggestionTypes.add(item.toString());
        } else if (selectRaw instanceof String s && !s.isBlank()) {
            for (String part : s.split(",")) {
                String t = part.trim();
                if (!t.isEmpty()) suggestionTypes.add(t);
            }
        }
        if (suggestionTypes.isEmpty()) suggestionTypes.add("检查建议");

        @SuppressWarnings("unchecked")
        Map<String, Object> reportData = new LinkedHashMap<>(params.get("reportData") != null
                ? (Map<String, Object>) params.get("reportData")
                : Map.of());

        if (archiveId != null) {
            try {
                HealthArchive archive = archiveService.getArchive(archiveId);
                if (archive != null) {
                    reportData.putIfAbsent("customerName", archive.getCustomerName());
                    reportData.putIfAbsent("gender", archive.getGender());
                    reportData.putIfAbsent("age", archive.getAge() != null ? archive.getAge().toString() : null);
                    reportData.putIfAbsent("bloodType", archive.getBloodType());
                    reportData.putIfAbsent("height", archive.getHeight() != null ? archive.getHeight().toString() : null);
                    reportData.putIfAbsent("weight", archive.getWeight() != null ? archive.getWeight().toString() : null);
                    reportData.putIfAbsent("bloodPressure", archive.getBloodPressure());
                    reportData.putIfAbsent("heartRate", archive.getHeartRate() != null ? archive.getHeartRate().toString() : null);
                }
                List<HealthExamination> exams = archiveService.getExaminations(archiveId);
                if (exams != null && !exams.isEmpty()) {
                    StringBuilder examResultBuilder = new StringBuilder();
                    for (HealthExamination exam : exams) {
                        if (exam.getExamItem() != null) {
                            examResultBuilder.append(exam.getExamItem())
                                    .append(": ").append(exam.getResultValue() != null ? exam.getResultValue() : "")
                                    .append(" (参考: ").append(exam.getReferenceRange() != null ? exam.getReferenceRange() : "无")
                                    .append(")");
                            if (Boolean.TRUE.equals(exam.getIsAbnormal())) examResultBuilder.append(" [异常]");
                            examResultBuilder.append("\n");
                        }
                    }
                    reportData.put("examResults", examResultBuilder.toString());
                }
            } catch (Exception e) {
                log.warn("[HealthAi] Stream data load failed: {}", e.getMessage());
            }
        }

        SseEmitter emitter = new SseEmitter(120000L);
        healthAiService.streamHealthInterpretations(archiveId, reportType, suggestionTypes, reportData, emitter);
        return emitter;
    }

    @PostMapping("/assess-risk-stream")
    @Operation(summary = "健康风险评估（SSE流式输出）")
    public SseEmitter assessRiskStream(@RequestBody Map<String, Object> params) {
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String reportType = params.get("reportType") != null ? params.get("reportType").toString() : "GENERAL";

        @SuppressWarnings("unchecked")
        Map<String, Object> reportData = new LinkedHashMap<>(params.get("reportData") != null
                ? (Map<String, Object>) params.get("reportData")
                : Map.of());

        if (archiveId != null) {
            try {
                HealthArchive archive = archiveService.getArchive(archiveId);
                if (archive != null) {
                    reportData.putIfAbsent("customerName", archive.getCustomerName());
                    reportData.putIfAbsent("gender", archive.getGender());
                    reportData.putIfAbsent("age", archive.getAge() != null ? archive.getAge().toString() : null);
                    reportData.putIfAbsent("bloodType", archive.getBloodType());
                    reportData.putIfAbsent("height", archive.getHeight() != null ? archive.getHeight().toString() : null);
                    reportData.putIfAbsent("weight", archive.getWeight() != null ? archive.getWeight().toString() : null);
                    reportData.putIfAbsent("bloodPressure", archive.getBloodPressure());
                    reportData.putIfAbsent("heartRate", archive.getHeartRate() != null ? archive.getHeartRate().toString() : null);
                }
                List<HealthExamination> exams = archiveService.getExaminations(archiveId);
                if (exams != null && !exams.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (HealthExamination exam : exams) {
                        if (exam.getExamItem() != null) {
                            sb.append(exam.getExamItem())
                                    .append(": ").append(exam.getResultValue() != null ? exam.getResultValue() : "")
                                    .append(" (参考: ").append(exam.getReferenceRange() != null ? exam.getReferenceRange() : "无")
                                    .append(")");
                            if (Boolean.TRUE.equals(exam.getIsAbnormal())) sb.append(" [异常]");
                            sb.append("\n");
                        }
                    }
                    reportData.put("examResults", sb.toString());
                }
            } catch (Exception e) {
                log.warn("[HealthAi] Risk stream data load failed: {}", e.getMessage());
            }
        }

        SseEmitter emitter = new SseEmitter(180000L);
        healthAiService.streamAssessRisk(archiveId, reportType, reportData, emitter);
        return emitter;
    }

    @GetMapping("/last-interpretation/{archiveId}")
    @Operation(summary = "获取客户最近一次AI解读记录")
    public HealthAiInterpretRecord getLastInterpretation(@PathVariable String archiveId) {
        return healthAiService.getLastInterpretation(archiveId);
    }

    @PostMapping("/interpretation-history")
    @Operation(summary = "获取AI解读历史列表（分页）")
    public List<HealthAiInterpretRecord> getInterpretationHistory(@RequestBody Map<String, Object> params) {
        int pageNum = params.get("page") != null ? ((Number) params.get("page")).intValue() : 1;
        int pageSize = params.get("pageSize") != null ? ((Number) params.get("pageSize")).intValue() : 20;
        return healthAiService.getInterpretationHistory(pageNum, pageSize);
    }

    @PostMapping("/save-interpret-record")
    @Operation(summary = "保存AI解读+推送记录")
    public void saveInterpretRecord(@RequestBody Map<String, Object> params) {
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String customerId = params.get("customerId") != null ? params.get("customerId").toString() : null;
        String customerName = params.get("customerName") != null ? params.get("customerName").toString() : null;
        String suggestionType = params.get("suggestionType") != null ? params.get("suggestionType").toString() : null;
        String interpretation = params.get("interpretation") != null ? params.get("interpretation").toString() : null;
        String pushContent = params.get("pushContent") != null ? params.get("pushContent").toString() : null;
        String pushChannel = params.get("pushChannel") != null ? params.get("pushChannel").toString() : null;
        healthAiService.saveInterpretRecord(archiveId, customerId, customerName, suggestionType, interpretation, pushContent, pushChannel);
    }

    @PostMapping("/assess-risk")
    @Operation(summary = "健康风险评估（对接 Dify 风险评估 Chat App）")
    public Map<String, Object> assessRisk(@RequestBody Map<String, Object> params) {
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String reportType = params.get("reportType") != null ? params.get("reportType").toString() : "GENERAL";

        @SuppressWarnings("unchecked")
        Map<String, Object> reportData = new LinkedHashMap<>(params.get("reportData") != null
                ? (Map<String, Object>) params.get("reportData")
                : Map.of());

        if (archiveId != null) {
            try {
                HealthArchive archive = archiveService.getArchive(archiveId);
                if (archive != null) {
                    reportData.putIfAbsent("customerName", archive.getCustomerName());
                    reportData.putIfAbsent("gender", archive.getGender());
                    reportData.putIfAbsent("age", archive.getAge() != null ? archive.getAge().toString() : null);
                    reportData.putIfAbsent("bloodType", archive.getBloodType());
                    reportData.putIfAbsent("height", archive.getHeight() != null ? archive.getHeight().toString() : null);
                    reportData.putIfAbsent("weight", archive.getWeight() != null ? archive.getWeight().toString() : null);
                    reportData.putIfAbsent("bloodPressure", archive.getBloodPressure());
                    reportData.putIfAbsent("heartRate", archive.getHeartRate() != null ? archive.getHeartRate().toString() : null);
                }
                List<HealthExamination> exams = archiveService.getExaminations(archiveId);
                if (exams != null && !exams.isEmpty()) {
                    StringBuilder sb = new StringBuilder();
                    for (HealthExamination exam : exams) {
                        if (exam.getExamItem() != null) {
                            sb.append(exam.getExamItem())
                                    .append(": ").append(exam.getResultValue() != null ? exam.getResultValue() : "")
                                    .append(" (参考: ").append(exam.getReferenceRange() != null ? exam.getReferenceRange() : "无")
                                    .append(")");
                            if (Boolean.TRUE.equals(exam.getIsAbnormal())) sb.append(" [异常]");
                            sb.append("\n");
                        }
                    }
                    reportData.put("examResults", sb.toString());
                }
            } catch (Exception e) {
                log.warn("[HealthAi] Risk assessment data load failed: {}", e.getMessage());
            }
        }

        String result = healthAiService.assessRisk(archiveId, reportType, reportData);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("result", result);
        return response;
    }

    @PostMapping("/delete-interpret-record/{id}")
    @Operation(summary = "删除AI解读记录")
    public void deleteInterpretRecord(@PathVariable String id) {
        healthAiService.deleteInterpretRecord(id);
    }

    @PostMapping("/batch-interpretation-status")
    @Operation(summary = "批量查询档案已解读的建议类型")
    public Map<String, Set<String>> batchInterpretationStatus(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<String> archiveIds = (List<String>) params.get("archiveIds");
        return healthAiService.batchGetInterpretationStatus(archiveIds);
    }

    @PostMapping("/summarize-sms")
    @Operation(summary = "调用Dify精简短信文案")
    public Map<String, String> summarizeSms(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) params.get("records");
        String knowledge = params.get("knowledge") != null ? params.get("knowledge").toString() : null;
        String customerName = params.get("customerName") != null ? params.get("customerName").toString() : null;
        String gender = params.get("gender") != null ? params.get("gender").toString() : null;
        return healthAiService.summarizeSms(records, knowledge, customerName, gender);
    }

    @PostMapping("/summarize-sms-stream")
    @Operation(summary = "调用Dify精简短信文案（SSE流式输出）")
    public SseEmitter summarizeSmsStream(@RequestBody Map<String, Object> params) {
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> records = (List<Map<String, Object>>) params.get("records");
        String knowledge = params.get("knowledge") != null ? params.get("knowledge").toString() : null;
        String customerName = params.get("customerName") != null ? params.get("customerName").toString() : null;
        String gender = params.get("gender") != null ? params.get("gender").toString() : null;
        SseEmitter emitter = new SseEmitter(120000L);
        healthAiService.streamSummarizeSms(records, knowledge, customerName, gender, emitter);
        return emitter;
    }

    @PostMapping("/send-invite-sms")
    @Operation(summary = "发送短信邀约（腾讯云短信）")
    public Map<String, Object> sendInviteSms(@RequestBody Map<String, Object> params) {
        String phone = params.get("phone") != null ? params.get("phone").toString() : null;
        String greeting = params.get("greeting") != null ? params.get("greeting").toString() : "";
        String finding = params.get("finding") != null ? params.get("finding").toString() : "";
        String action = params.get("action") != null ? params.get("action").toString() : "";
        String archiveId = params.get("archiveId") != null ? params.get("archiveId").toString() : null;
        String customerName = params.get("customerName") != null ? params.get("customerName").toString() : null;

        if (phone == null || phone.isBlank()) return Map.of("success", false, "message", "手机号为空");
        if (!phone.startsWith("+")) {
            phone = phone.trim();
            if (phone.startsWith("0")) phone = phone.substring(1);
            if (!phone.startsWith("86")) phone = "86" + phone;
            phone = "+" + phone;
        }

        Map<String, Object> smsResult = smsSender.sendWithTemplate(phone, new String[]{greeting, finding, action}, inviteTemplateId);

        boolean smsOk = Boolean.TRUE.equals(smsResult.get("success"));
        if (smsOk) {
            HealthPushRecord record = new HealthPushRecord();
            record.setId(IDGenerator.nextStr());
            record.setCustomerId(archiveId);
            record.setPushChannel("SMS");
            record.setPushStatus("SENT");
            record.setPushTime(System.currentTimeMillis());
            record.setCreateUser("system");
            record.setCreateTime(System.currentTimeMillis());
            record.setUpdateTime(System.currentTimeMillis());
            pushRecordMapper.insert(record);
        }

        return smsResult;
    }
}
