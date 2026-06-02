package cn.cordys.crm.health.service;

import cn.cordys.crm.health.domain.HealthAiInterpretRecord;
import cn.cordys.common.uid.IDGenerator;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RequestCallback;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@Slf4j
public class HealthAiService {

    @Value("${dify.api.url:http://220.163.111.166/v1}")
    private String difyApiUrl;

    @Value("${dify.api.key:}")
    private String difyApiKey;

    @Value("${dify.risk.api.key:}")
    private String difyRiskApiKey;

    @Value("${dify.sms.api.key:}")
    private String difySmsApiKey;

    @Value("${dify.api.type:chat}")
    private String difyApiType;

    @Value("${dify.api.timeout:120000}")
    private long timeoutMs;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Resource
    private BaseMapper<HealthAiInterpretRecord> interpretRecordMapper;

    public HealthAiService() {
        this.restTemplate = new RestTemplate();
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(30000);
        factory.setReadTimeout(120000);
        this.restTemplate.setRequestFactory(factory);
        this.objectMapper = new ObjectMapper();
    }

    /**
     * 将中文建议类型映射为 Dify 工作流条件分支所需的 select 值
     * 1=检查建议, 2=生活方式建议, 3=饮食建议, 其余=运动建议
     */
    private String mapSelectValue(String suggestionType) {
        if (suggestionType == null) return "4";
        return switch (suggestionType) {
            case "检查建议" -> "1";
            case "生活方式建议" -> "2";
            case "饮食建议" -> "3";
            default -> "4";
        };
    }

