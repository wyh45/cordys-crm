package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 病史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_medical_history")
public class HealthMedicalHistory extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "疾病名称")
    private String diseaseName;

    @Schema(description = "诊断日期")
    private Long diagnosisDate;

    @Schema(description = "治疗状态")
    private String treatmentStatus;

    @Schema(description = "家族病史")
    private String familyHistory;

    @Schema(description = "备注")
    private String remarks;
}
