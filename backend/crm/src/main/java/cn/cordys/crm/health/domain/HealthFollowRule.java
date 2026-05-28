package cn.cordys.crm.health.domain;

import cn.cordys.common.domain.BaseModel;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 随访规则模板
 * 
 * 触发时机：健康档案新建/更新时，自动评估所有启用规则，符合条件则生成随访记录
 * 
 * conditionExpr 示例（JSON格式）：
 *   {"field":"bloodType","op":"eq","value":"O"}                    // 血型为O
 *   {"field":"age","op":"gte","value":50}                         // 年龄>=50
 *   {"field":"diagnosis","op":"contains","value":"高血压"}         // 诊断包含高血压
 *   {"field":"weight","op":"between","value":[80,100]}            // 体重在80-100之间
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Table(name = "health_follow_rule")
public class HealthFollowRule extends BaseModel {

    @Schema(description = "规则名称")
    private String name;

    @Schema(description = "规则描述")
    private String description;

    @Schema(description = "触发条件类型: BLOOD_TYPE|AGE_RANGE|DIAGNOSIS|ARCHIVE_FIELD|CUSTOM")
    private String conditionType;

    @Schema(description = "触发条件表达式(JSON)")
    private String conditionExpr;

    @Schema(description = "随访方式")
    private String followType;

    @Schema(description = "随访内容模板")
    private String followResultTemplate;

    @Schema(description = "随访间隔天数（自动计算下次随访日期）")
    private Integer followIntervalDays;

    @Schema(description = "是否启用")
    private Boolean enabled;

    @Schema(description = "优先级（数字越小优先级越高）")
    private Integer priority;

    @Schema(description = "关注的异常检查项，逗号分隔（如 血糖,总胆固醇）")
    private String watchExamItems;

    @Schema(description = "最小异常项数量，达到此值触发规则")
    private Integer minAbnormalCount;

    @Schema(description = "随访方式: SMS/PHONE/VISIT/WECHAT")
    private String followMethod;

    @Schema(description = "随访间隔天数")
    private Integer followInterval;
}
