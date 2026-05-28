package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 健康推送记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_push_record")
public class HealthPushRecord extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "知识ID")
    private String knowledgeId;

    @Schema(description = "推送渠道")
    private String pushChannel;

    @Schema(description = "推送状态")
    private String pushStatus;

    @Schema(description = "推送时间")
    private Long pushTime;
}
