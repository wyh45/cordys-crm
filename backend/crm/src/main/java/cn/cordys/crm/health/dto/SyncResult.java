package cn.cordys.crm.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class SyncResult {

    @Schema(description = "客户名称")
    private String customerName;

    @Schema(description = "档案ID")
    private String archiveId;

    @Schema(description = "创建的体检记录数")
    private int createdCount;

    @Schema(description = "是否成功")
    private boolean success;

    @Schema(description = "失败原因")
    private String reason;
}
