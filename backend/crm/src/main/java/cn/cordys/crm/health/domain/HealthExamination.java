package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 体检记录
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_examination")
public class HealthExamination extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "体检号")
    private String examNo;

    @Schema(description = "体检日期")
    private Long examDate;

    @Schema(description = "检查项目名称")
    private String examItem;

    @Schema(description = "检查结果值")
    private String resultValue;

    @Schema(description = "参考范围")
    private String referenceRange;

    @Schema(description = "是否异常: 0正常 1异常")
    private Boolean isAbnormal;

    @Schema(description = "结果标记")
    private String resultFlag;
}
