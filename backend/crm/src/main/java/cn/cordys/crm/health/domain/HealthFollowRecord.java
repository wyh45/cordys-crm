package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 随访/访问记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_follow_record")
public class HealthFollowRecord extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "随访日期")
    private Long followDate;

    @Schema(description = "随访方式")
    private String followType;

    @Schema(description = "随访内容/结果")
    private String followResult;

    @Schema(description = "下次随访日期")
    private Long nextFollowDate;
}
