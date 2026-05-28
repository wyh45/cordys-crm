package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 健康知识库
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_knowledge")
public class HealthKnowledge extends BaseModel {

    @Schema(description = "标题")
    private String title;

    @Schema(description = "内容")
    private String content;

    @Schema(description = "标签(逗号分隔)")
    private String tags;

    @Schema(description = "分类")
    private String category;

    @Schema(description = "内容Embedding向量(JSON数组)")
    private String embedding;
}
