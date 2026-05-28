package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_ai_interpret_record")
public class HealthAiInterpretRecord extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "档案ID")
    private String archiveId;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "解读内容（AI返回的完整文本）")
    private String interpretation;

    @Schema(description = "推送内容（发送给客户的摘要）")
    private String pushContent;

    @Schema(description = "推送渠道")
    private String pushChannel;

    @Schema(description = "建议类型: 检查建议/生活方式建议/饮食建议/运动建议")
    private String suggestionType;

    @Schema(description = "解释时间（时间戳）")
    private Long interpretTime;
}
