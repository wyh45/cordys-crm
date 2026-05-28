package cn.cordys.crm.health.service;

import cn.cordys.crm.health.domain.HealthKnowledge;
import cn.cordys.mybatis.BaseMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;

@Service
public class HealthKnowledgeEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(HealthKnowledgeEmbeddingService.class);
    private static final String EMBEDDING_URL = "http://200.200.200.132:8080/embed";
    private static final String EMBEDDING_MODEL = "BAAI/bge-large-zh-v1.5";

    @Resource
    private BaseMapper<HealthKnowledge> knowledgeMapper;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = buildHttpClient();

    private static HttpClient buildHttpClient() {
        System.setProperty("http.nonProxyHosts", "200.200.200.132");
        return HttpClient.newBuilder().proxy(ProxySelector.of(null)).build();
    }

    /**
     * 为知识内容生成embedding向量并存储
     */
    public void computeAndStoreEmbedding(HealthKnowledge knowledge) {
        if (StringUtils.isBlank(knowledge.getContent())) return;
        try {
            String text = knowledge.getTitle() + " " + knowledge.getContent();
            if (text.length() > 512) text = text.substring(0, 512);

            double[] vector = callEmbeddingServer(text);
            if (vector == null) return;

            knowledge.setEmbedding(objectMapper.writeValueAsString(vector));
            knowledgeMapper.updateById(knowledge);
            log.info("Embedding stored for knowledge {}", knowledge.getId());
        } catch (Exception e) {
            log.error("Failed to compute embedding for knowledge {}", knowledge.getId(), e);
        }
    }

    /**
     * 语义搜索
     */
    public List<HealthKnowledge> semanticSearch(String query, int topK) {
        if (StringUtils.isBlank(query)) return List.of();

        try {
            double[] queryVector = callEmbeddingServer(query);
            if (queryVector == null) return List.of();

            List<HealthKnowledge> all = knowledgeMapper.select(new HealthKnowledge());
            if (all == null || all.isEmpty()) return List.of();

            List<Map.Entry<HealthKnowledge, Double>> scored = new ArrayList<>();
            for (HealthKnowledge k : all) {
                if (StringUtils.isBlank(k.getEmbedding())) continue;
                try {
                    double[] docVector = parseEmbedding(k.getEmbedding());
                    if (docVector == null) continue;
                    double score = cosineSimilarity(queryVector, docVector);
                    scored.add(Map.entry(k, score));
                } catch (Exception ignored) {}
            }

            scored.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
            List<HealthKnowledge> results = new ArrayList<>();
            for (int i = 0; i < Math.min(topK, scored.size()); i++) {
                results.add(scored.get(i).getKey());
            }
            return results;
        } catch (Exception e) {
            log.error("Semantic search failed", e);
            return List.of();
        }
    }

    private double[] callEmbeddingServer(String text) {
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                "inputs", text,
                "model", EMBEDDING_MODEL
            ));
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(EMBEDDING_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            log.info("Embedding response status={}", response.statusCode());
            log.info("Embedding response body={}", response.body());
            if (response.statusCode() != 200) {
                log.error("Embedding server returned {}", response.statusCode());
                return null;
            }
            JsonNode root = objectMapper.readTree(response.body());
            if (!root.isArray() || root.size() == 0) return null;
            JsonNode arr = root.get(0).isArray() && root.size() == 1 ? root.get(0) : root;
            double[] vector = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vector[i] = arr.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            log.error("Failed to call embedding server", e);
            return null;
        }
    }

    private double[] parseEmbedding(String json) {
        try {
            JsonNode arr = objectMapper.readTree(json);
            double[] vector = new double[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vector[i] = arr.get(i).asDouble();
            }
            return vector;
        } catch (Exception e) {
            return null;
        }
    }

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }
}
