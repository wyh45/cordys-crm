package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 健康档案
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_archive")
public class HealthArchive extends BaseModel {

    @Schema(description = "客户ID")
    private String customerId;

    @Schema(description = "客户姓名")
    private String customerName;

    @Schema(description = "性别")
    private String gender;

    @Schema(description = "年龄")
    private Integer age;

    @Schema(description = "血型")
    private String bloodType;

    @Schema(description = "身高(cm)")
    private java.math.BigDecimal height;

    @Schema(description = "体重(kg)")
    private java.math.BigDecimal weight;

    @Schema(description = "手机号")
    private String phone;

    @Schema(description = "身份证号")
    private String idcardNo;

    @Schema(description = "档案编号")
    private String archiveNo;

    @Schema(description = "血压")
    private String bloodPressure;

    @Schema(description = "心率")
    private Integer heartRate;

    @Schema(description = "健康风险评分 0-100，基于异常指标计算")
    private Integer riskScore;

    @Schema(description = "吸烟: NONE/QUIT/OCCASIONAL/REGULAR/HEAVY")
    private String smoking;

    @Schema(description = "饮酒: NONE/QUIT/OCCASIONAL/REGULAR/HEAVY")
    private String drinking;

    @Schema(description = "熬夜: NEVER/RARELY/SOMETIMES/OFTEN/ALWAYS")
    private String sleepQuality;

    @Schema(description = "运动频率: NONE/ONCE_WEEK/2_3_WEEK/DAILY")
    private String exercise;

    @Schema(description = "饮食习惯: LIGHT/BALANCED/OILY/IRREGULAR")
    private String diet;

    @Schema(description = "血糖(mmol/L)")
    private java.math.BigDecimal bloodSugar;

}
