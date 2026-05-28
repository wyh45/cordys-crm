package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 过敏史
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_allergy")
public class HealthAllergy extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "过敏原")
    private String allergen;

    @Schema(description = "严重程度")
    private String severity;
}
