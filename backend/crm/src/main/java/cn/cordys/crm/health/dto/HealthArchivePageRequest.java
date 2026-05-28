package cn.cordys.crm.health.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 健康档案分页请求
 */
@Data
public class HealthArchivePageRequest {

    @Schema(description = "页码", example = "1")
    private Integer page;

    @Schema(description = "每页条数", example = "20")
    private Integer pageSize;

    @Schema(description = "搜索关键字")
    private String keyword;
}
