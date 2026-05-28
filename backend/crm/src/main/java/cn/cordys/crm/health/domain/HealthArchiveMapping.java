package cn.cordys.crm.health.domain;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

/**
 * 体检档案映射表（身份证 ↔ CRM客户/档案ID）
 */
@Data
@Table(name = "health_archive_mapping")
public class HealthArchiveMapping {

    @Id
    @Schema(description = "主键ID")
    private String id;

    @Schema(description = "身份证号")
    private String idcardNo;

    @Schema(description = "CRM客户ID")
    private String customerId;

    @Schema(description = "健康档案ID")
    private String archiveId;

    @Schema(description = "体检号")
    private String examNo;

    @Schema(description = "体检日期")
    private Long examDate;

    @Schema(description = "同步状态: 0=待同步, 1=已同步, 2=失败")
    private Integer syncStatus;

    @Schema(description = "错误信息")
    private String errorMsg;

    @Schema(description = "创建时间")
    private Long createTime;

    @Schema(description = "更新时间")
    private Long updateTime;
}
