package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 疫苗接种
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_vaccination")
public class HealthVaccination extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "疫苗名称")
    private String vaccineName;

    @Schema(description = "接种日期")
    private Long vaccinateDate;

    @Schema(description = "下一针日期")
    private Long nextDoseDate;
}