    /**
     * 批量生成健康建议（对接 Dify）
     * 对每个建议类型分别调用 Dify，返回类型→结果的映射
     *
     * @param archiveId       体检档案ID
     * @param reportType      报告类型
     * @param suggestionTypes 建议类型列表: 检查建议/生活方式建议/饮食建议/运动建议
     * @param reportData      体检报告数据
     */
    public Map<String, String> generateHealthInterpretations(String archiveId, String reportType,
                                                              List<String> suggestionTypes, Map<String, Object> reportData) {
        Map<String, String> results = new LinkedHashMap<>();
        if (difyApiKey == null || difyApiKey.isBlank()) {
            String err = "AI 服务未配置，请联系管理员设置 Dify API Key。";
            for (String t : suggestionTypes) results.put(t, err);
            return results;
        }

        String highRiskDiseases = extractHighRiskDiseases(reportData);
        String customerName = reportData.getOrDefault("customerName", "").toString();
        String gender = reportData.getOrDefault("gender", "").toString();
        String age = reportData.getOrDefault("age", "").toString();
        String summary = String.format("%s %s %s岁 %s", customerName, gender, age, reportType);

        boolean isWorkflow = "workflow".equalsIgnoreCase(difyApiType);
        String baseUrl = difyApiUrl.endsWith("/") ? difyApiUrl.substring(0, difyApiUrl.length() - 1) : difyApiUrl;
        String url = baseUrl + (isWorkflow ? "/workflows/run" : "/chat-messages");

        for (String suggestionType : suggestionTypes) {
            try {
                log.info("[HealthAi] archiveId={}, type={}", archiveId, suggestionType);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + difyApiKey);

                String typedQuery = buildHealthQuery(reportType, reportData);
                Map<String, Object> inputs = buildInputs(reportType, reportData);
                inputs.put("query", typedQuery);
                inputs.put("input", summary.length() > 200 ? summary.substring(0, 200) : summary);
                inputs.put("1746601345789.input", highRiskDiseases);
                inputs.put("select", mapSelectValue(suggestionType));

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("inputs", inputs);
                body.put("response_mode", "blocking");
                body.put("user", archiveId != null ? archiveId : "anonymous");
                if (!isWorkflow) {
                    body.put("query", typedQuery);
                    body.put("conversation_id", "");
                }

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

                if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                    log.info("[HealthAi] {} response received, length={}", suggestionType, response.getBody().length());
                    results.put(suggestionType, parseDifyResponse(response.getBody()));
                } else {
                    results.put(suggestionType, "AI 服务暂时无法响应，请稍后再试。");
                }
            } catch (Exception e) {
                log.error("[HealthAi] {} call failed: {}", suggestionType, e.getMessage());
                results.put(suggestionType, "AI 解读失败: " + e.getMessage());
            }
        }
        return results;
    }

    /**
     * 健康风险评估（对接 Dify 风险评估 Chat App）
     */
    public String assessRisk(String archiveId, String reportType, Map<String, Object> reportData) {
        if (difyRiskApiKey == null || difyRiskApiKey.isBlank()) {
            return "{\"assessments\":[],\"message\":\"风险评估服务未配置\"}";
        }

        String baseUrl = difyApiUrl.endsWith("/") ? difyApiUrl.substring(0, difyApiUrl.length() - 1) : difyApiUrl;
        String url = baseUrl + "/chat-messages";

        String query = buildHealthQuery(reportType, reportData);
        String customerName = reportData.getOrDefault("customerName", "").toString();
        String gender = reportData.getOrDefault("gender", "").toString();
        String age = reportData.getOrDefault("age", "").toString();
        String summary = String.format("%s %s %s岁 %s", customerName, gender, age, reportType);

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + difyRiskApiKey);

            Map<String, Object> inputs = buildInputs(reportType, reportData);
            inputs.put("input", summary.length() > 200 ? summary.substring(0, 200) : summary);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("inputs", inputs);
            body.put("response_mode", "blocking");
            body.put("user", archiveId != null ? archiveId : "anonymous");
            body.put("query", query);
            body.put("conversation_id", "");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                log.info("[HealthAi] Risk assessment response received, length={}", response.getBody().length());
                String parsed = parseDifyResponse(response.getBody());
                saveInterpretRecord(archiveId,
                    reportData.getOrDefault("customerId", "").toString(),
                    customerName, "风险评估", parsed, null, null);
                return parsed;
            }
            return "{\"assessments\":[],\"message\":\"AI 服务暂时无法响应\"}";
        } catch (Exception e) {
            log.error("[HealthAi] Risk assessment failed: {}", e.getMessage());
            return "{\"assessments\":[],\"message\":\"风险评估失败: " + e.getMessage() + "\"}";
        }
    }

    /**
     * 健康风险评估（SSE 流式输出）
     */
    public void streamAssessRisk(String archiveId, String reportType, Map<String, Object> reportData, SseEmitter emitter) {
        if (difyRiskApiKey == null || difyRiskApiKey.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("风险评估服务未配置"));
                emitter.complete();
            } catch (Exception ignored) {}
            return;
        }

        String baseUrl = difyApiUrl.endsWith("/") ? difyApiUrl.substring(0, difyApiUrl.length() - 1) : difyApiUrl;
        String url = baseUrl + "/chat-messages";
        String query = buildHealthQuery(reportType, reportData);
        String customerName = reportData.getOrDefault("customerName", "").toString();
        String gender = reportData.getOrDefault("gender", "").toString();
        String age = reportData.getOrDefault("age", "").toString();
        String summary = String.format("%s %s %s岁 %s", customerName, gender, age, reportType);

        new Thread(() -> {
            try {
                Map<String, Object> inputs = buildInputs(reportType, reportData);
                inputs.put("input", summary.length() > 200 ? summary.substring(0, 200) : summary);

                Map<String, Object> body = new LinkedHashMap<>();
                body.put("inputs", inputs);
                body.put("response_mode", "streaming");
                body.put("user", archiveId != null ? archiveId : "anonymous");
                body.put("query", query);
                body.put("conversation_id", "");

                String requestBody = objectMapper.writeValueAsString(body);
                String result = streamDifyResponse(url, requestBody, "risk", emitter, difyRiskApiKey);

                String stripped = result;
                if (stripped.startsWith("```json")) {
                    stripped = stripped.replaceAll("```json\\s*", "").replaceAll("```\\s*", "").trim();
                }
                try {
                    Object cid = reportData.get("customerId");
                    saveInterpretRecord(archiveId,
                        cid != null ? cid.toString() : "",
                        customerName, "风险评估", stripped, null, null);
                } catch (Exception e) {
                    log.warn("[HealthAi] Failed to save risk record: {}", e.getMessage());
                }

                emitter.send(SseEmitter.event().name("done").data("all_complete"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    public void streamHealthInterpretations(String archiveId, String reportType,
                                            List<String> suggestionTypes, Map<String, Object> reportData,
                                            SseEmitter emitter) {
        if (difyApiKey == null || difyApiKey.isBlank()) {
            try {
                emitter.send(SseEmitter.event().name("error").data("AI 服务未配置"));
                emitter.complete();
            } catch (Exception ignored) {}
            return;
        }

        String highRiskDiseases = extractHighRiskDiseases(reportData);
        String customerName = reportData.getOrDefault("customerName", "").toString();
        String gender = reportData.getOrDefault("gender", "").toString();
        String age = reportData.getOrDefault("age", "").toString();
        String summary = String.format("%s %s %s岁 %s", customerName, gender, age, reportType);

        boolean isWorkflow = "workflow".equalsIgnoreCase(difyApiType);
        String baseUrl = difyApiUrl.endsWith("/") ? difyApiUrl.substring(0, difyApiUrl.length() - 1) : difyApiUrl;
        String url = baseUrl + (isWorkflow ? "/workflows/run" : "/chat-messages");

        String customerId = reportData.getOrDefault("customerId", "").toString();
        new Thread(() -> {
            try {
                for (String suggestionType : suggestionTypes) {
                    String typedQuery = buildHealthQuery(reportType, reportData);
                    String requestBody = buildStreamRequestBody(archiveId, reportType, reportData, suggestionType,
                        typedQuery, highRiskDiseases, summary, isWorkflow);
                    String fullText = streamDifyResponse(url, requestBody, suggestionType, emitter, difyApiKey);
                    try {
                        saveInterpretRecord(archiveId,
                            customerId != null && !customerId.isBlank() ? customerId : null,
                            customerName != null && !customerName.isBlank() ? customerName : null,
                            suggestionType, fullText, null, null);
                    } catch (Exception e) {
                        log.warn("[HealthAi] Failed to save stream record for {}: {}", suggestionType, e.getMessage());
                    }
                }
                emitter.send(SseEmitter.event().name("done").data("all_complete"));
                emitter.complete();
            } catch (Exception e) {
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private String buildStreamRequestBody(String archiveId, String reportType, Map<String, Object> reportData,
                                          String suggestionType, String query, String highRiskDiseases,
                                          String summary, boolean isWorkflow) {
        Map<String, Object> inputs = buildInputs(reportType, reportData);
        inputs.put("query", query);
        inputs.put("input", summary.length() > 200 ? summary.substring(0, 200) : summary);
        inputs.put("1746601345789.input", highRiskDiseases);
        inputs.put("select", mapSelectValue(suggestionType));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "streaming");
        body.put("user", archiveId != null ? archiveId : "anonymous");
        if (!isWorkflow) {
            body.put("query", query);
            body.put("conversation_id", "");
        }
        try {
            return objectMapper.writeValueAsString(body);
        } catch (Exception e) {
            return "{}";
        }
    }

    private String streamDifyResponse(String url, String requestBody, String type, SseEmitter emitter, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(30000);
            factory.setReadTimeout(120000);
            factory.setBufferRequestBody(false);

            RestTemplate streamTemplate = new RestTemplate(factory);
            RequestCallback requestCallback = req -> {
                req.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                req.getHeaders().set("Authorization", "Bearer " + apiKey);
                req.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
            };

            StringBuilder fullText = new StringBuilder();
            streamTemplate.execute(url, HttpMethod.POST, requestCallback, response -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.startsWith("data: ")) {
                            String json = line.substring(6).trim();
                            try {
                                JsonNode node = objectMapper.readTree(json);
                                String event = node.has("event") ? node.get("event").asText() : "";
                                String answer = node.has("answer") ? node.get("answer").asText() : "";
                                if (!answer.isEmpty()) {
                                    String cleaned = stripThink(answer);
                                    fullText.append(cleaned);
                                    emitter.send(SseEmitter.event().name(type).data(cleaned));
                                }
                                if ("message_end".equals(event) || "workflow_finished".equals(event)) {
                                    emitter.send(SseEmitter.event().name(type + "_end").data(fullText.toString()));
                                }
                            } catch (Exception parseErr) {
                                // skip malformed SSE lines
                            }
                        }
                    }
                }
                return null;
            });
            return fullText.toString();
        } catch (Exception e) {
            log.error("[HealthAi] Stream {} failed: {}", type, e.getMessage());
            try {
                emitter.send(SseEmitter.event().name(type + "_error").data(e.getMessage()));
            } catch (Exception ignored) {}
            return "";
        }
    }

    /**
     * 从体检数据中提取高风险疾病名称
     * 优先用异常体检项 examResults 中的 [异常] 标注项，其次用 pastMedicalHistory
     */
    private String extractHighRiskDiseases(Map<String, Object> reportData) {
        StringBuilder diseases = new StringBuilder();

        Object examResults = reportData.get("examResults");
        if (examResults != null && !examResults.toString().isBlank()) {
            String[] lines = examResults.toString().split("\n");
            for (String line : lines) {
                if (line.contains("[异常]")) {
                    String itemName = line.split(":")[0].trim();
                    if (!itemName.isEmpty()) {
                        if (diseases.length() > 0) diseases.append("、");
                        diseases.append(itemName);
                    }
                }
            }
        }

        Object pastHistory = reportData.get("pastMedicalHistory");
        if (pastHistory != null && !pastHistory.toString().isBlank()) {
            if (diseases.length() > 0) diseases.append("、");
            diseases.append(pastHistory.toString());
        }

        return diseases.length() > 0 ? diseases.toString() : "无明显高风险疾病";
    }

    /**
     * 构建发送给 Dify 的 inputs 字段
     * Dify input 表单字段限制 256 字符，仅放入简短摘要
     * 详细数据通过 query 传递
     */
    private Map<String, Object> buildInputs(String reportType, Map<String, Object> reportData) {
        Map<String, Object> inputs = new LinkedHashMap<>();
        inputs.put("report_type", reportType);
        reportData.forEach((k, v) -> {
            if (v != null && !v.toString().isBlank()) {
                inputs.put(k, v);
            }
        });
        return inputs;
    }

    /**
     * 将英文字段名映射为中文描述
     */
    private String mapFieldName(String key) {
        return switch (key) {
            case "bloodType" -> "血型";
            case "height" -> "身高(cm)";
            case "weight" -> "体重(kg)";
            case "bloodPressure" -> "血压(mmHg)";
            case "heartRate" -> "心率(bpm)";
            case "allergies" -> "过敏史";
            case "pastMedicalHistory" -> "既往病史";
            case "familyHistory" -> "家族病史";
            case "bloodGlucose" -> "血糖(mmol/L)";
            case "totalCholesterol" -> "总胆固醇(mmol/L)";
            case "triglyceride" -> "甘油三酯(mmol/L)";
            case "hdl" -> "高密度脂蛋白(mmol/L)";
            case "ldl" -> "低密度脂蛋白(mmol/L)";
            case "alt" -> "谷丙转氨酶(U/L)";
            case "ast" -> "谷草转氨酶(U/L)";
            case "examResults" -> "检查结果";
            case "remark" -> "备注";
            case "customerName" -> "姓名";
            case "gender" -> "性别";
            case "age" -> "年龄";
            default -> key;
        };
    }

    /**
     * 格式化指标值（带参考值标注）
     */
    private String formatValue(String key, String value) {
        return switch (key) {
            case "bloodGlucose" -> value + "（参考值: 3.9-6.1 mmol/L）";
            case "totalCholesterol" -> value + "（参考值: 2.8-5.2 mmol/L）";
            case "triglyceride" -> value + "（参考值: 0.56-1.7 mmol/L）";
            case "hdl" -> value + "（参考值: 男>1.0, 女>1.3 mmol/L）";
            case "ldl" -> value + "（参考值: <3.4 mmol/L）";
            case "alt" -> value + "（参考值: 男<50, 女<40 U/L）";
            case "ast" -> value + "（参考值: 男<40, 女<35 U/L）";
            case "bloodPressure" -> value + "（参考值: 收缩压90-140, 舒张压60-90 mmHg）";
            case "heartRate" -> value + "（参考值: 60-100 bpm）";
            default -> value;
        };
    }

    /**
     * 构建自然语言查询，携带完整体检指标数据
     * Dify inputs 有 256 字符限制，详细数据通过 query 传递
     */
    private String buildHealthQuery(String reportType, Map<String, Object> reportData) {
        StringBuilder query = new StringBuilder();
        query.append("请根据以下体检指标数据进行健康风险评估。\n\n");
        query.append("报告类型：").append(reportType).append("\n");
        query.append("体检指标数据：\n");

        List<String> lines = new ArrayList<>();
        reportData.forEach((k, v) -> {
            if (v != null && !v.toString().isBlank() && !"customerName".equals(k) && !"gender".equals(k) && !"age".equals(k)) {
                String key = mapFieldName(k);
                lines.add(key + "：" + formatValue(k, v.toString()));
            }
        });

        if (!lines.isEmpty()) {
            query.append(String.join("\n", lines));
        } else {
            query.append("(无具体指标数据，请基于报告类型给出通用风险评估)");
        }

        String result = query.toString();
        log.info("[HealthAi] Risk query FULL: {}", result);
        return result;
    }

    private String stripThink(String text) {
        if (text == null) return null;
        return text.replaceAll("(?s)<think>.*?</think>", "").replaceAll("</?think>", "").trim();
    }

    /**
     * 解析 Dify API 响应
     */
    private String parseDifyResponse(String body) {
        try {
            String firstLine = body.split("\n")[0].trim();
            if (firstLine.startsWith("{")) {
                JsonNode firstNode = objectMapper.readTree(firstLine);
                if (firstNode.has("answer") && !firstNode.get("answer").isNull()) {
                    return stripThink(firstNode.get("answer").asText());
                }
            }

            JsonNode root = objectMapper.readTree(body);
            if (root.has("answer") && !root.get("answer").isNull()) {
                return stripThink(root.get("answer").asText());
            }

            if (root.has("data")) {
                JsonNode data = root.get("data");
                if (data.has("outputs")) {
                    JsonNode outputs = data.get("outputs");
                    if (outputs.has("output") && !outputs.get("output").isNull()) {
                        return stripThink(outputs.get("output").asText());
                    }
                    if (outputs.has("text") && !outputs.get("text").isNull()) {
                        return stripThink(outputs.get("text").asText());
                    }
                    if (outputs.has("result") && !outputs.get("result").isNull()) {
                        return stripThink(outputs.get("result").asText());
                    }
                }
                if (data.has("text")) {
                    return stripThink(data.get("text").asText());
                }
            }
            if (root.has("answer") && !root.get("answer").isNull()) {
                return stripThink(root.get("answer").asText());
            }
            if (root.has("text")) {
                return stripThink(root.get("text").asText());
            }
            if (root.has("message")) {
                return stripThink(root.get("message").asText());
            }
            if (root.has("result")) {
                return stripThink(root.get("result").asText());
            }

            log.warn("[HealthAi] Unrecognized Dify response format, returning raw body");
            return stripThink(body);

        } catch (Exception e) {
            log.warn("[HealthAi] Failed to parse Dify response: {}, returning raw body", e.getMessage());
            return stripThink(body);
        }
    }

    public void saveInterpretRecord(String archiveId, String customerId, String customerName,
                                     String suggestionType, String interpretation,
                                     String pushContent, String pushChannel) {
        HealthAiInterpretRecord record = new HealthAiInterpretRecord();
        record.setId(IDGenerator.nextStr());
        record.setArchiveId(archiveId);
        record.setCustomerId(customerId);
        record.setCustomerName(customerName);
        record.setSuggestionType(suggestionType);
        record.setInterpretation(interpretation);
        record.setPushContent(pushContent);
        record.setPushChannel(pushChannel);
        record.setInterpretTime(System.currentTimeMillis());
        record.setCreateTime(System.currentTimeMillis());
        record.setUpdateTime(System.currentTimeMillis());
        interpretRecordMapper.insert(record);
    }

    public void deleteInterpretRecord(String id) {
        interpretRecordMapper.deleteByPrimaryKey(id);
    }

    public HealthAiInterpretRecord getLastInterpretation(String archiveId) {
        HealthAiInterpretRecord criteria = new HealthAiInterpretRecord();
        criteria.setArchiveId(archiveId);
        List<HealthAiInterpretRecord> records = interpretRecordMapper.select(criteria);
        if (records == null || records.isEmpty()) return null;
        return records.stream()
            .max(Comparator.comparingLong(r -> r.getInterpretTime() != null ? r.getInterpretTime() : 0L))
            .orElse(null);
    }

    public List<HealthAiInterpretRecord> getInterpretationHistory(int pageNum, int pageSize) {
        List<HealthAiInterpretRecord> all = interpretRecordMapper.select(new HealthAiInterpretRecord());
        if (all == null) return List.of();
        all.sort((a, b) -> Long.compare(
            b.getInterpretTime() != null ? b.getInterpretTime() : 0L,
            a.getInterpretTime() != null ? a.getInterpretTime() : 0L
        ));
        int start = (pageNum - 1) * pageSize;
        int end = Math.min(start + pageSize, all.size());
        if (start >= all.size()) return List.of();
        return all.subList(start, end);
    }

    /**
     * 批量查询档案已解读的建议类型
     * @return archiveId → Set<suggestionType>
     */
    public Map<String, Set<String>> batchGetInterpretationStatus(List<String> archiveIds) {
        Map<String, Set<String>> result = new LinkedHashMap<>();
        if (archiveIds == null || archiveIds.isEmpty()) return result;

        List<HealthAiInterpretRecord> all = interpretRecordMapper.select(new HealthAiInterpretRecord());
        if (all == null) return result;

        for (HealthAiInterpretRecord r : all) {
            if (r.getArchiveId() != null && archiveIds.contains(r.getArchiveId())) {
                String type = r.getSuggestionType();
                if (type != null && !type.contains("短信") && !type.contains("SMS")) {
                    result.computeIfAbsent(r.getArchiveId(), k -> new LinkedHashSet<>()).add(type);
                }
            }
        }
        return result;
    }

    public Map<String, String> summarizeSms(List<Map<String, Object>> records, String knowledge, String customerName, String gender) {
        if (difySmsApiKey == null || difySmsApiKey.isBlank() || difySmsApiKey.contains("placeholder")) {
            return Map.of("greeting", "", "finding", "短信服务未配置", "action", "");
        }
        StringBuilder input = new StringBuilder();
        input.append("客户：").append(customerName).append("，性别：").append(gender != null ? gender : "未知").append("\n");
        if (records != null && !records.isEmpty()) {
            input.append("AI分析记录：\n");
            for (Map<String, Object> r : records) {
                input.append("[").append(r.getOrDefault("type", "")).append("] ");
                input.append(r.getOrDefault("content", "")).append("\n");
            }
        }
        if (knowledge != null && !knowledge.isBlank()) {
            input.append("健康知识：").append(knowledge).append("\n");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("query", input.toString());
        requestBody.put("inputs", Map.of());
        requestBody.put("response_mode", "blocking");
        requestBody.put("user", "system");
        String url = difyApiUrl + "/chat-messages";

        try {
            String body = callDifyBlocking(url, requestBody, difySmsApiKey);
            String json = parseDifyResponse(body);
            if (json != null && !json.isBlank()) {
                json = json.replaceAll("```[a-zA-Z]*\\s*", "").replaceAll("```", "").replace("`", "").trim();
                @SuppressWarnings("unchecked")
                Map<String, String> result = objectMapper.readValue(json, Map.class);
                return result;
            }
        } catch (Exception e) {
            log.error("[HealthAi] summarizeSms failed: {}", e.getMessage());
        }
        return Map.of("greeting", "", "finding", "生成失败，请重试", "action", "");
    }

    public void streamSummarizeSms(List<Map<String, Object>> records, String knowledge,
                                   String customerName, String gender, SseEmitter emitter) {
        if (difySmsApiKey == null || difySmsApiKey.isBlank() || difySmsApiKey.contains("placeholder")) {
            try {
                emitter.send(SseEmitter.event().name("error").data("短信服务未配置"));
                emitter.complete();
            } catch (Exception ignored) {}
            return;
        }
        StringBuilder input = new StringBuilder();
        input.append("客户：").append(customerName).append("，性别：").append(gender != null ? gender : "未知").append("\n");
        if (records != null && !records.isEmpty()) {
            input.append("AI分析记录：\n");
            for (Map<String, Object> r : records) {
                input.append("[").append(r.getOrDefault("type", "")).append("] ");
                input.append(r.getOrDefault("content", "")).append("\n");
            }
        }
        if (knowledge != null && !knowledge.isBlank()) {
            input.append("健康知识：").append(knowledge).append("\n");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("query", input.toString());
        requestBody.put("inputs", Map.of());
        requestBody.put("response_mode", "streaming");
        requestBody.put("user", "system");
        String url = difyApiUrl + "/chat-messages";

        new Thread(() -> {
            try {
                String requestJson = objectMapper.writeValueAsString(requestBody);
                String fullText = streamDifyResponse(url, requestJson, "sms", emitter, difySmsApiKey);
                    log.info("[HealthAi] streamSummarizeSms finished, fullText length: {}",
                    fullText != null ? fullText.length() : 0);
                String resultJson;
                if (fullText != null && !fullText.isBlank()) {
                    String cleaned = stripThink(fullText);
                    cleaned = cleaned
                        .replaceAll("```[a-zA-Z]*\\s*", "")
                        .replaceAll("```", "")
                        .replace("`", "").trim();
                    int jsonStart = cleaned.lastIndexOf('{');
                    int jsonEnd = cleaned.lastIndexOf('}');
                    String json = (jsonStart >= 0 && jsonEnd > jsonStart)
                        ? cleaned.substring(jsonStart, jsonEnd + 1) : cleaned;
                    log.info("[HealthAi] summarizeSms extracted json: {}", json);
                    try {
                        @SuppressWarnings("unchecked")
                        Map<String, String> result = objectMapper.readValue(json, Map.class);
                        log.info("[HealthAi] summarizeSms result: {}", result);
                        resultJson = objectMapper.writeValueAsString(result);
                    } catch (Exception e) {
                        log.warn("[HealthAi] summarizeSms JSON parse failed: {} — text: {}", e.getMessage(), json);
                        resultJson = objectMapper.writeValueAsString(
                            Map.of("greeting", "", "finding", "解析失败，请重试", "action", ""));
                    }
                } else {
                    resultJson = objectMapper.writeValueAsString(
                        Map.of("greeting", "", "finding", "生成失败，请重试", "action", ""));
                }
                emitter.send(SseEmitter.event().name("done").data(resultJson));
                emitter.complete();
                log.info("[HealthAi] streamSummarizeSms emitter completed");
            } catch (Exception e) {
                log.error("[HealthAi] streamSummarizeSms failed: {}", e.getMessage());
                try {
                    emitter.send(SseEmitter.event().name("error").data(e.getMessage()));
                    emitter.complete();
                } catch (Exception ignored) {}
            }
        }).start();
    }

    private String callDifyBlocking(String url, Map<String, Object> requestBody, String apiKey) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
            factory.setConnectTimeout(10000);
            factory.setReadTimeout((int) timeoutMs);
            RestTemplate rt = new RestTemplate(factory);

            ResponseEntity<String> response = rt.exchange(url, HttpMethod.POST, entity, String.class);
            return response.getBody();
        } catch (Exception e) {
            log.error("[HealthAi] Dify blocking call failed: {}", e.getMessage());
            return null;
        }
    }
}
