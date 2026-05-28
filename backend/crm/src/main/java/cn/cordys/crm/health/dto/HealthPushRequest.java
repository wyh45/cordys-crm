package cn.cordys.crm.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

/**
 * 健康推送请求
 */
@Data
public class HealthPushRequest {

    @Schema(description = "客户ID列表（为空则推送给所有客户）")
    private List<String> customerIds;

    @Schema(description = "知识ID")
    private String knowledgeId;

    @Schema(description = "推送渠道: SMS/EMAIL/INSITE/DINGTALK/LARK/WECOM")
    private String channel;

    @Schema(description = "推送标题")
    private String title;

    @Schema(description = "推送内容（不填则自动取知识库内容）")
    private String content;
}
